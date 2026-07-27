package com.bank.edccompare.compare;

/**
 * A contiguous span of characters that differs between the matched production
 * and test records. Unlike a named-field difference, this doesn't require a
 * field dictionary - it's derived purely from a positional (byte-by-byte)
 * comparison of the two raw lines, skipping any declared ignore zones.
 */
public class PositionalDifference {
    private final int startPos;   // 1-based
    private final int length;
    private final String prodValue;
    private final String testValue;

    public PositionalDifference(int startPos, int length, String prodValue, String testValue) {
        this.startPos = startPos;
        this.length = length;
        this.prodValue = prodValue;
        this.testValue = testValue;
    }

    public int getStartPos() { return startPos; }
    public int getLength() { return length; }
    public String getProdValue() { return prodValue; }
    public String getTestValue() { return testValue; }
}
