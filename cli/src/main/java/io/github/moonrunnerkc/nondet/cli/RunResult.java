package io.github.moonrunnerkc.nondet.cli;

import java.nio.file.Path;

/**
 * The outcome of one child workload run, enough for the caller to report a clean failure or
 * proceed to the diff.
 *
 * <p>Only {@link Status#SUCCESS} guarantees a readable trace and registry. Every other
 * status carries the captured child output so the failure can be shown without a stack
 * trace, and {@link #exitCode()} is meaningful only for {@link Status#FAILED_EXIT}.
 *
 * @param status   how the run ended
 * @param index    the 1-based run number
 * @param trace    the trace file the run was asked to write
 * @param registry the registry file the run was asked to write
 * @param captured the file holding the child's merged stdout and stderr
 * @param exitCode the child exit code, valid only when {@code status} is {@link Status#FAILED_EXIT}
 */
record RunResult(Status status, int index, Path trace, Path registry, Path captured, int exitCode) {

  /** How a child run ended. */
  enum Status {
    /** The child exited zero and wrote both a trace and a registry. */
    SUCCESS,
    /** The child did not finish before the timeout and was killed. */
    TIMED_OUT,
    /** The child exited with a non-zero code. */
    FAILED_EXIT,
    /** The child exited zero but no trace or registry was found. */
    NO_TRACE
  }

  /**
   * Builds a successful result.
   *
   * @param index    the 1-based run number
   * @param trace    the written trace file
   * @param registry the written registry file
   * @param captured the captured child output
   * @return a {@link Status#SUCCESS} result
   */
  static RunResult success(int index, Path trace, Path registry, Path captured) {
    return new RunResult(Status.SUCCESS, index, trace, registry, captured, 0);
  }

  /**
   * Reports whether the run succeeded.
   *
   * @return {@code true} when this is a {@link Status#SUCCESS} result
   */
  boolean ok() {
    return status == Status.SUCCESS;
  }
}
