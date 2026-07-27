package com.bank.edccompare.compare;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ComparisonReport {
    private Path prodFile;
    private Path testFile;
    private final LocalDateTime generatedAt = LocalDateTime.now();
    private int prodTotalLines;
    private int testTotalLines;

    private final Map<String, RecordTypeSummary> summaryByType = new LinkedHashMap<>();
    private final List<MissingOrExtraRecord> missingOrExtra = new ArrayList<>();
    private final List<RecordDifference> recordDifferences = new ArrayList<>();
    private final List<String> duplicateKeyWarnings = new ArrayList<>();

    public Path getProdFile() { return prodFile; }
    public void setProdFile(Path p) { prodFile = p; }
    public Path getTestFile() { return testFile; }
    public void setTestFile(Path p) { testFile = p; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public int getProdTotalLines() { return prodTotalLines; }
    public void setProdTotalLines(int v) { prodTotalLines = v; }
    public int getTestTotalLines() { return testTotalLines; }
    public void setTestTotalLines(int v) { testTotalLines = v; }

    public Map<String, RecordTypeSummary> getSummaryByType() { return summaryByType; }
    public List<MissingOrExtraRecord> getMissingOrExtra() { return missingOrExtra; }
    public List<RecordDifference> getRecordDifferences() { return recordDifferences; }
    public List<String> getDuplicateKeyWarnings() { return duplicateKeyWarnings; }

    public boolean isPass() {
        if (!missingOrExtra.isEmpty()) return false;
        for (RecordDifference rd : recordDifferences) if (rd.hasDifferences()) return false;
        return true;
    }

    public int totalFieldDifferenceCount() {
        int count = 0;
        for (RecordDifference rd : recordDifferences) count += rd.getPositionalDifferences().size();
        return count;
    }
}
