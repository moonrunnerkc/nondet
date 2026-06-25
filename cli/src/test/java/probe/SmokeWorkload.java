package probe;

/**
 * A trivial workload outside the skipped packages, used by the agent plumbing smoke test.
 *
 * <p>It touches one entropy source so a finished rewrite would have something to record.
 * Under the current pass-through rewrite it records nothing, which is fine: the smoke test
 * only checks that the agent attaches and writes parseable trace and registry files.
 */
public final class SmokeWorkload {

  private SmokeWorkload() {
  }

  /**
   * Reads the nanosecond clock once and exits.
   *
   * @param args ignored
   */
  public static void main(String[] args) {
    if (System.nanoTime() == Long.MIN_VALUE) {
      System.out.println("unreachable");
    }
  }
}
