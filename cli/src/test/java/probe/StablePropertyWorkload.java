package probe;

/**
 * A workload that reads an entropy source whose value is stable across runs, so the checker
 * records reads but finds no divergence.
 *
 * <p>It reads {@code java.version}, a system property identical in every child JVM launched
 * from the same JDK. This is the NONE case with reads: distinct from a workload that reads
 * nothing at all, the agent records a SYSPROP event each run and the runs agree on it.
 */
public final class StablePropertyWorkload {

  private StablePropertyWorkload() {
  }

  /**
   * Reads the stable property and prints it.
   *
   * @param args ignored
   */
  public static void main(String[] args) {
    System.out.println("java.version=" + System.getProperty("java.version"));
  }
}
