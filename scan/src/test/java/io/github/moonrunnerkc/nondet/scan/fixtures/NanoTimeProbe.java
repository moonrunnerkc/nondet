package io.github.moonrunnerkc.nondet.scan.fixtures;

/** Fixture with a single {@code System.nanoTime} call site for the scanner tests. */
public final class NanoTimeProbe {

  private NanoTimeProbe() {
  }

  static long sample() {
    return System.nanoTime();
  }
}
