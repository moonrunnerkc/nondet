package io.github.moonrunnerkc.nondet.examples.samples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The deadline decision is exercised directly, without the clock, so the test is about the
 * retry policy rather than wall-clock timing.
 */
class RetryWithTimeoutTest {

  @Test
  void aTimeStrictlyBeforeTheDeadlineIsWithinIt() {
    assertTrue(RetryWithTimeout.withinDeadline(5, 10));
  }

  @Test
  void theDeadlineItselfIsNotWithinIt() {
    assertFalse(RetryWithTimeout.withinDeadline(10, 10));
    assertFalse(RetryWithTimeout.withinDeadline(11, 10));
  }

  @Test
  void anExpiredBudgetMakesNoAttempts() throws InterruptedException {
    assertEquals(0, RetryWithTimeout.attempts(-1), "a deadline already in the past stops the loop");
  }
}
