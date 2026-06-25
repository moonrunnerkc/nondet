package io.github.moonrunnerkc.nondet.cli;

import io.github.moonrunnerkc.nondet.catalog.Event;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Aggregates first-divergence across more than two runs of the same workload.
 *
 * <p>Two runs can agree by chance on a value that only sometimes changes, so the checker
 * supports N runs to widen recall. Run zero is the baseline; every later run is diffed
 * against it with {@link Diff#first}. The primary result is the earliest first-divergence
 * across those pairings, the one with the smallest read index, so the report still points
 * at a single first point of departure. Every distinct call site that diverges in any
 * pairing is collected too, so a site that only the third run exposes is not lost behind
 * the primary one.
 *
 * <p>The aggregation is deterministic: pairings are walked in run order, ties on index keep
 * the earlier run, and the extra sites come back sorted by id. It controls no entropy;
 * separate JVMs already vary the clock, UUIDs, and process salts on their own.
 */
final class MultiRunDiff {

  private MultiRunDiff() {
  }

  /**
   * The outcome of diffing several runs against the baseline.
   *
   * @param primary           the earliest divergence across all pairings, or
   *     {@link Divergence#none()} when every run agrees with the baseline
   * @param additionalSiteIds ids of call sites that diverge in some pairing but are not the
   *     primary site, sorted by id, so the report can list them as a secondary result
   */
  record Result(Divergence primary, List<String> additionalSiteIds) {
  }

  /**
   * Diffs each run after the first against the first and aggregates the divergences.
   *
   * @param runs the per run event lists, baseline first, at least two entries, none {@code null}
   * @return the primary divergence and the other diverging sites
   * @throws IllegalArgumentException if fewer than two runs are given
   */
  static Result analyze(List<List<Event>> runs) {
    if (runs.size() < 2) {
      throw new IllegalArgumentException(
          "multi-run diff needs at least two runs but got " + runs.size()
              + "; launch the workload at least twice before diffing");
    }
    final List<Event> baseline = runs.get(0);
    final Set<String> diverging = new TreeSet<>();
    Divergence primary = Divergence.none();
    for (int i = 1; i < runs.size(); i++) {
      final Divergence pairing = Diff.first(baseline, runs.get(i));
      collectInto(diverging, pairing);
      primary = earlier(primary, pairing);
    }
    final Set<String> primarySites = new TreeSet<>();
    collectInto(primarySites, primary);
    final List<String> additional = diverging.stream()
        .filter(id -> !primarySites.contains(id))
        .toList();
    return new Result(primary, additional);
  }

  private static Divergence earlier(Divergence current, Divergence candidate) {
    if (current.isNone()) {
      return candidate;
    }
    if (candidate.isNone()) {
      return current;
    }
    return candidate.index() < current.index() ? candidate : current;
  }

  private static void collectInto(Set<String> ids, Divergence divergence) {
    switch (divergence.kind()) {
      case MISMATCH -> {
        ids.add(divergence.left().callSiteId());
        ids.add(divergence.right().callSiteId());
      }
      case LENGTH -> {
        final Event extra = divergence.left() != null ? divergence.left() : divergence.right();
        ids.add(extra.callSiteId());
      }
      case NONE -> {
        // an agreeing pairing contributes no diverging site
      }
    }
  }
}
