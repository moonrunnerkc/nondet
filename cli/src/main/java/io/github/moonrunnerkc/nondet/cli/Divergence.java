package io.github.moonrunnerkc.nondet.cli;

import io.github.moonrunnerkc.nondet.catalog.Event;
import java.util.Objects;

/**
 * The first point where two traced runs disagree, or the absence of one.
 *
 * <p>A divergence is one of three shapes. A {@link Kind#MISMATCH} means both runs had an
 * event at the same index but the call site or value differed. A {@link Kind#LENGTH}
 * means one run had a further event where the other had already ended; the side that
 * ended is {@code null}. A {@link Kind#NONE} means the runs read the same entropy in the
 * same order, with {@code index} of {@code -1}.
 *
 * @param kind  which of the three shapes this is, never {@code null}
 * @param index the event index of the divergence, or {@code -1} for {@link Kind#NONE}
 * @param left  the event from the first run at {@code index}, or {@code null}
 * @param right the event from the second run at {@code index}, or {@code null}
 */
public record Divergence(Kind kind, int index, Event left, Event right) {

  /** The three shapes a divergence can take. */
  public enum Kind {
    /** Both runs had an event at the index but they differ. */
    MISMATCH,
    /** One run had a further event where the other had ended. */
    LENGTH,
    /** The runs agree everywhere. */
    NONE
  }

  /**
   * Validates that the kind is present.
   *
   * @throws NullPointerException if {@code kind} is {@code null}
   */
  public Divergence {
    Objects.requireNonNull(kind, "kind is required for a divergence");
  }

  /**
   * Builds a value or call site mismatch at an index.
   *
   * @param index the event index where the runs differ, zero or greater
   * @param left  the first run's event at that index, never {@code null}
   * @param right the second run's event at that index, never {@code null}
   * @return a {@link Kind#MISMATCH} divergence
   * @throws NullPointerException if {@code left} or {@code right} is {@code null}
   */
  public static Divergence mismatch(int index, Event left, Event right) {
    Objects.requireNonNull(left, "left event is required for a mismatch");
    Objects.requireNonNull(right, "right event is required for a mismatch");
    return new Divergence(Kind.MISMATCH, index, left, right);
  }

  /**
   * Builds a length divergence where one run ran longer than the other.
   *
   * <p>Exactly one of {@code left} and {@code right} is the extra event; the other is
   * {@code null} for the run that ended.
   *
   * @param index the index of the extra event, zero or greater
   * @param left  the first run's extra event, or {@code null} if the first run ended
   * @param right the second run's extra event, or {@code null} if the second run ended
   * @return a {@link Kind#LENGTH} divergence
   * @throws IllegalArgumentException if both or neither side is {@code null}
   */
  public static Divergence length(int index, Event left, Event right) {
    if ((left == null) == (right == null)) {
      throw new IllegalArgumentException(
          "a length divergence needs exactly one extra event; pass the longer run's event and null for the shorter");
    }
    return new Divergence(Kind.LENGTH, index, left, right);
  }

  /**
   * The divergence value meaning the runs agree everywhere.
   *
   * @return a {@link Kind#NONE} divergence
   */
  public static Divergence none() {
    return new Divergence(Kind.NONE, -1, null, null);
  }

  /**
   * Reports whether the two runs agreed.
   *
   * @return {@code true} when this is {@link Kind#NONE}
   */
  public boolean isNone() {
    return kind == Kind.NONE;
  }
}
