package com.bank.edccompare.compare;

import java.util.ArrayList;
import java.util.List;

public class RecordDifference {
    private final String recordType;
    private final String recordTypeName;
    private final String matchKey;
    private final int prodLineNumber;
    private final int testLineNumber;
    private final List<PositionalDifference> positionalDifferences = new ArrayList<>();
    private boolean lineLengthMismatch = false;
    private int prodLineLength;
    private int testLineLength;

    public RecordDifference(String recordType, String recordTypeName, String matchKey,
                             int prodLineNumber, int testLineNumber) {
        this.recordType = recordType;
        this.recordTypeName = recordTypeName;
        this.matchKey = matchKey;
        this.prodLineNumber = prodLineNumber;
        this.testLineNumber = testLineNumber;
    }

    public void addPositionalDifference(PositionalDifference diff) { positionalDifferences.add(diff); }
    public List<PositionalDifference> getPositionalDifferences() { return positionalDifferences; }
    public boolean hasDifferences() { return !positionalDifferences.isEmpty() || lineLengthMismatch; }

    public void setLineLengthMismatch(int prodLen, int testLen) {
        this.lineLengthMismatch = true;
        this.prodLineLength = prodLen;
        this.testLineLength = testLen;
    }
    public boolean isLineLengthMismatch() { return lineLengthMismatch; }
    public int getProdLineLength() { return prodLineLength; }
    public int getTestLineLength() { return testLineLength; }

    public String getRecordType() { return recordType; }
    public String getRecordTypeName() { return recordTypeName; }
    public String getMatchKey() { return matchKey; }
    public int getProdLineNumber() { return prodLineNumber; }
    public int getTestLineNumber() { return testLineNumber; }
}
