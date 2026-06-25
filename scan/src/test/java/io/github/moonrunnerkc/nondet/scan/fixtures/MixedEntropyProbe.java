package io.github.moonrunnerkc.nondet.scan.fixtures;

import java.util.UUID;

/** Fixture that touches four catalog sources across two categories for the scanner tests. */
public final class MixedEntropyProbe {

  private MixedEntropyProbe() {
  }

  static String sample() {
    final double roll = Math.random();
    final UUID id = UUID.randomUUID();
    final String home = System.getProperty("user.home");
    final String path = System.getenv("PATH");
    return roll + ":" + id + ":" + home + ":" + path;
  }
}
