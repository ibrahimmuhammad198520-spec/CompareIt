package com.bank.edccompare.model;

import com.bank.edccompare.config.RecordTypeRule;

/** One parsed line from an EDC file: its raw content, its identified type, and its match key (if any). */
public class ParsedRecord {
    private final int lineNumber;
    private final String rawLine;
    private final RecordTypeRule typeRule;
    private final String matchKey;

    public ParsedRecord(int lineNumber, String rawLine, RecordTypeRule typeRule) {
        this.lineNumber = lineNumber;
        this.rawLine = rawLine;
        this.typeRule = typeRule;
        this.matchKey = typeRule.hasKey() ? typeRule.extractKey(rawLine) : "";
    }

    public int getLineNumber() { return lineNumber; }
    public String getRawLine() { return rawLine; }
    public RecordTypeRule getTypeRule() { return typeRule; }
    public String getMatchKey() { return matchKey; }
    public boolean hasKey() { return typeRule.hasKey(); }
}
