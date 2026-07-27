package com.bank.edccompare.report;

import com.bank.edccompare.compare.ComparisonReport;
import com.bank.edccompare.compare.MissingOrExtraRecord;
import com.bank.edccompare.compare.PositionalDifference;
import com.bank.edccompare.compare.RecordDifference;
import com.bank.edccompare.compare.RecordTypeSummary;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportWriter {

    private static final String LINE = "=".repeat(100);
    private static final String SUB_LINE = "-".repeat(100);

    public void printSummaryToConsole(ComparisonReport report) {
        System.out.println(LINE);
        System.out.println("TSYS EDC (POS 5.3) FILE COMPARISON - SUMMARY");
        System.out.println(LINE);
        System.out.println("Production file : " + report.getProdFile());
        System.out.println("Test file       : " + report.getTestFile());
        System.out.println("Prod line count : " + report.getProdTotalLines());
        System.out.println("Test line count : " + report.getTestTotalLines());
        System.out.println();
        printSummaryTable(report, System.out);
        System.out.println();
        System.out.println("Missing/Extra records           : " + report.getMissingOrExtra().size());
        System.out.println("Records with content differences : " +
                report.getRecordDifferences().stream().filter(RecordDifference::hasDifferences).count());
        System.out.println("Total positional differences      : " + report.totalFieldDifferenceCount());
        System.out.println("Duplicate key warnings            : " + report.getDuplicateKeyWarnings().size());
        System.out.println();
        System.out.println("OVERALL RESULT : " + (report.isPass() ? "PASS - files are equivalent" : "FAIL - differences found"));
        System.out.println(LINE);
    }

    private void printSummaryTable(ComparisonReport report, PrintStream out) {
        out.printf("%-6s %-24s %8s %8s %8s %8s %8s %10s%n",
                "Type", "Name", "ProdCnt", "TestCnt", "Matched", "Missing", "Extra", "WithDiffs");
        out.println(SUB_LINE);
        for (RecordTypeSummary s : report.getSummaryByType().values()) {
            out.printf("%-6s %-24s %8d %8d %8d %8d %8d %10d%n",
                    s.getRecordType(), truncate(s.getRecordTypeName(), 24),
                    s.getProdCount(), s.getTestCount(), s.getMatchedCount(),
                    s.getMissingCount(), s.getExtraCount(), s.getRecordsWithDifferences());
        }
    }

    private String truncate(String s, int len) {
        return s.length() <= len ? s : s.substring(0, len - 1) + "...";
    }

    public void writeTextReport(ComparisonReport report, Path outPath) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(outPath)) {
            w.write(LINE); w.newLine();
            w.write("TSYS EDC (POS 5.3) FILE COMPARISON REPORT"); w.newLine();
            w.write(LINE); w.newLine();
            w.write("Generated at    : " + report.getGeneratedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))); w.newLine();
            w.write("Production file : " + report.getProdFile()); w.newLine();
            w.write("Test file       : " + report.getTestFile()); w.newLine();
            w.write("Prod line count : " + report.getProdTotalLines()); w.newLine();
            w.write("Test line count : " + report.getTestTotalLines()); w.newLine();
            w.newLine();

            w.write("SECTION 1: SUMMARY BY RECORD TYPE"); w.newLine();
            w.write(SUB_LINE); w.newLine();
            w.write(String.format("%-6s %-24s %8s %8s %8s %8s %8s %10s",
                    "Type", "Name", "ProdCnt", "TestCnt", "Matched", "Missing", "Extra", "WithDiffs")); w.newLine();
            for (RecordTypeSummary s : report.getSummaryByType().values()) {
                w.write(String.format("%-6s %-24s %8d %8d %8d %8d %8d %10d",
                        s.getRecordType(), truncate(s.getRecordTypeName(), 24),
                        s.getProdCount(), s.getTestCount(), s.getMatchedCount(),
                        s.getMissingCount(), s.getExtraCount(), s.getRecordsWithDifferences()));
                w.newLine();
            }
            w.write("(Note: 'Matched' pairs records by key - e.g. RRN - not by row number;"); w.newLine();
            w.write(" a record found on a different row than its counterpart is NOT treated as a mismatch.)"); w.newLine();
            w.newLine();

            w.write("SECTION 2: MISSING RECORDS (present in PRODUCTION, absent in TEST)"); w.newLine();
            w.write(SUB_LINE); w.newLine();
            writeMissingExtra(w, report.getMissingOrExtra(), MissingOrExtraRecord.Side.MISSING_IN_TEST);
            w.newLine();

            w.write("SECTION 3: EXTRA RECORDS (present in TEST, absent in PRODUCTION)"); w.newLine();
            w.write(SUB_LINE); w.newLine();
            writeMissingExtra(w, report.getMissingOrExtra(), MissingOrExtraRecord.Side.EXTRA_IN_TEST);
            w.newLine();

            w.write("SECTION 4: CONTENT DIFFERENCES (matched records with position/length/content differences)"); w.newLine();
            w.write(SUB_LINE); w.newLine();
            boolean anyDiff = false;
            for (RecordDifference rd : report.getRecordDifferences()) {
                if (!rd.hasDifferences()) continue;
                anyDiff = true;
                w.write(String.format("Record Type: %s (%s) | Key: %s | Prod Line# %d <-> Test Line# %d",
                        rd.getRecordType(), rd.getRecordTypeName(), rd.getMatchKey(),
                        rd.getProdLineNumber(), rd.getTestLineNumber()));
                w.newLine();
                if (rd.isLineLengthMismatch()) {
                    w.write(String.format("    [LINE LENGTH MISMATCH] Prod length=%d, Test length=%d",
                            rd.getProdLineLength(), rd.getTestLineLength()));
                    w.newLine();
                }
                for (PositionalDifference pd : rd.getPositionalDifferences()) {
                    w.write(String.format("    [CONTENT DIFF] Position:%-5d Length:%-3d | Prod:'%s' | Test:'%s'",
                            pd.getStartPos(), pd.getLength(), pd.getProdValue(), pd.getTestValue()));
                    w.newLine();
                }
                w.newLine();
            }
            if (!anyDiff) { w.write("(none)"); w.newLine(); w.newLine(); }

            w.write("SECTION 5: DUPLICATE KEY WARNINGS"); w.newLine();
            w.write(SUB_LINE); w.newLine();
            if (report.getDuplicateKeyWarnings().isEmpty()) {
                w.write("(none)"); w.newLine();
            } else {
                for (String warn : report.getDuplicateKeyWarnings()) { w.write(warn); w.newLine(); }
            }
            w.newLine();

            w.write(LINE); w.newLine();
            w.write("OVERALL RESULT: " + (report.isPass() ? "PASS - files are equivalent" : "FAIL - differences found")); w.newLine();
            w.write("Total positional differences: " + report.totalFieldDifferenceCount()); w.newLine();
            w.write(LINE); w.newLine();
        }
    }

    private void writeMissingExtra(BufferedWriter w, List<MissingOrExtraRecord> list,
                                    MissingOrExtraRecord.Side side) throws IOException {
        boolean any = false;
        for (MissingOrExtraRecord m : list) {
            if (m.getSide() != side) continue;
            any = true;
            w.write(String.format("Type: %s (%s) | Key: %s | Line#: %d | Raw: %s",
                    m.getRecordType(), m.getRecordTypeName(), m.getMatchKey(), m.getLineNumber(), m.getRawLine()));
            w.newLine();
        }
        if (!any) { w.write("(none)"); w.newLine(); }
    }

    public void writeDifferencesCsv(ComparisonReport report, Path outPath) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(outPath)) {
            w.write("Category,RecordType,RecordTypeName,MatchKey,ProdLine,TestLine,Position,Length,ProdValue,TestValue");
            w.newLine();

            for (MissingOrExtraRecord m : report.getMissingOrExtra()) {
                String category = m.getSide() == MissingOrExtraRecord.Side.MISSING_IN_TEST ? "MISSING_IN_TEST" : "EXTRA_IN_TEST";
                w.write(csvRow(category, m.getRecordType(), m.getRecordTypeName(), m.getMatchKey(),
                        m.getSide() == MissingOrExtraRecord.Side.MISSING_IN_TEST ? String.valueOf(m.getLineNumber()) : "",
                        m.getSide() == MissingOrExtraRecord.Side.EXTRA_IN_TEST ? String.valueOf(m.getLineNumber()) : "",
                        "", "", "", ""));
                w.newLine();
            }

            for (RecordDifference rd : report.getRecordDifferences()) {
                if (rd.isLineLengthMismatch()) {
                    w.write(csvRow("LINE_LENGTH_MISMATCH", rd.getRecordType(), rd.getRecordTypeName(), rd.getMatchKey(),
                            String.valueOf(rd.getProdLineNumber()), String.valueOf(rd.getTestLineNumber()),
                            "", "",
                            String.valueOf(rd.getProdLineLength()), String.valueOf(rd.getTestLineLength())));
                    w.newLine();
                }
                for (PositionalDifference pd : rd.getPositionalDifferences()) {
                    w.write(csvRow("CONTENT_DIFFERENCE", rd.getRecordType(), rd.getRecordTypeName(), rd.getMatchKey(),
                            String.valueOf(rd.getProdLineNumber()), String.valueOf(rd.getTestLineNumber()),
                            String.valueOf(pd.getStartPos()), String.valueOf(pd.getLength()),
                            pd.getProdValue(), pd.getTestValue()));
                    w.newLine();
                }
            }
        }
    }

    private String csvRow(String... cols) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(escapeCsv(cols[i]));
        }
        return sb.toString();
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
