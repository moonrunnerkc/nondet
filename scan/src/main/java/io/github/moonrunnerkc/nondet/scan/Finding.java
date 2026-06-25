package io.github.moonrunnerkc.nondet.scan;

import io.github.moonrunnerkc.nondet.catalog.Category;
import java.util.Comparator;
import java.util.Objects;

/**
 * One entropy call site the scanner found in a class.
 *
 * <p>Findings carry the same coordinates the dynamic agent would record, including the
 * stable {@code callSiteId}, so a static finding and a runtime divergence can be lined
 * up by id.
 *
 * @param callSiteId     the stable id of the site, never {@code null}
 * @param declaringClass the declaring class in internal form, never {@code null}
 * @param method         the enclosing method name, never {@code null}
 * @param line           the source line of the call, or a negative value when unknown
 * @param category       the kind of entropy the called source introduces
 * @param api            the catalog api label, for example {@code System.nanoTime}, never {@code null}
 */
public record Finding(
    String callSiteId,
    String declaringClass,
    String method,
    int line,
    Category category,
    String api) implements Comparable<Finding> {

  private static final Comparator<Finding> ORDER = Comparator
      .comparing(Finding::category)
      .thenComparing(Finding::declaringClass)
      .thenComparing(Finding::method)
      .thenComparingInt(Finding::line)
      .thenComparing(Finding::api);

  /**
   * Validates that the reference fields are present.
   *
   * @throws NullPointerException if {@code callSiteId}, {@code declaringClass},
   *     {@code method}, {@code category}, or {@code api} is {@code null}
   */
  public Finding {
    Objects.requireNonNull(callSiteId, "callSiteId is required for a finding");
    Objects.requireNonNull(declaringClass, "declaringClass is required for a finding");
    Objects.requireNonNull(method, "method is required for a finding");
    Objects.requireNonNull(category, "category is required for a finding");
    Objects.requireNonNull(api, "api label is required for a finding");
  }

  /**
   * Orders findings by category, then class, method, line, and api.
   *
   * <p>The order is total and depends only on the finding's coordinates, so a sorted
   * report is byte for byte stable across runs.
   *
   * @param other the finding to compare against
   * @return a negative, zero, or positive value per {@link Comparable}
   */
  @Override
  public int compareTo(Finding other) {
    return ORDER.compare(this, other);
  }
}
