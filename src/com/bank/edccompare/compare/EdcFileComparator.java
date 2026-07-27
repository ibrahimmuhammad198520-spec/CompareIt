package com.bank.edccompare.compare;

import com.bank.edccompare.config.LayoutRepository;
import com.bank.edccompare.model.ParsedRecord;
import com.bank.edccompare.parser.EdcFileParser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Core comparison engine.
 *
 * Design notes:
 *  - Records are matched by a business key (e.g. the RRN for transaction
 *    records) - NEVER by their line/row position. A record that appears on a
 *    different row in the test file is matched correctly by its key and is
 *    NOT reported as a mismatch just because its position changed; only real
 *    content/length differences within the matched pair are reported.
 *  - Record types with no natural key (typically a single Batch Header /
 *    Batch Trailer / File Header / File Trailer per file) fall back to
 *    occurrence-order matching (1st BH vs 1st BH, 2nd BT vs 2nd BT, etc.)
 *    since they have nothing else to match on.
 *  - Field-level differencing does not require a field dictionary: matched
 *    records are compared character-by-character, skipping any declared
 *    ignore zones (e.g. the batch header sequence number), and contiguous
 *    differing character runs are grouped into a single reported difference
 *    (position, length, production value, test value).
 *  - Lookups use HashMaps, so runtime is O(n) for n = number of records; this
 *    comfortably handles files with 10,000+ records without quadratic blowup.
 */
public class EdcFileComparator {

    private final LayoutRepository layout;

    public EdcFileComparator(LayoutRepository layout) {
        this.layout = layout;
    }

    public ComparisonReport compare(Path prodPath, Path testPath) throws IOException {
        EdcFileParser parser = new EdcFileParser(layout);
        List<ParsedRecord> prodRecords = parser.parse(prodPath);
        List<ParsedRecord> testRecords = parser.parse(testPath);

        ComparisonReport report = new ComparisonReport();
        report.setProdFile(prodPath);
        report.setTestFile(testPath);
        report.setProdTotalLines(prodRecords.size());
        report.setTestTotalLines(testRecords.size());

        Map<String, List<ParsedRecord>> prodByType = groupByType(prodRecords);
        Map<String, List<ParsedRecord>> testByType = groupByType(testRecords);

        Set<String> allTypes = new LinkedHashSet<>();
        allTypes.addAll(prodByType.keySet());
        allTypes.addAll(testByType.keySet());

        for (String type : allTypes) {
            List<ParsedRecord> prodList = prodByType.getOrDefault(type, Collections.emptyList());
            List<ParsedRecord> testList = testByType.getOrDefault(type, Collections.emptyList());

            String typeName = !prodList.isEmpty() ? prodList.get(0).getTypeRule().getRecordTypeName()
                                                   : testList.get(0).getTypeRule().getRecordTypeName();

            RecordTypeSummary summary = new RecordTypeSummary(type, typeName);
            summary.setProdCount(prodList.size());
            summary.setTestCount(testList.size());

            boolean hasKey = !prodList.isEmpty() ? prodList.get(0).hasKey()
                              : (!testList.isEmpty() && testList.get(0).hasKey());

            if (hasKey) {
                compareByKey(type, typeName, prodList, testList, report, summary);
            } else {
                compareByPosition(type, typeName, prodList, testList, report, summary);
            }

            report.getSummaryByType().put(type, summary);
        }

        return report;
    }

    private Map<String, List<ParsedRecord>> groupByType(List<ParsedRecord> records) {
        Map<String, List<ParsedRecord>> map = new LinkedHashMap<>();
        for (ParsedRecord r : records) {
            map.computeIfAbsent(r.getTypeRule().getRecordTypeCode(), k -> new ArrayList<>()).add(r);
        }
        return map;
    }

    /**
     * Matches records of the same type by business key (e.g. RRN).
     * This is fully order-independent: a key found at line 50 in production
     * and line 3000 in test is matched and compared normally - a different
     * row number alone never produces a reported difference.
     */
    private void compareByKey(String type, String typeName,
                               List<ParsedRecord> prodList, List<ParsedRecord> testList,
                               ComparisonReport report, RecordTypeSummary summary) {

        Map<String, List<ParsedRecord>> prodByKey = indexByKey(prodList);
        Map<String, List<ParsedRecord>> testByKey = indexByKey(testList);

        reportDuplicateKeys(prodByKey, "PRODUCTION", type, report);
        reportDuplicateKeys(testByKey, "TEST", type, report);

        Set<String> allKeys = new LinkedHashSet<>();
        allKeys.addAll(prodByKey.keySet());
        allKeys.addAll(testByKey.keySet());

        int matched = 0, missing = 0, extra = 0, withDiffs = 0;

        for (String key : allKeys) {
            List<ParsedRecord> prodMatches = prodByKey.get(key);
            List<ParsedRecord> testMatches = testByKey.get(key);

            if (prodMatches == null) {
                for (ParsedRecord t : testMatches) {
                    report.getMissingOrExtra().add(new MissingOrExtraRecord(
                            type, typeName, key, t.getLineNumber(), t.getRawLine(),
                            MissingOrExtraRecord.Side.EXTRA_IN_TEST));
                }
                extra += testMatches.size();
                continue;
            }
            if (testMatches == null) {
                for (ParsedRecord p : prodMatches) {
                    report.getMissingOrExtra().add(new MissingOrExtraRecord(
                            type, typeName, key, p.getLineNumber(), p.getRawLine(),
                            MissingOrExtraRecord.Side.MISSING_IN_TEST));
                }
                missing += prodMatches.size();
                continue;
            }

            int pairCount = Math.min(prodMatches.size(), testMatches.size());
            for (int i = 0; i < pairCount; i++) {
                RecordDifference diff = compareRecords(prodMatches.get(i), testMatches.get(i));
                matched++;
                if (diff.hasDifferences()) withDiffs++;
                report.getRecordDifferences().add(diff);
            }
            for (int i = pairCount; i < prodMatches.size(); i++) {
                ParsedRecord p = prodMatches.get(i);
                report.getMissingOrExtra().add(new MissingOrExtraRecord(
                        type, typeName, key, p.getLineNumber(), p.getRawLine(),
                        MissingOrExtraRecord.Side.MISSING_IN_TEST));
                missing++;
            }
            for (int i = pairCount; i < testMatches.size(); i++) {
                ParsedRecord t = testMatches.get(i);
                report.getMissingOrExtra().add(new MissingOrExtraRecord(
                        type, typeName, key, t.getLineNumber(), t.getRawLine(),
                        MissingOrExtraRecord.Side.EXTRA_IN_TEST));
                extra++;
            }
        }

        summary.setMatchedCount(matched);
        summary.setMissingCount(missing);
        summary.setExtraCount(extra);
        summary.setRecordsWithDifferences(withDiffs);
    }

