package io.github.moonrunnerkc.nondet.catalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The ordered set of entropy values a run read, the recipe a replay reproduces it from.
 *
 * <p>A bundle is a trace turned into a lookup table. Where a trace is a log of reads, a bundle is
 * keyed so a replay can answer "what did the n-th read at this call site return?" and serve that
 * value instead of calling the live JDK. Building one from a recorded run assigns each read a
 * per-site sequence, the count of earlier reads at the same call site, so the key is stable across
 * runs that read the same sites in the same order.
 *
 * @param entries the bundle entries in recorded order, never {@code null}
 */
public record Bundle(List<BundleEntry> entries) {

  /**
   * The identity of one read within a bundle: its call site and which occurrence at that site.
   *
   * @param callSiteId the stable call site id, never {@code null}
   * @param perSiteSeq the zero-based occurrence index at that call site
   */
  public record Key(String callSiteId, int perSiteSeq) {
  }

  /**
   * Copies the entries into an immutable list.
   */
  public Bundle {
    entries = List.copyOf(entries);
  }

  /**
   * Derives a bundle from a recorded run's events.
   *
   * <p>The events must be in their recorded order, which is the global sequence order a trace is
   * written in. Each event is assigned the per-site sequence equal to the number of earlier events
   * at its call site, so the n-th read of a site keys to per-site sequence n.
   *
   * @param events the recorded events in order, never {@code null}
   * @return a bundle that replays those reads
   */
  public static Bundle fromEvents(List<Event> events) {
    final Map<String, Integer> seen = new HashMap<>();
    final List<BundleEntry> entries = new ArrayList<>(events.size());
    for (final Event event : events) {
      final int perSiteSeq = seen.merge(event.callSiteId(), 1, Integer::sum) - 1;
      entries.add(new BundleEntry(
          event.seq(), event.callSiteId(), perSiteSeq, event.category(), event.value()));
    }
    return new Bundle(entries);
  }

  /**
   * Indexes the entries by their call site and per-site sequence key.
   *
   * @return a map from key to entry, in recorded order, so iteration stays stable
   */
  public Map<Key, BundleEntry> byKey() {
    final Map<Key, BundleEntry> index = new LinkedHashMap<>();
    for (final BundleEntry entry : entries) {
      index.put(new Key(entry.callSiteId(), entry.perSiteSeq()), entry);
    }
    return index;
  }
}
