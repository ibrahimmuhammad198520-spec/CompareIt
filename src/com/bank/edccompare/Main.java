package com.bank.edccompare;

import com.bank.edccompare.compare.ComparisonReport;
import com.bank.edccompare.compare.EdcFileComparator;
import com.bank.edccompare.config.LayoutRepository;
import com.bank.edccompare.report.ReportWriter;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Entry point.
 *
 * Usage:
 *   java -cp out com.bank.edccompare.Main <prodFilePath> <testFilePath> [layoutConfigCsv] [outputDir]
 *
 *   prodFilePath    : path to the production TSYS EDC (POS 5.3) file
 *   testFilePath    : path to the test TSYS EDC (POS 5.3) file
 *   layoutConfigCsv : optional path to a record-layout CSV (see sample-data/layout-config.csv).
 *                      If omitted, a bundled SAMPLE layout is used - see LayoutRepository.defaultLayout().
 *   outputDir       : optional directory to write reports to (default: current directory)
 *
 * Exit codes: 0 = PASS (files equivalent), 2 = FAIL (differences found), 1 = error.
 */
public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("TSYS EDC (POS 5.3) File Comparator");
            System.out.println("Usage: java -cp out com.bank.edccompare.Main <prodFilePath> <testFilePath> [layoutConfigCsv] [outputDir]");
            System.out.println();
            System.out.println("  prodFilePath      : path to production EDC file");
            System.out.println("  testFilePath      : path to test EDC file");
            System.out.println("  layoutConfigCsv   : optional path to record layout CSV (default: bundled sample layout)");
            System.out.println("  outputDir         : optional output directory for reports (default: current directory)");
            System.exit(1);
        }

        String prodPath = args[0];
        String testPath = args[1];
        String layoutPath = args.length > 2 ? args[2] : null;
        String outputDir = args.length > 3 ? args[3] : ".";

        try {
            LayoutRepository layoutRepository = (layoutPath != null)
                    ? LayoutRepository.fromCsv(Paths.get(layoutPath))
                    : LayoutRepository.defaultLayout();

            EdcFileComparator comparator = new EdcFileComparator(layoutRepository);
            ComparisonReport report = comparator.compare(Paths.get(prodPath), Paths.get(testPath));

            ReportWriter writer = new ReportWriter();
            writer.printSummaryToConsole(report);

            Path textReport = Paths.get(outputDir, "comparison_report.txt");
            Path csvReport = Paths.get(outputDir, "differences.csv");
            writer.writeTextReport(report, textReport);
            writer.writeDifferencesCsv(report, csvReport);

            System.out.println();
            System.out.println("Detailed report written to : " + textReport.toAbsolutePath());
            System.out.println("Differences CSV written to : " + csvReport.toAbsolutePath());

            System.exit(report.isPass() ? 0 : 2);
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
