package probe;

/**
 * A workload class outside the tool's package so the agent does not skip it.
 *
 * <p>Used by the rewrite test to check that an instrumented {@code System.nanoTime} call
 * routes through the agent runtime.
 */
public final class NanoSite {

  private NanoSite() {
  }

  /**
   * Reads the nanosecond clock once.
   *
   * @return the value of {@link System#nanoTime()}
   */
  public static long read() {
    return System.nanoTime();
  }
}
