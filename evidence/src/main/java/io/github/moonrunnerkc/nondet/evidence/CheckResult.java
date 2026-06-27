package io.github.moonrunnerkc.nondet.evidence;

import java.util.List;

/**
 * What {@code nondet check --minimize} found for one corpus target, classified.
 *
 * <p>This is a faithful record of a real run, nothing inferred beyond the classification. The
 * {@link Verdict} comes from the run's exit code and the headers check printed; the causal reads and
 * repro command are lifted verbatim from its output when it attributed a cause. A target that could
 * not run keeps that verdict so the report can say so rather than imply a clean result.
 *
 * @param target       the target this result is for, never {@code null}
 * @param verdict      how the run came out, never {@code null}
 * @param causalReads  the source locations check named as the cause, empty unless attributed
 * @param reproCommand the command check printed to reproduce the failure, or {@code null} when none
 */
public record CheckResult(CorpusTarget target, Verdict verdict, List<String> causalReads,
    String reproCommand) {

  /** How a check run came out for a target. */
  public enum Verdict {
    /** The runs agreed on outcome: no causal nondeterminism, the no-false-positive case. */
    OUTCOME_STABLE,
    /** The outcomes differed and check named the minimal reads that cause it. */
    CAUSE_ATTRIBUTED,
    /** The outcomes differed but no catalog read controlled them: a cause outside the six sources. */
    DIVERGED_UNATTRIBUTED,
    /** The target could not be run under the agent, so nothing was measured. */
    COULD_NOT_RUN
  }

  /**
   * Copies the causal-read list.
   */
  public CheckResult {
    causalReads = List.copyOf(causalReads);
  }
}
