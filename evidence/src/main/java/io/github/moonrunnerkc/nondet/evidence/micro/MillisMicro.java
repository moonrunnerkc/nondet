package io.github.moonrunnerkc.nondet.evidence.micro;

/**
 * A micro flaky workload whose outcome is controlled by one {@code System.currentTimeMillis} read.
 *
 * <p>The millisecond clock reaches standard output, so two runs differ and the single read controls
 * the outcome. The minimizer must keep attributing it to {@code System.currentTimeMillis}.
 */
public final class MillisMicro {

  private MillisMicro() {
  }

  /**
   * Prints a line containing one millisecond clock read.
   *
   * @param args ignored
   */
  public static void main(String[] args) {
    System.out.println("millis:" + System.currentTimeMillis());
  }
}
