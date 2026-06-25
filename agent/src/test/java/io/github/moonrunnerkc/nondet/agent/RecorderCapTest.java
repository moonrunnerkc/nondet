package io.github.moonrunnerkc.nondet.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.moonrunnerkc.nondet.catalog.Category;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The event cap keeps an unbounded run from exhausting memory: once the cap is reached the
 * recorder stops buffering and reports the run as truncated, while everything below the cap
 * is recorded as usual.
 */
class RecorderCapTest {

  @BeforeEach
  void clearRecorder() {
    Recorder.reset();
  }

  @AfterEach
  void clearCapProperty() {
    System.clearProperty(Recorder.MAX_EVENTS_PROPERTY);
    Recorder.reset();
  }

  @Test
  void recordsEveryEventWhenUnderTheCap() {
    System.setProperty(Recorder.MAX_EVENTS_PROPERTY, "100");
    Recorder.reset();
    for (int i = 0; i < 40; i++) {
      Recorder.record(Category.TIME, "id", Integer.toString(i));
    }
    assertEquals(40, Recorder.snapshot().size());
    assertFalse(Recorder.truncated(), "a run under the cap is not truncated");
  }

  @Test
  void recordsExactlyTheCapAndFlagsTruncationWhenExceeded() {
    System.setProperty(Recorder.MAX_EVENTS_PROPERTY, "5");
    Recorder.reset();
    for (int i = 0; i < 5_000; i++) {
      Recorder.record(Category.TIME, "id", Integer.toString(i));
    }
    assertEquals(5, Recorder.snapshot().size(), "recording stops at the cap, holding memory bounded");
    assertTrue(Recorder.truncated(), "exceeding the cap marks the run truncated");
  }

  @Test
  void theRecordedPrefixIsTheFirstEventsInOrder() {
    System.setProperty(Recorder.MAX_EVENTS_PROPERTY, "3");
    Recorder.reset();
    Recorder.record(Category.TIME, "id", "first");
    Recorder.record(Category.TIME, "id", "second");
    Recorder.record(Category.TIME, "id", "third");
    Recorder.record(Category.TIME, "id", "dropped");

    assertEquals("first", Recorder.snapshot().get(0).value());
    assertEquals("third", Recorder.snapshot().get(2).value());
    assertTrue(Recorder.truncated());
  }

  @Test
  void defaultsToTheMillionEventCapWhenThePropertyIsUnset() {
    System.clearProperty(Recorder.MAX_EVENTS_PROPERTY);
    Recorder.reset();
    assertEquals(1_000_000L, Recorder.DEFAULT_MAX_EVENTS);
    Recorder.record(Category.TIME, "id", "x");
    assertFalse(Recorder.truncated(), "a single read is far under the default cap");
  }
}