    /** For record types with no key (e.g. singleton header/trailer): match by occurrence order. */
    private void compareByPosition(String type, String typeName,
                                    List<ParsedRecord> prodList, List<ParsedRecord> testList,
                                    ComparisonReport report, RecordTypeSummary summary) {
        int pairCount = Math.min(prodList.size(), testList.size());
        int withDiffs = 0;

        for (int i = 0; i < pairCount; i++) {
            RecordDifference diff = compareRecords(prodList.get(i), testList.get(i));
            if (diff.hasDifferences()) withDiffs++;
            report.getRecordDifferences().add(diff);
        }
        for (int i = pairCount; i < prodList.size(); i++) {
            ParsedRecord p = prodList.get(i);
            report.getMissingOrExtra().add(new MissingOrExtraRecord(
                    type, typeName, "(no key - occurrence #" + (i + 1) + ")", p.getLineNumber(), p.getRawLine(),
                    MissingOrExtraRecord.Side.MISSING_IN_TEST));
        }
        for (int i = pairCount; i < testList.size(); i++) {
            ParsedRecord t = testList.get(i);
            report.getMissingOrExtra().add(new MissingOrExtraRecord(
                    type, typeName, "(no key - occurrence #" + (i + 1) + ")", t.getLineNumber(), t.getRawLine(),
                    MissingOrExtraRecord.Side.EXTRA_IN_TEST));
        }

        summary.setMatchedCount(pairCount);
        summary.setMissingCount(Math.max(0, prodList.size() - pairCount));
        summary.setExtraCount(Math.max(0, testList.size() - pairCount));
        summary.setRecordsWithDifferences(withDiffs);
    }

    private Map<String, List<ParsedRecord>> indexByKey(List<ParsedRecord> list) {
        Map<String, List<ParsedRecord>> map = new LinkedHashMap<>();
        for (ParsedRecord r : list) {
            map.computeIfAbsent(r.getMatchKey(), k -> new ArrayList<>()).add(r);
        }
        return map;
    }

    private void reportDuplicateKeys(Map<String, List<ParsedRecord>> byKey, String side, String type,
                                      ComparisonReport report) {
        for (Map.Entry<String, List<ParsedRecord>> e : byKey.entrySet()) {
            if (e.getValue().size() > 1) {
                report.getDuplicateKeyWarnings().add(String.format(
                        "[%s] record type '%s' key '%s' appears %d times (lines: %s)",
                        side, type, e.getKey(), e.getValue().size(), lineNumbers(e.getValue())));
            }
        }
    }

    private String lineNumbers(List<ParsedRecord> list) {
        StringBuilder sb = new StringBuilder();
        for (ParsedRecord r : list) sb.append(r.getLineNumber()).append(",");
        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    /**
     * Compares two matched records purely positionally: same-length lines
     * are walked character by character (skipping declared ignore zones);
     * differing runs of characters are grouped into a single reported
     * PositionalDifference rather than one difference per character.
     * An overall line-length mismatch is reported separately.
     */
    private RecordDifference compareRecords(ParsedRecord prod, ParsedRecord test) {
        RecordDifference diff = new RecordDifference(
                prod.getTypeRule().getRecordTypeCode(),
                prod.getTypeRule().getRecordTypeName(),
                prod.getMatchKey(),
                prod.getLineNumber(),
                test.getLineNumber());

        String prodLine = prod.getRawLine();
        String testLine = test.getRawLine();

        if (prodLine.length() != testLine.length()) {
            diff.setLineLengthMismatch(prodLine.length(), testLine.length());
        }

        int minLen = Math.min(prodLine.length(), testLine.length());
        int spanStart = -1;

        for (int i = 0; i <= minLen; i++) {
            boolean atEnd = (i == minLen);
            boolean ignored = !atEnd && prod.getTypeRule().isIgnored(i);
            boolean differsHere = !atEnd && !ignored && prodLine.charAt(i) != testLine.charAt(i);

            if (differsHere) {
                if (spanStart == -1) spanStart = i;
            } else if (spanStart != -1) {
                closeSpan(diff, prodLine, testLine, spanStart, i);
                spanStart = -1;
            }
        }

        return diff;
    }

    private void closeSpan(RecordDifference diff, String prodLine, String testLine, int start, int endExclusive) {
        diff.addPositionalDifference(new PositionalDifference(
                start + 1, endExclusive - start,
                prodLine.substring(start, endExclusive),
                testLine.substring(start, endExclusive)));
    }
}
