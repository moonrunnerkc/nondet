package probe;

/**
 * A workload that reads no entropy, so every run records an empty trace and the checker
 * reports no divergence no matter how many times it runs.
 *
 * <p>It lives outside the skipped packages, so the agent instruments it; the point is that
 * instrumenting a program with no catalog call site still yields agreement.
 */
public final class DeterministicWorkload {

  private DeterministicWorkload() {
  }

  /**
   * Computes a fixed sum and prints it, touching no entropy source.
   *
   * @param args ignored
   */
  public static void main(String[] args) {
    long total = 0;
    for (int i = 0; i < 100; i++) {
      total += i;
    }
    System.out.println("deterministic-total:" + total);
  }
}
