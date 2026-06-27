package probe;

/**
 * A workload where the clock reaches the output, so two runs produce different outcomes.
 *
 * <p>It reads {@link System#nanoTime()} once and branches on it, and both branches print the
 * sampled value, so the output changes whenever the clock does. Two fresh JVMs essentially never
 * read the same nanosecond, so the outcomes reliably differ and the checker reports an
 * outcome-divergent run with the clock read as the candidate cause.
 */
public final class OutcomeDivergentWorkload {

  private OutcomeDivergentWorkload() {
  }

  /**
   * Reads the clock and prints a line derived from it.
   *
   * @param args ignored
   */
  public static void main(String[] args) {
    final long sampled = System.nanoTime();
    if (sampled % 2L == 0L) {
      System.out.println("even-" + sampled);
    } else {
      System.out.println("odd-" + sampled);
    }
  }
}
