package com.bank.edccompare.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule for one record type: how to identify it, how to uniquely match a
 * record of this type between production and test (its "key", e.g. the RRN),
 * and which byte ranges to ignore during comparison (e.g. a sequence number
 * that legitimately differs between runs).
 *
 * This deliberately does NOT model every named field in the record - for
 * record types with 100+ fields and many transaction sub-types, maintaining
 * a name for every byte range isn't worth it. Instead, matching is done via
 * a single key range (typically the RRN, since it's already guaranteed
 * unique per transaction), and differencing is done as a raw positional
 * diff of the two matched lines. That still tells you exactly which
 * position(s) and how many characters differ, and what the two values are -
 * without needing a field dictionary.
 */
public class RecordTypeRule {
    private final String recordTypeCode;
    private final String recordTypeName;
    private final int keyStart;   // 1-based; 0 means "no natural key - match by occurrence order"
    private final int keyLength;
    private final List<int[]> ignoreZones = new ArrayList<>(); // each = {start(1-based), length}

    public RecordTypeRule(String recordTypeCode, String recordTypeName, int keyStart, int keyLength) {
        this.recordTypeCode = recordTypeCode;
        this.recordTypeName = recordTypeName;
        this.keyStart = keyStart;
        this.keyLength = keyLength;
    }

    public void addIgnoreZone(int start, int length) {
        ignoreZones.add(new int[]{start, length});
    }

    public String getRecordTypeCode() { return recordTypeCode; }
    public String getRecordTypeName() { return recordTypeName; }
    public boolean hasKey() { return keyStart > 0 && keyLength > 0; }
    public int getKeyStart() { return keyStart; }
    public int getKeyLength() { return keyLength; }
    public List<int[]> getIgnoreZones() { return ignoreZones; }

    /** Extracts the matching key (e.g. RRN) from a raw line. Empty string if this type has no key. */
    public String extractKey(String line) {
        if (!hasKey()) return "";
        int from = keyStart - 1;
        if (from < 0 || from >= line.length()) return "";
        int to = Math.min(from + keyLength, line.length());
        return line.substring(from, to).trim();
    }

    /** True if the given 0-based character index falls inside a declared ignore zone. */
    public boolean isIgnored(int charIndex0based) {
        for (int[] zone : ignoreZones) {
            int from = zone[0] - 1;
            int to = from + zone[1];
            if (charIndex0based >= from && charIndex0based < to) return true;
        }
        return false;
    }
}
