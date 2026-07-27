package com.bank.edccompare.parser;

import com.bank.edccompare.config.LayoutRepository;
import com.bank.edccompare.config.RecordTypeRule;
import com.bank.edccompare.model.ParsedRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses an EDC file one line at a time (BufferedReader), so memory use stays
 * proportional to the number of records rather than the raw file size -
 * comfortably handles files with 10,000+ records.
 *
 * Each non-blank line is treated as one record. Unmapped record type
 * prefixes are still captured (as type "UNKNOWN") instead of being silently
 * dropped, so they surface in the comparison report.
 */
public class EdcFileParser {

    private final LayoutRepository layout;

    public EdcFileParser(LayoutRepository layout) {
        this.layout = layout;
    }

    public List<ParsedRecord> parse(Path filePath) throws IOException {
        List<ParsedRecord> records = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;

                RecordTypeRule typeRule = layout.identifyType(line);
                records.add(new ParsedRecord(lineNo, line, typeRule));
            }
        }
        return records;
    }
}
