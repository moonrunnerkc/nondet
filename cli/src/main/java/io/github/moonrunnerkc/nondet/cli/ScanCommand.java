package io.github.moonrunnerkc.nondet.cli;

import io.github.moonrunnerkc.nondet.catalog.Catalog;
import io.github.moonrunnerkc.nondet.scan.ClassEntry;
import io.github.moonrunnerkc.nondet.scan.ClassSource;
import io.github.moonrunnerkc.nondet.scan.EntropyScanner;
import io.github.moonrunnerkc.nondet.scan.Finding;
import io.github.moonrunnerkc.nondet.scan.ScanReport;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * The {@code nondet scan} subcommand: statically list entropy call sites.
 *
 * <p>Reads compiled classes from a directory, jar, or single class file, matches each
 * call against the catalog, and prints the findings grouped by category. The scan is a
 * report and always exits zero; it is not a pass or fail gate.
 */
@Command(
    name = "scan",
    description = "Statically list entropy call sites in compiled classes.")
public final class ScanCommand implements Callable<Integer> {

  @Parameters(
      index = "0",
      paramLabel = "PATH",
      description = "A classes directory, a jar, or a single .class file to scan.")
  private Path path;

  /**
   * Scans the path and prints the report.
   *
   * @return zero on success
   * @throws Exception if the path cannot be read or holds unreadable class files
   */
  @Override
  public Integer call() throws Exception {
    final List<ClassEntry> classes = ClassSource.read(path);
    final List<Finding> findings = new EntropyScanner(Catalog.ofDefault()).scan(classes);
    ScanReport.of(findings).printTo(System.out);
    return 0;
  }
}
