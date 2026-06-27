package io.github.moonrunnerkc.nondet.evidence.micro;

/**
 * A micro flaky workload whose outcome is controlled by one {@code System.nanoTime} read.
 *
 * <p>The clock value reaches standard output, so two runs differ and the single read controls the
 * outcome. It is a known-cause case the minimizer must keep attributing to {@code System.nanoTime}.
 */
public final class NanoMicro {

  private NanoMicro() {
  }

  /**
   * Prints a line containing one nanosecond clock read.
   *
   * @param args ignored
   */
  public static void main(String[] args) {
    System.out.println("nano:" + System.nanoTime());
  }
}
