package probe;

/**
 * A workload that always fails, used to prove a non-zero child exit is reported with its
 * code rather than producing a silent empty diff.
 */
public final class FailingWorkload {

  private FailingWorkload() {
  }

  /**
   * Throws so the child JVM exits non-zero.
   *
   * @param args ignored
   */
  public static void main(String[] args) {
    throw new IllegalStateException("failing on purpose so the checker can report the failure");
  }
}
