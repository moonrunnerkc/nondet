package io.github.moonrunnerkc.nondet.examples;

/**
 * A retry loop whose clock samples differ from run to run.
 *
 * <p>It checks {@link System#nanoTime()} once per attempt, up to a fixed cap, and stops
 * early if a time budget elapses first. The cap keeps the number of reads small so the
 * trace stays bounded under instrumentation, but the clock values behind each attempt
 * still differ between runs. That difference is the nondeterminism the scanner flags and
 * the checker pins, at the {@code System.nanoTime} call.
 */
public final class FlakyRetry {

  private static final long BUDGET_NANOS = 1_000_000_000L;
  private static final int MAX_ATTEMPTS = 8;

  private FlakyRetry() {
  }

  /**
   * Runs the loop once and prints the attempt count.
   *
   * @param args ignored
   */
  public static void main(String[] args) {
    System.out.println(attempts(BUDGET_NANOS));
  }

  /**
   * Counts attempts until the time budget elapses or the attempt cap is reached.
   *
   * <p>The cap bounds the loop independently of the clock, so the number of
   * {@code System.nanoTime} reads is at most one plus the cap regardless of machine speed.
   *
   * @param budgetNanos the spin budget in nanoseconds, must be positive
   * @return the number of completed attempts, from zero to the attempt cap
   */
  static int attempts(long budgetNanos) {
    final long deadline = System.nanoTime() + budgetNanos;
    int count = 0;
    while (count < MAX_ATTEMPTS && System.nanoTime() < deadline) {
      count++;
    }
    return count;
  }
}
