package io.github.moonrunnerkc.nondet.examples.samples;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The greeting branch is deterministic given a mode and is tested directly; the configuration
 * read that selects the mode is the entropy the checker observes.
 */
class ConfigGreetingTest {

  @Test
  void knownModesPickTheirGreeting() {
    assertEquals("Good day.", ConfigGreeting.greetingFor("formal"));
    assertEquals("hey!", ConfigGreeting.greetingFor("casual"));
  }

  @Test
  void modeMatchingIsCaseInsensitive() {
    assertEquals("Good day.", ConfigGreeting.greetingFor("FORMAL"));
  }

  @Test
  void anUnknownOrNullModeFallsBackToTheDefaultGreeting() {
    assertEquals("Hello.", ConfigGreeting.greetingFor("anything-else"));
    assertEquals("Hello.", ConfigGreeting.greetingFor(null));
  }
}
