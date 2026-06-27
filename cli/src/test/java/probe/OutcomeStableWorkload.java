package probe;

/**
 * A workload that reads the clock on every run but always produces the same output.
 *
 * <p>The nanosecond clock differs between runs, so the recorded read varies, but the value never
 * reaches the output: it is only compared against a guard that is never true. So the outcome is
 * stable and the checker must report no causal nondeterminism, even though an entropy read varied.
 */
public final class OutcomeStableWorkload {

  private OutcomeStableWorkload() {
  }

  /**
   * Reads the clock, discards it, and prints a constant line.
   *
   * @param args ignored
   */
  public static void main(String[] args) {
    final long sampled = System.nanoTime();
    if (sampled == 7L && args.length > 1000) {
      System.out.println("unreachable: the clock never controls this branch");
    }
    System.out.println("stable-result");
  }
}
