package probe;

/**
 * A workload whose outcome is controlled by a single clock read.
 *
 * <p>It reads {@link System#nanoTime()} once and prints one of two discrete results depending on
 * the parity of that read. The value itself never reaches the output, only which branch it took, so
 * the outcome is one of exactly two strings. That makes it a clean single-cause fixture: forcing the
 * one read to an even or an odd value picks the outcome.
 */
public final class SingleCauseWorkload {

  private SingleCauseWorkload() {
  }

  /**
   * Prints one of two results based on the parity of a single clock read.
   *
   * @param args ignored
   */
  public static void main(String[] args) {
    if (System.nanoTime() % 2L == 0L) {
      System.out.println("EVEN");
    } else {
      System.out.println("ODD");
    }
  }
}
