package com.bank.edccompare.compare;

public class MissingOrExtraRecord {
    public enum Side { MISSING_IN_TEST, EXTRA_IN_TEST }

    private final String recordType;
    private final String recordTypeName;
    private final String matchKey;
    private final int lineNumber;
    private final String rawLine;
    private final Side side;

    public MissingOrExtraRecord(String recordType, String recordTypeName, String matchKey,
                                 int lineNumber, String rawLine, Side side) {
        this.recordType = recordType;
        this.recordTypeName = recordTypeName;
        this.matchKey = matchKey;
        this.lineNumber = lineNumber;
        this.rawLine = rawLine;
        this.side = side;
    }

    public String getRecordType() { return recordType; }
    public String getRecordTypeName() { return recordTypeName; }
    public String getMatchKey() { return matchKey; }
    public int getLineNumber() { return lineNumber; }
    public String getRawLine() { return rawLine; }
    public Side getSide() { return side; }
}
