package io.github.moonrunnerkc.nondet.examples.samples;

/**
 * A retry loop that keeps polling until a wall-clock deadline, the way real code waits for a
 * slow resource to come up. How many attempts it makes before the budget runs out depends on
 * the clock, so the count, and the trace behind it, changes from run to run.
 *
 * <p>The entropy is {@link System#currentTimeMillis()}, a TIME source. {@code nondet check}
 * pins the divergence at that call rather than at the attempt count it feeds.
 */
public final class RetryWithTimeout {

  private static final long DEFAULT_BUDGET_MILLIS = 200L;
  private static final long BACKOFF_MILLIS = 10L;
  private static final int MAX_ATTEMPTS = 50;

  private RetryWithTimeout() {
  }

  /**
   * Runs the retry loop once and prints how many attempts the budget allowed.
   *
   * @param args optional single argument: the budget in milliseconds
   * @throws InterruptedException if a backoff sleep is interrupted
   */
  public static void main(String[] args) throws InterruptedException {
    final long budget = args.length > 0 ? Long.parseLong(args[0]) : DEFAULT_BUDGET_MILLIS;
    System.out.println("attempts=" + attempts(budget));
  }

  /**
   * Counts polling attempts until the budget elapses or the attempt cap is reached.
   *
   * <p>The cap bounds the loop independently of the clock, so the number of recorded reads
   * stays small no matter how fast the machine is.
   *
   * @param budgetMillis the polling budget in milliseconds; a non-positive budget yields zero
   *     attempts
   * @return the number of attempts made, from zero to {@link #MAX_ATTEMPTS}
   * @throws InterruptedException if a backoff sleep is interrupted
   */
  static int attempts(long budgetMillis) throws InterruptedException {
    final long deadline = System.currentTimeMillis() + budgetMillis;
    int count = 0;
    while (count < MAX_ATTEMPTS && withinDeadline(System.currentTimeMillis(), deadline)) {
      count++;
      Thread.sleep(BACKOFF_MILLIS);
    }
    return count;
  }

  /**
   * Reports whether the current time is still before the deadline.
   *
   * @param now      the current time in milliseconds
   * @param deadline the deadline in milliseconds
   * @return {@code true} when {@code now} is strictly before {@code deadline}
   */
  static boolean withinDeadline(long now, long deadline) {
    return now < deadline;
  }
}
