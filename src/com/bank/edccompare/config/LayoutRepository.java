package com.bank.edccompare.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the record-type rules (record type code -> RecordTypeRule).
 *
 * IMPORTANT - ASSUMPTION CALLOUT:
 * The exact TSYS EDC (POS 5.3) record layout was not fully confirmed (it was
 * read from a photographed screen), so this class ships with a
 * clearly-labeled SAMPLE layout in defaultLayout() below. Update it - or
 * better, maintain it in an external CSV (see sample-data/layout-config.csv)
 * and pass its path as the 3rd command-line argument - once the real
 * structure is confirmed. The comparison engine itself never needs to change.
 *
 * Record types are identified by PREFIX MATCH against the start of each
 * line (longest registered code wins), not by a fixed-width type field -
 * this supports files that mix code widths (e.g. "BH" at 2 chars and "TXV"
 * at 3 chars in the same file, as seen in the sample).
 */
public class LayoutRepository {

    private final Map<String, RecordTypeRule> recordTypes = new LinkedHashMap<>();
    private List<String> codesByLengthDesc; // cached, longest-prefix-first order for identification

    public void register(RecordTypeRule rule) {
        recordTypes.put(rule.getRecordTypeCode(), rule);
        codesByLengthDesc = null; // invalidate cache
    }

    public RecordTypeRule get(String recordTypeCode) {
        return recordTypes.get(recordTypeCode);
    }

    public Map<String, RecordTypeRule> all() {
        return recordTypes;
    }

    /**
     * Identifies which registered record type a raw line belongs to, by
     * checking (longest code first, so "TXV" is tried before any 2-char
     * code that might also match) whether the line starts with that code.
     * Returns an "UNKNOWN" rule (no key, no ignore zones) if nothing matches,
     * so unmapped lines still surface in the report instead of disappearing.
     */
    public RecordTypeRule identifyType(String line) {
        if (codesByLengthDesc == null) {
            codesByLengthDesc = new ArrayList<>(recordTypes.keySet());
            codesByLengthDesc.sort(Comparator.comparingInt(String::length).reversed());
        }
        for (String code : codesByLengthDesc) {
            if (line.startsWith(code)) return recordTypes.get(code);
        }
        return new RecordTypeRule("UNKNOWN", "UNKNOWN / Unmapped record type", 0, 0);
    }

    /**
     * Loads rules from a CSV file with header row:
     *   recordType,recordTypeName,keyStart,keyLength,ignoreZones
     * keyStart/keyLength: 1-based position/length of the unique key (e.g. RRN).
     *                      Use 0,0 if this record type has no natural key
     *                      (it will be matched by occurrence order instead -
     *                      appropriate for singleton records like a Batch
     *                      Header or Batch Trailer).
     * ignoreZones: "start:length" pairs separated by "|", e.g. "3:4" or
     *              "3:4|50:2". Leave blank if nothing should be ignored.
     * Lines starting with '#' and blank lines are skipped.
     */
    public static LayoutRepository fromCsv(Path csvPath) throws IOException {
        LayoutRepository repo = new LayoutRepository();
        List<String> lines = Files.readAllLines(csvPath);
        boolean headerSkipped = false;
        for (String raw : lines) {
            if (raw == null || raw.isBlank() || raw.trim().startsWith("#")) continue;
            if (!headerSkipped) { headerSkipped = true; continue; }

            String[] c = raw.split(",", -1);
            if (c.length < 5) continue;

            String recordType = c[0].trim();
            String recordTypeName = c[1].trim();
            int keyStart = Integer.parseInt(c[2].trim());
            int keyLength = Integer.parseInt(c[3].trim());
            String ignoreZonesRaw = c[4].trim();

            RecordTypeRule rule = new RecordTypeRule(recordType, recordTypeName, keyStart, keyLength);
            if (!ignoreZonesRaw.isEmpty()) {
                for (String zone : ignoreZonesRaw.split("\\|")) {
                    String[] parts = zone.split(":");
                    if (parts.length == 2) {
                        rule.addIgnoreZone(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
                    }
                }
            }
            repo.register(rule);
        }
        return repo;
    }

    /**
     * Bundled SAMPLE layout used when no CSV is supplied - reflects the
     * record types visible in the sample screenshot (FH/BH/TXV/BT/FT).
     * Replace keyStart/keyLength/ignoreZones once the real positions are
     * confirmed from the actual file (not a photo).
     */
    public static LayoutRepository defaultLayout() {
        LayoutRepository repo = new LayoutRepository();

        RecordTypeRule fileHeader = new RecordTypeRule("FH", "File Header", 0, 0);
        repo.register(fileHeader);

        RecordTypeRule batchHeader = new RecordTypeRule("BH", "Batch Header", 0, 0);
        batchHeader.addIgnoreZone(3, 4); // batch sequence number - confirmed: ignore during comparison
        repo.register(batchHeader);

        RecordTypeRule transaction = new RecordTypeRule("TXV", "Transaction Detail", 122, 12); // RRN as unique key
        repo.register(transaction);

        RecordTypeRule batchTrailer = new RecordTypeRule("BT", "Batch Trailer", 0, 0);
        repo.register(batchTrailer);

        RecordTypeRule fileTrailer = new RecordTypeRule("FT", "File Trailer", 0, 0);
        repo.register(fileTrailer);

        return repo;
    }
}
