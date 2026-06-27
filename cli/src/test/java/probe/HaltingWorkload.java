package probe;

/**
 * A workload that halts the JVM so shutdown hooks never run, leaving no trace behind.
 *
 * <p>{@link Runtime#halt(int)} skips shutdown hooks, so the agent never flushes a trace. This is
 * the harness-error path: a child that finishes without leaving a trace cannot be compared, and the
 * checker must say so with the execution-error code rather than treating the missing trace as
 * agreement.
 */
public final class HaltingWorkload {

  private HaltingWorkload() {
  }

  /**
   * Halts the JVM immediately, bypassing the agent's shutdown flush.
   *
   * @param args ignored
   */
  public static void main(String[] args) {
    Runtime.getRuntime().halt(0);
  }
}
