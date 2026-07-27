package com.bank.edccompare.compare;

public class RecordTypeSummary {
    private final String recordType;
    private final String recordTypeName;
    private int prodCount;
    private int testCount;
    private int matchedCount;
    private int missingCount;
    private int extraCount;
    private int recordsWithDifferences;

    public RecordTypeSummary(String recordType, String recordTypeName) {
        this.recordType = recordType;
        this.recordTypeName = recordTypeName;
    }

    public String getRecordType() { return recordType; }
    public String getRecordTypeName() { return recordTypeName; }
    public int getProdCount() { return prodCount; }
    public void setProdCount(int v) { prodCount = v; }
    public int getTestCount() { return testCount; }
    public void setTestCount(int v) { testCount = v; }
    public int getMatchedCount() { return matchedCount; }
    public void setMatchedCount(int v) { matchedCount = v; }
    public int getMissingCount() { return missingCount; }
    public void setMissingCount(int v) { missingCount = v; }
    public int getExtraCount() { return extraCount; }
    public void setExtraCount(int v) { extraCount = v; }
    public int getRecordsWithDifferences() { return recordsWithDifferences; }
    public void setRecordsWithDifferences(int v) { recordsWithDifferences = v; }
}
