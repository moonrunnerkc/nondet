package io.github.moonrunnerkc.nondet.scan;

import io.github.moonrunnerkc.nondet.catalog.Category;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Renders findings grouped by category in a stable, reproducible layout.
 *
 * <p>Categories print in {@link Category} declaration order; within a category,
 * findings print in {@link Finding} sort order. The same findings always render to the
 * same text, so a report can be diffed or checked into a baseline.
 */
public final class ScanReport {

  private final List<Finding> findings;

  private ScanReport(List<Finding> findings) {
    final List<Finding> copy = new ArrayList<>(findings);
    copy.sort(null);
    this.findings = List.copyOf(copy);
  }

  /**
   * Builds a report over a set of findings.
   *
   * @param findings the findings to report, never {@code null}; copied and sorted defensively
   * @return a report ready to render
   */
  public static ScanReport of(List<Finding> findings) {
    return new ScanReport(findings);
  }

  /**
   * Returns the number of findings in this report.
   *
   * @return the finding count, zero or more
   */
  public int size() {
    return findings.size();
  }

  /**
   * Renders the report to a string with a trailing newline per line.
   *
   * @return the full report text, deterministic for a given set of findings
   */
  public String render() {
    final StringBuilder out = new StringBuilder();
    if (findings.isEmpty()) {
      out.append("nondet scan: no entropy call sites found\n");
      return out.toString();
    }
    out.append("nondet scan: ").append(findings.size()).append(" entropy call site");
    out.append(findings.size() == 1 ? "" : "s").append('\n');

    final Map<Category, List<Finding>> byCategory = new EnumMap<>(Category.class);
    for (final Finding finding : findings) {
      byCategory.computeIfAbsent(finding.category(), key -> new ArrayList<>()).add(finding);
    }
    for (final Category category : Category.values()) {
      final List<Finding> group = byCategory.get(category);
      if (group == null) {
        continue;
      }
      out.append('\n').append(category.name()).append(" (").append(group.size()).append(")\n");
      for (final Finding finding : group) {
        out.append("  ").append(format(finding)).append('\n');
      }
    }
    return out.toString();
  }

  /**
   * Writes the rendered report to a stream.
   *
   * @param out the stream to write to, never {@code null}
   */
  public void printTo(PrintStream out) {
    out.print(render());
  }

  private static String format(Finding finding) {
    final String dotted = finding.declaringClass().replace('/', '.');
    final String location = finding.line() >= 0 ? "line " + finding.line() : "line unknown";
    return dotted + '.' + finding.method() + "  " + location + "  " + finding.api();
  }
}
