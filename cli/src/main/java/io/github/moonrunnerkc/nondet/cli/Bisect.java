package io.github.moonrunnerkc.nondet.cli;

import io.github.moonrunnerkc.nondet.catalog.Bundle;
import io.github.moonrunnerkc.nondet.catalog.BundleEntry;
import io.github.moonrunnerkc.nondet.catalog.BundleIO;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Drives delta-debugging trials by replaying a workload with a chosen mix of recorded values.
 *
 * <p>It holds a passing context and a failing context for one workload, both recorded as bundles.
 * A trial forces a chosen subset of the reads that differ between them to their failing values,
 * frees the rest to their passing values, replays the workload against that mixed bundle, and asks
 * whether the run reproduced the failing outcome. {@link CausalMinimizer} calls that test to narrow
 * the subset to the reads that actually control the outcome.
 *
 * <p>Every replay is deterministic, so a trial's result depends only on which reads were forced.
 * Results are cached by the forced set, so the search never replays the same configuration twice,
 * and the trials run in a fixed order, so the whole search is reproducible.
 */
final class Bisect {

  /**
   * The outcome of a causal search.
   *
   * @param causalKeys         the minimal set of reads that control the failing outcome, in
   *     recorded order, empty when no recorded read among the candidates controls it
   * @param reproBundle        the bundle that reproduces the failing outcome with only the causal
   *     reads forced, or {@code null} when there were no causal reads to write one for
   * @param failingFingerprint the failing outcome the search reproduces
   * @param replays            how many child replays the search ran
   */
  record Result(List<Bundle.Key> causalKeys, Path reproBundle,
      String failingFingerprint, int replays) {
  }

  private final WorkloadRunner runner;
  private final Path workDir;
  private final Map<Bundle.Key, BundleEntry> passing;
  private final Map<Bundle.Key, BundleEntry> failing;
  private final Map<Set<Bundle.Key>, Boolean> cache = new HashMap<>();
  private String failingFingerprint;
  private int replays;

  /**
   * Creates a driver over a passing and a failing context for one workload.
   *
   * @param runner  the runner that launches replay children, never {@code null}
   * @param workDir the directory trial bundles and their runs are written to, never {@code null}
   * @param passing the bundle recorded from a passing-outcome run, never {@code null}
   * @param failing the bundle recorded from a failing-outcome run, never {@code null}
   */
  Bisect(WorkloadRunner runner, Path workDir, Bundle passing, Bundle failing) {
    this.runner = runner;
    this.workDir = workDir;
    this.passing = passing.byKey();
    this.failing = failing.byKey();
  }

  /**
   * Searches for the minimal causal reads and writes a repro bundle.
   *
   * @param reproOut where to write the bundle that reproduces the failure with only the causal
   *     reads forced, never {@code null}
   * @return the minimal causal reads, the repro bundle, the failing fingerprint, and the trial count
   * @throws IOException if the repro bundle cannot be written
   */
  Result search(Path reproOut) throws IOException {
    failingFingerprint = replayFingerprint(failingBundle());
    final List<Bundle.Key> candidates = candidates();
    if (candidates.isEmpty() || reproduces(Set.of())) {
      return new Result(List.of(), null, failingFingerprint, replays);
    }
    final List<Bundle.Key> minimal = CausalMinimizer.minimize(candidates, this::reproduces);
    BundleIO.write(reproOut, trialBundle(new LinkedHashSet<>(minimal)));
    return new Result(List.copyOf(minimal), reproOut, failingFingerprint, replays);
  }

  /**
   * Returns the reads that differ between the passing and failing contexts, in recorded order.
   *
   * @return the candidate keys sorted by their failing-run global sequence
   */
  List<Bundle.Key> candidates() {
    final List<Bundle.Key> candidates = new ArrayList<>();
    for (final Map.Entry<Bundle.Key, BundleEntry> entry : failing.entrySet()) {
      final BundleEntry passingEntry = passing.get(entry.getKey());
      if (passingEntry != null && !passingEntry.value().equals(entry.getValue().value())) {
        candidates.add(entry.getKey());
      }
    }
    candidates.sort(Comparator.comparingLong(key -> failing.get(key).globalSeq()));
    return candidates;
  }

  /**
   * Reports whether forcing the given reads to their failing values reproduces the failing outcome.
   *
   * @param forced the reads to force to their failing values; the rest use their passing values
   * @return {@code true} when the replay reproduces the failing outcome
   */
  boolean reproduces(Set<Bundle.Key> forced) {
    final Set<Bundle.Key> key = Set.copyOf(forced);
    final Boolean cached = cache.get(key);
    if (cached != null) {
      return cached;
    }
    final boolean result = failingFingerprint.equals(replayFingerprint(trialBundle(forced)));
    cache.put(key, result);
    return result;
  }

  /**
   * Builds the bundle for a trial: forced reads at their failing values, the rest at passing values.
   *
   * @param forced the reads to force to their failing values
   * @return a complete bundle over every read either context touched
   */
  Bundle trialBundle(Set<Bundle.Key> forced) {
    final Set<Bundle.Key> allKeys = new TreeSet<>(
        Comparator.comparing(Bundle.Key::callSiteId).thenComparingInt(Bundle.Key::perSiteSeq));
    allKeys.addAll(passing.keySet());
    allKeys.addAll(failing.keySet());
    final List<BundleEntry> entries = new ArrayList<>(allKeys.size());
    for (final Bundle.Key key : allKeys) {
      final BundleEntry source = failing.containsKey(key) ? failing.get(key) : passing.get(key);
      final String value = forced.contains(key)
          ? failing.get(key).value()
          : (passing.containsKey(key) ? passing.get(key).value() : failing.get(key).value());
      entries.add(new BundleEntry(source.globalSeq(), key.callSiteId(), key.perSiteSeq(),
          source.category(), value));
    }
    entries.sort(Comparator.comparingLong(BundleEntry::globalSeq));
    return new Bundle(entries);
  }

  private Bundle failingBundle() {
    return trialBundle(failing.keySet());
  }

  private String replayFingerprint(Bundle bundle) {
    final int index = ++replays;
    final Path bundleFile = workDir.resolve("trial" + index + ".bundle");
    try {
      BundleIO.write(bundleFile, bundle);
      final RunResult run = runner.run(index, bundleFile);
      if (!run.ok()) {
        throw new IllegalStateException("a replay trial did not complete cleanly (" + run.status()
            + "); the workload must run under the agent to be minimized");
      }
      return OutcomeCapture.capture(run).fingerprint();
    } catch (final IOException cause) {
      throw new UncheckedIOException("a replay trial could not be read or written", cause);
    } catch (final InterruptedException cause) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("interrupted while replaying a trial", cause);
    }
  }
}
