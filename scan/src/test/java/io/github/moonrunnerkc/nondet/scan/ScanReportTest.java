package io.github.moonrunnerkc.nondet.scan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.moonrunnerkc.nondet.catalog.Category;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScanReportTest {

  private static Finding finding(String declaringClass, String method, int line, Category category, String api) {
    return new Finding(declaringClass + method + line, declaringClass, method, line, category, api);
  }

  @Test
  void rendersEmptyInputWithAClearMessage() {
    final ScanReport report = ScanReport.of(List.of());
    assertEquals("nondet scan: no entropy call sites found\n", report.render());
  }

  @Test
  void groupsByCategoryInDeclarationOrder() {
    final ScanReport report = ScanReport.of(List.of(
        finding("pkg/A", "f", 10, Category.RANDOM, "Math.random"),
        finding("pkg/A", "f", 5, Category.TIME, "System.nanoTime")));

    final String text = report.render();
    assertTrue(text.indexOf("TIME") < text.indexOf("RANDOM"),
        "TIME must group before RANDOM to match Category declaration order");
  }

  @Test
  void rendersDottedClassNamesAndLines() {
    final ScanReport report = ScanReport.of(List.of(
        finding("pkg/Demo", "run", 13, Category.TIME, "System.nanoTime")));

    final String text = report.render();
    assertTrue(text.contains("pkg.Demo.run"), "class names render dotted: " + text);
    assertTrue(text.contains("line 13"), "the source line shows: " + text);
    assertTrue(text.contains("System.nanoTime"), "the api label shows: " + text);
  }

  @Test
  void isByteForByteStableAcrossRenders() {
    final List<Finding> findings = List.of(
        finding("pkg/B", "g", 2, Category.SYSPROP, "System.getProperty"),
        finding("pkg/A", "f", 9, Category.TIME, "System.nanoTime"));
    assertEquals(ScanReport.of(findings).render(), ScanReport.of(findings).render());
  }
}
