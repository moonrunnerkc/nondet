package io.github.moonrunnerkc.nondet.cli;

import java.nio.file.Path;

/**
 * The outcome of one child workload run, enough for the caller to report a harness failure or to
 * fingerprint what the run produced and proceed to compare it.
 *
 * <p>A run's exit code is part of its outcome, not a verdict on whether it may be compared. A child
 * that completes and writes its trace is a {@link Status#SUCCESS}, whether it exited zero or threw,
 * because a flaky test that fails by throwing is exactly the kind of run this tool exists to
 * analyse. Only a run that never finished ({@link Status#TIMED_OUT}) or finished without leaving a
 * trace ({@link Status#NO_TRACE}) is a harness failure the caller must surface instead of compare.
 *
 * @param status   how the run ended
 * @param index    the 1-based run number
 * @param trace    the trace file the run was asked to write
 * @param registry the registry file the run was asked to write
 * @param stdout   the file holding the child's standard output
 * @param stderr   the file holding the child's standard error
 * @param result   the file a workload may publish a declared result to; may not exist
 * @param outcome  the file the checker writes this run's outcome fingerprint to
 * @param exitCode the child exit code, valid for {@link Status#SUCCESS} and {@link Status#NO_TRACE}
 */
record RunResult(Status status, int index, Path trace, Path registry, Path stdout, Path stderr,
    Path result, Path outcome, int exitCode) {

  /** How a child run ended. */
  enum Status {
    /** The child finished and wrote both a trace and a registry; comparable whatever its exit. */
    SUCCESS,
    /** The child did not finish before the timeout and was killed. */
    TIMED_OUT,
    /** The child finished but left no trace or registry, so there is nothing to compare. */
    NO_TRACE
  }

  /**
   * Builds a comparable result for a child that finished and wrote its trace.
   *
   * @param index    the 1-based run number
   * @param trace    the written trace file
   * @param registry the written registry file
   * @param stdout   the captured standard output
   * @param stderr   the captured standard error
   * @param result   the declared-result file, which may or may not exist
   * @param outcome  the file the run's outcome fingerprint is written to
   * @param exitCode the child exit code, kept as part of the outcome
   * @return a {@link Status#SUCCESS} result
   */
  static RunResult success(int index, Path trace, Path registry, Path stdout, Path stderr,
      Path result, Path outcome, int exitCode) {
    return new RunResult(Status.SUCCESS, index, trace, registry, stdout, stderr, result, outcome,
        exitCode);
  }

  /**
   * Reports whether the run can be compared.
   *
   * @return {@code true} when this is a {@link Status#SUCCESS} result
   */
  boolean ok() {
    return status == Status.SUCCESS;
  }
}
