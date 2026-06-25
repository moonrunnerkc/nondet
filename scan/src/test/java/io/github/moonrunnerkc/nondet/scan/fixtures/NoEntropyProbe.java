package io.github.moonrunnerkc.nondet.scan.fixtures;

/** Fixture with no entropy call sites, the negative case for the scanner tests. */
public final class NoEntropyProbe {

  private NoEntropyProbe() {
  }

  static int sample(int seed) {
    int total = seed;
    for (int i = 0; i < 4; i++) {
      total = total * 31 + i;
    }
    return total;
  }
}
