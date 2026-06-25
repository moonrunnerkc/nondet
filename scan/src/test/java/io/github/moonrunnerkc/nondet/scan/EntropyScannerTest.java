package io.github.moonrunnerkc.nondet.scan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.moonrunnerkc.nondet.catalog.Catalog;
import io.github.moonrunnerkc.nondet.catalog.Category;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class EntropyScannerTest {

  private final EntropyScanner scanner = new EntropyScanner(Catalog.ofDefault());

  @Test
  void findsTheSingleNanoTimeSiteWithItsLineAndMethod() throws IOException {
    final List<Finding> findings =
        scanner.scan(ClassSource.read(ScanFixtures.classFile("NanoTimeProbe")));

    assertEquals(1, findings.size());
    final Finding finding = findings.get(0);
    assertEquals("System.nanoTime", finding.api());
    assertEquals(Category.TIME, finding.category());
    assertEquals("sample", finding.method());
    assertTrue(finding.line() > 0, "expected a real source line from the line number table");
  }

  @Test
  void findsEveryCatalogSourceInAMixedClass() throws IOException {
    final List<Finding> findings =
        scanner.scan(ClassSource.read(ScanFixtures.classFile("MixedEntropyProbe")));

    final Set<String> apis = findings.stream().map(Finding::api).collect(Collectors.toSet());
    assertEquals(Set.of("Math.random", "UUID.randomUUID", "System.getProperty", "System.getenv"), apis);
  }

  @Test
  void reportsNothingForAClassWithoutEntropy() throws IOException {
    assertEquals(List.of(), scanner.scan(ClassSource.read(ScanFixtures.classFile("NoEntropyProbe"))));
  }

  @Test
  void producesTheSameFindingsOnRepeatedScans() throws IOException {
    final List<ClassEntry> classes = ClassSource.read(ScanFixtures.directory());
    assertEquals(scanner.scan(classes), scanner.scan(classes));
  }
}
