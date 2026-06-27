package io.github.moonrunnerkc.nondet.agent;

import io.github.moonrunnerkc.nondet.catalog.Bundle;
import io.github.moonrunnerkc.nondet.catalog.BundleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The recorded values a replay run serves back to its rewritten call sites.
 *
 * <p>Built from a {@link Bundle}, it holds, per call site, the values that site read in order. A
 * replay run calls {@link #next(String)} each time a rewritten site fires, and the table returns
 * the next recorded value for that site, advancing a per-site cursor. So the n-th hit of a site
 * gets the value the n-th hit recorded, which reproduces the recorded run as long as control flow
 * matches. When a site is absent or its values are exhausted, {@code next} returns {@code null} so
 * the caller can fall back to the live JDK rather than invent a value.
 *
 * <p>Pure JDK by design, like the rest of the runtime, so it resolves under any class loader. The
 * cursors are atomic so a threaded workload's reads stay individually consistent; pinning their
 * cross-thread order is a separate concern handled elsewhere.
 */
final class ReplayTable {

  private final Map<String, List<String>> valuesBySite;
  private final Map<String, AtomicInteger> cursors = new ConcurrentHashMap<>();

  private ReplayTable(Map<String, List<String>> valuesBySite) {
    this.valuesBySite = valuesBySite;
  }

  /**
   * Builds a replay table from a bundle.
   *
   * <p>Entries are grouped by call site and placed in per-site sequence order, so a gap in the
   * recorded sequence is filled with an empty slot rather than shifting later values forward.
   *
   * @param bundle the bundle to replay, never {@code null}
   * @return a table that serves the bundle's values in per-site order
   */
  static ReplayTable fromBundle(Bundle bundle) {
    final Map<String, List<String>> values = new HashMap<>();
    for (final BundleEntry entry : bundle.entries()) {
      final List<String> perSite = values.computeIfAbsent(entry.callSiteId(), key -> new ArrayList<>());
      while (perSite.size() <= entry.perSiteSeq()) {
        perSite.add(null);
      }
      perSite.set(entry.perSiteSeq(), entry.value());
    }
    return new ReplayTable(values);
  }

  /**
   * Returns the next recorded value for a call site, or {@code null} when there is none.
   *
   * @param callSiteId the call site firing now, never {@code null}
   * @return the next recorded value to serve, or {@code null} when the site is absent or exhausted
   */
  String next(String callSiteId) {
    final List<String> values = valuesBySite.get(callSiteId);
    if (values == null) {
      return null;
    }
    final int index = cursors.computeIfAbsent(callSiteId, key -> new AtomicInteger()).getAndIncrement();
    return index < values.size() ? values.get(index) : null;
  }
}
