package io.github.moonrunnerkc.nondet.evidence.micro;

import java.util.UUID;

/**
 * A micro flaky workload whose outcome is controlled by one {@code UUID.randomUUID} call.
 *
 * <p>The generated id reaches standard output, so two runs differ and the single call controls the
 * outcome. The minimizer must keep attributing it to {@code UUID.randomUUID}.
 */
public final class UuidMicro {

  private UuidMicro() {
  }

  /**
   * Prints a line containing one random UUID.
   *
   * @param args ignored
   */
  public static void main(String[] args) {
    System.out.println("uuid:" + UUID.randomUUID());
  }
}
