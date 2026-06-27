package io.github.moonrunnerkc.nondet.catalog;

/**
 * One part of the result surface that an {@link Outcome} fingerprint can cover.
 *
 * <p>An outcome is a fingerprint over what a run produced, independent of the entropy it read
 * to get there. These are the parts that fingerprint draws from. {@link #EXIT}, {@link #STDOUT},
 * and {@link #STDERR} are always present, since every process has them. {@link #DECLARED_RESULT}
 * is present only when the workload published an explicit result through the result channel.
 */
public enum OutcomeComponent {

  /** The process exit code. */
  EXIT,

  /** The bytes the process wrote to standard output. */
  STDOUT,

  /** The bytes the process wrote to standard error. */
  STDERR,

  /** An explicit result the workload published, when it used the result channel. */
  DECLARED_RESULT
}
