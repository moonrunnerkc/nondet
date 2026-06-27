package io.github.moonrunnerkc.nondet.agent;

import io.github.moonrunnerkc.nondet.catalog.Bundle;
import io.github.moonrunnerkc.nondet.catalog.BundleEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Serves recorded reads in their recorded global order, pinning a threaded run's interleaving.
 *
 * <p>Without pinning, a threaded replay serves each site's recorded values in order but lets the
 * threads reach those reads in whatever order the scheduler picks, so the cross-thread interleaving
 * is not reproduced. This serves from a single list of entries in recorded global order: a thread
 * about to read a site waits until that site is the next recorded read, takes its value, advances
 * the cursor, and wakes the others. So the reads happen in the recorded order across threads, which
 * is the order the global sequence captured.
 *
 * <p>It pins ordering at entropy-read points only; it does not pin what threads do between reads, so
 * a workload whose outcome depends on non-read interleaving stays approximate. A bundle that does
 * not match the live reads could leave a thread waiting; the per-run timeout in the launcher kills
 * such a run rather than letting it hang.
 */
final class ThreadPin {

  private final List<BundleEntry> order;
  private int cursor;

  private ThreadPin(List<BundleEntry> order) {
    this.order = order;
  }

  /**
   * Builds a turnstile from a bundle, ordered by global sequence.
   *
   * @param bundle the bundle to replay, never {@code null}
   * @return a turnstile that serves the bundle's reads in recorded order
   */
  static ThreadPin fromBundle(Bundle bundle) {
    final List<BundleEntry> order = new ArrayList<>(bundle.entries());
    order.sort(Comparator.comparingLong(BundleEntry::globalSeq));
    return new ThreadPin(order);
  }

  /**
   * Waits until the given site is the next recorded read, then serves its value.
   *
   * @param callSiteId the site the calling thread is about to read, never {@code null}
   * @return the next recorded value for that site in global order, or {@code null} once the recorded
   *     reads are exhausted, so the caller falls through to the live api
   */
  synchronized String serve(String callSiteId) {
    while (cursor < order.size() && !order.get(cursor).callSiteId().equals(callSiteId)) {
      try {
        wait();
      } catch (final InterruptedException interrupted) {
        Thread.currentThread().interrupt();
        return null;
      }
    }
    if (cursor >= order.size()) {
      return null;
    }
    final String value = order.get(cursor).value();
    cursor++;
    notifyAll();
    return value;
  }
}
