package io.github.moonrunnerkc.nondet.evidence.micro;

/**
 * A micro flaky workload whose outcome is controlled by one {@code Math.random} draw.
 *
 * <p>The drawn value reaches standard output, so two runs differ and the single draw controls the
 * outcome. The minimizer must keep attributing it to {@code Math.random}.
 */
public final class RandomMicro {

  private RandomMicro() {
  }

  /**
   * Prints a line containing one random draw.
   *
   * @param args ignored
   */
  public static void main(String[] args) {
    System.out.println("random:" + Math.random());
  }
}
