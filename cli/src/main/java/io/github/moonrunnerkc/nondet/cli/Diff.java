package io.github.moonrunnerkc.nondet.cli;

import io.github.moonrunnerkc.nondet.catalog.Event;
import java.util.List;

/**
 * Finds the first entropy read where two traced runs disagree.
 *
 * <p>Contract for the finished implementation: walk both event lists together by index.
 * At the first index where the two events differ in call site id or value, return
 * {@link Divergence#mismatch}. If the lists agree on every shared index but one is
 * longer, return {@link Divergence#length} at the index of the first unmatched event,
 * carrying the longer run's event and {@code null} for the shorter run. If the lists are
 * equal in length and content, return {@link Divergence#none}. The walk stops at the
 * first divergence; later differences are not reported. Sequence numbers are not
 * compared, only call site id and value, so two runs that read the same sources in the
 * same order agree even if absolute counts differ.
 */
public final class Diff {

  private Diff() {
  }

  /**
   * Computes the first divergence between two runs.
   *
   * @param first  the events from the first run, in order, never {@code null}
   * @param second the events from the second run, in order, never {@code null}
   * @return the first divergence, or {@link Divergence#none} when the runs agree
   */
  public static Divergence first(List<Event> first, List<Event> second) {
    final int shared = Math.min(first.size(), second.size());
    for (int i = 0; i < shared; i++) {
      final Event left = first.get(i);
      final Event right = second.get(i);
      if (!sameSite(left, right) || !left.value().equals(right.value())) {
        return Divergence.mismatch(i, left, right);
      }
    }
    if (first.size() != second.size()) {
      final boolean firstRanLonger = first.size() > second.size();
      final Event extra = firstRanLonger ? first.get(shared) : second.get(shared);
      return Divergence.length(shared, firstRanLonger ? extra : null, firstRanLonger ? null : extra);
    }
    return Divergence.none();
  }

  /**
   * Reports whether two events name the same call site.
   *
   * <p>Call site ids are SHA-256 derived strings, so they compare by value. Keeping the
   * comparison here means callers do not depend on the id's concrete representation.
   *
   * @param left  an event from the first run, never {@code null}
   * @param right an event from the second run, never {@code null}
   * @return {@code true} when both events came from the same call site
   */
  private static boolean sameSite(Event left, Event right) {
    return left.callSiteId().equals(right.callSiteId());
  }
}
