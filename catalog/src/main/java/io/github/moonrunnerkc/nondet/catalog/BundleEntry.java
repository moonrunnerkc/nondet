package io.github.moonrunnerkc.nondet.catalog;

import java.util.Objects;

/**
 * One recorded entropy value in a {@link Bundle}, the unit a replay run serves back.
 *
 * <p>An entry is keyed by its call site id and its per-site sequence: the call site says which read
 * this is, and the per-site sequence says which occurrence at that site, counting from zero. Two
 * runs that read the same sites in the same order line up entry for entry on that key, which is what
 * lets a replay return the recorded value for the n-th hit of a site. The global sequence is kept so
 * a replay can also pin cross-thread order, and the category and value carry the recorded read
 * itself.
 *
 * @param globalSeq  the global sequence number from the recorded run, defining total order
 * @param callSiteId the stable id of the call site this value came from, never {@code null}
 * @param perSiteSeq the zero-based occurrence index at that call site
 * @param category   the kind of entropy that produced the value, never {@code null}
 * @param value      the recorded value rendered as text, never {@code null}
 */
public record BundleEntry(
    long globalSeq,
    String callSiteId,
    int perSiteSeq,
    Category category,
    String value) {

  /**
   * Validates the reference fields and the per-site sequence.
   *
   * @throws NullPointerException if {@code callSiteId}, {@code category}, or {@code value} is
   *     {@code null}
   * @throws IllegalArgumentException if {@code perSiteSeq} is negative
   */
  public BundleEntry {
    Objects.requireNonNull(callSiteId, "callSiteId is required for a bundle entry");
    Objects.requireNonNull(category, "category is required for a bundle entry");
    Objects.requireNonNull(value, "value is required for a bundle entry; use an empty string for no value");
    if (perSiteSeq < 0) {
      throw new IllegalArgumentException(
          "perSiteSeq counts occurrences from zero and cannot be negative; got " + perSiteSeq);
    }
  }
}
