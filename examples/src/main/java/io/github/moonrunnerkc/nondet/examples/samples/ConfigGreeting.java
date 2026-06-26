package io.github.moonrunnerkc.nondet.examples.samples;

import java.util.Locale;

/**
 * Picks a greeting by reading a system property, the way an app branches on configuration.
 * The branch is real nondeterminism risk: the same code prints different things on two boxes
 * with different config, even though each box is internally consistent.
 *
 * <p>The entropy is {@link System#getProperty(String, String)}, a SYSPROP source. On a single
 * machine the property does not change between runs, so {@code nondet check} usually reports
 * no divergence here. That is the honest limit of passive observation and the motivation for
 * the planned active-injection mode, which would vary the property on purpose to expose the
 * branch.
 */
public final class ConfigGreeting {

  /** The system property that selects the greeting. */
  public static final String MODE_PROPERTY = "nondet.greeting.mode";

  private ConfigGreeting() {
  }

  /**
   * Reads the greeting mode from configuration and prints the matching greeting.
   *
   * @param args ignored
   */
  public static void main(String[] args) {
    System.out.println(greetingFor(System.getProperty(MODE_PROPERTY, "default")));
  }

  /**
   * Returns the greeting for a mode.
   *
   * @param mode the configured mode, case insensitive; {@code null} is treated as the default
   * @return the greeting for {@code formal} or {@code casual}, otherwise the default greeting
   */
  static String greetingFor(String mode) {
    return switch (mode == null ? "default" : mode.toLowerCase(Locale.ROOT)) {
      case "formal" -> "Good day.";
      case "casual" -> "hey!";
      default -> "Hello.";
    };
  }
}
