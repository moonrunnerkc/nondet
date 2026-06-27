package io.github.moonrunnerkc.nondet.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class OutcomeTest {

  private static byte[] utf8(String text) {
    return text.getBytes(StandardCharsets.UTF_8);
  }

  @Test
  void identicalSurfacesFingerprintEqual() {
    final Outcome first = Outcome.of(0, utf8("done\n"), utf8(""), null);
    final Outcome second = Outcome.of(0, utf8("done\n"), utf8(""), null);
    assertEquals(first.fingerprint(), second.fingerprint());
    assertTrue(first.agreesWith(second));
  }

  @Test
  void differentStdoutFingerprintsDiffer() {
    final Outcome even = Outcome.of(0, utf8("even"), utf8(""), null);
    final Outcome odd = Outcome.of(0, utf8("odd"), utf8(""), null);
    assertNotEquals(even.fingerprint(), odd.fingerprint());
    assertFalse(even.agreesWith(odd));
  }

  @Test
  void exitCodeIsPartOfTheFingerprint() {
    final Outcome clean = Outcome.of(0, utf8("x"), utf8(""), null);
    final Outcome failed = Outcome.of(1, utf8("x"), utf8(""), null);
    assertNotEquals(clean.fingerprint(), failed.fingerprint());
  }

  @Test
  void streamFramingPreventsBoundaryCollisions() {
    final Outcome split = Outcome.of(0, utf8("a"), utf8("b"), null);
    final Outcome joined = Outcome.of(0, utf8("ab"), utf8(""), null);
    assertNotEquals(split.fingerprint(), joined.fingerprint(),
        "stdout and stderr must not run together across their boundary");
  }

  @Test
  void aDeclaredResultIsRecordedAndChangesTheFingerprint() {
    final Outcome withoutResult = Outcome.of(0, utf8("same"), utf8(""), null);
    final Outcome withResult = Outcome.of(0, utf8("same"), utf8(""), utf8("published"));
    assertFalse(withoutResult.components().contains(OutcomeComponent.DECLARED_RESULT));
    assertTrue(withResult.components().contains(OutcomeComponent.DECLARED_RESULT));
    assertNotEquals(withoutResult.fingerprint(), withResult.fingerprint());
  }

  @Test
  void publishingNothingDiffersFromPublishingAnEmptyResult() {
    final Outcome none = Outcome.of(0, utf8("x"), utf8(""), null);
    final Outcome empty = Outcome.of(0, utf8("x"), utf8(""), utf8(""));
    assertNotEquals(none.fingerprint(), empty.fingerprint());
  }

  @Test
  void exitStdoutAndStderrAreAlwaysPresent() {
    final Outcome outcome = Outcome.of(0, utf8(""), utf8(""), null);
    assertTrue(outcome.components().contains(OutcomeComponent.EXIT));
    assertTrue(outcome.components().contains(OutcomeComponent.STDOUT));
    assertTrue(outcome.components().contains(OutcomeComponent.STDERR));
  }

  @Test
  void componentSetIsImmutable() {
    final Outcome outcome = Outcome.of(0, utf8(""), utf8(""), null);
    assertThrows(UnsupportedOperationException.class,
        () -> outcome.components().add(OutcomeComponent.DECLARED_RESULT));
  }
}
