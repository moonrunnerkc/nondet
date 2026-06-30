package io.github.moonrunnerkc.nondet.cli;

import io.github.moonrunnerkc.nondet.catalog.Bundle;
import io.github.moonrunnerkc.nondet.catalog.CallSite;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Drives the causal minimizer once the runs have diverged, and prints its report.
 *
 * <p>It takes the baseline run and the first run whose outcome differs, turns each run's reads into
 * a {@link Bundle}, and hands both to {@link Bisect}, which narrows the differing reads to the
 * minimal set that controls the outcome and writes a repro bundle. The rendered report names that
 * set and the {@code nondet replay} command that reproduces the failure. When replaying the
 * recorded reads does not reproduce the observed outcome exactly, it says so, since that means part
 * of the cause is outside the recorded reads, a reflective read or thread ordering.
 */
final class CausalSearch {

  private final Path reproOut;
  private final String classpath;
  private final String mainClass;
  private final List<String> workloadArgs;
  private final boolean debug;

  /**
   * Creates a search bound to the workload coordinates the repro command needs.
   *
   * @param reproOut     where to write the repro bundle, never {@code null}
   * @param classpath    the workload class path to embed in the repro command, may be {@code null}
   * @param mainClass    the workload main class, never {@code null}
   * @param workloadArgs the workload arguments to embed in the repro command, never {@code null}
   * @param debug        whether to print the replay count to stderr
   */
  CausalSearch(Path reproOut, String classpath, String mainClass, List<String> workloadArgs,
      boolean debug) {
    this.reproOut = reproOut;
    this.classpath = classpath;
    this.mainClass = mainClass;
    this.workloadArgs = workloadArgs;
    this.debug = debug;
  }

  /**
   * Minimizes the cause of the divergence and prints the causal report.
   *
   * @param runner      the runner used to replay minimization trials, never {@code null}
   * @param workDir     the directory trial bundles are written to, never {@code null}
   * @param runOutcomes the per-run outcomes, with the baseline first, never empty
   * @param registry    the merged call-site registry for rendering coordinates, never {@code null}
   * @throws IOException if a trial bundle or the repro bundle cannot be written
   */
  void run(WorkloadRunner runner, Path workDir, List<OutcomeReport.RunOutcome> runOutcomes,
      Map<String, CallSite> registry) throws IOException {
    final OutcomeReport.RunOutcome baseline = runOutcomes.get(0);
    final OutcomeReport.RunOutcome failing = firstDifferentOutcome(runOutcomes, baseline);
    final Bundle passingBundle = Bundle.fromEvents(baseline.events());
    final Bundle failingBundle = Bundle.fromEvents(failing.events());
    final Bisect bisect = new Bisect(runner, workDir, passingBundle, failingBundle);
    final Bisect.Result result = bisect.search(reproOut.toAbsolutePath());
    System.out.print(CausalReport.render(result, registry, classpath, mainClass, workloadArgs));
    if (!result.failingFingerprint().equals(failing.outcome().fingerprint())) {
      System.out.println("\nnote: replaying the recorded reads did not reproduce the observed"
          + " outcome exactly,\nso part of the cause is outside the recorded reads, likely a"
          + " reflective read or thread ordering");
    }
    if (debug) {
      System.err.println("nondet check: causal search ran " + result.replays() + " replays");
    }
  }

  private static OutcomeReport.RunOutcome firstDifferentOutcome(
      List<OutcomeReport.RunOutcome> runOutcomes, OutcomeReport.RunOutcome baseline) {
    for (final OutcomeReport.RunOutcome candidate : runOutcomes) {
      if (!candidate.outcome().agreesWith(baseline.outcome())) {
        return candidate;
      }
    }
    throw new IllegalStateException("outcomes diverged but no run differs from the baseline; "
        + "this is a bug in the divergence check");
  }
}
