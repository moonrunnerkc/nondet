package io.github.moonrunnerkc.nondet.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FlakyRetryTest {

  @Test
  void theAttemptCapStopsTheLoopWhenTheBudgetWillNotElapse() {
    final int count = Assertions.assertTimeoutPreemptively(
        Duration.ofSeconds(5), () -> FlakyRetry.attempts(5_000_000_000L));
    assertEquals(8, count, "a budget that will not elapse leaves the attempt cap as the only stop");
  }

  @Test
  void aSmallBudgetTerminatesWithinTheCap() {
    final int count = Assertions.assertTimeoutPreemptively(
        Duration.ofSeconds(5), () -> FlakyRetry.attempts(1_000L));
    assertTrue(count >= 0 && count <= 8, "attempt count stays within the cap but was " + count);
  }
}
