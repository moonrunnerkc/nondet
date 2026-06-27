package probe;

/**
 * A workload whose outcome is controlled jointly by two clock reads, neither alone.
 *
 * <p>It reads {@link System#nanoTime()} twice, on two separate lines, and prints the failing result
 * only when both reads are odd. Flipping either read to even changes the outcome back, so neither
 * read controls the result on its own; both together do. That is the fixture a minimal-cause search
 * must return both reads for, and must not reproduce the failure when either is freed.
 */
public final class TwoCauseWorkload {

  private TwoCauseWorkload() {
  }

  /**
   * Prints the failing result only when both clock reads are odd.
   *
   * @param args ignored
   */
  public static void main(String[] args) {
    final boolean firstOdd = System.nanoTime() % 2L != 0L;
    final boolean secondOdd = System.nanoTime() % 2L != 0L;
    if (firstOdd && secondOdd) {
      System.out.println("BOTH-ODD");
    } else {
      System.out.println("NOT-BOTH-ODD");
    }
  }
}
