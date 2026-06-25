package io.github.moonrunnerkc.nondet.catalog;

/**
 * The kind of entropy a source introduces.
 *
 * <p>Every {@link EntropySource} belongs to exactly one category. Categories drive
 * the grouping in scan reports and let the dynamic checker label a divergence by
 * the kind of nondeterminism behind it.
 *
 * <p>{@link #HASH_ORDER} and {@link #IDENTITY_HASH} are declared for completeness but
 * have no entry in {@link Catalog#DEFAULT}. The v0.1.0 catalog is frozen at six call
 * based sources; intrinsic ordering and identity hashing are deferred to a later phase.
 */
public enum Category {

  /** Clocks and timers, such as {@code System.nanoTime} and {@code System.currentTimeMillis}. */
  TIME,

  /** Pseudo random values, such as {@code Math.random} and {@code UUID.randomUUID}. */
  RANDOM,

  /** Process environment reads, such as {@code System.getenv}. */
  ENV,

  /** System property reads, such as {@code System.getProperty}. */
  SYSPROP,

  /** Iteration order of unordered collections. Reserved; no v0.1.0 source. */
  HASH_ORDER,

  /** Identity hash codes, such as {@code System.identityHashCode}. Reserved; no v0.1.0 source. */
  IDENTITY_HASH
}
