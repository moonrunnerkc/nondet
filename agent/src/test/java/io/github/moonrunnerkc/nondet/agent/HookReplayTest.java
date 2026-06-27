package io.github.moonrunnerkc.nondet.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.moonrunnerkc.nondet.catalog.Bundle;
import io.github.moonrunnerkc.nondet.catalog.Category;
import io.github.moonrunnerkc.nondet.catalog.Event;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HookReplayTest {

  @BeforeEach
  void clearState() {
    Recorder.reset();
    Replay.reset();
  }

  @AfterEach
  void clearReplay() {
    Replay.reset();
  }

  private static void replay(Event... events) {
    Replay.install(ReplayTable.fromBundle(Bundle.fromEvents(List.of(events))));
  }

  @Test
  void servesTheRecordedClockInsteadOfTheLiveOne() {
    replay(new Event(0, Category.TIME, "site", "424242"));

    assertEquals(424242L, Hook.nanoTime("site"),
        "replay returns the recorded value, which the live clock would never match");
    assertEquals("424242", Recorder.snapshot().get(0).value(),
        "the served value is recorded so the replay run produces a faithful trace");
  }

  @Test
  void servesPerSiteSequenceInOrder() {
    replay(new Event(0, Category.TIME, "site", "1"), new Event(1, Category.TIME, "site", "2"));

    assertEquals(1L, Hook.nanoTime("site"));
    assertEquals(2L, Hook.nanoTime("site"));
  }

  @Test
  void fallsThroughToTheLiveApiForAnUnbundledSite() {
    replay(new Event(0, Category.TIME, "known", "5"));

    Hook.nanoTime("unbundled");
    assertFalse(Recorder.snapshot().isEmpty(),
        "an unbundled read still calls the live api and records its value");
  }

  @Test
  void replaysARecordedUuid() {
    final String recorded = "123e4567-e89b-12d3-a456-426614174000";
    replay(new Event(0, Category.RANDOM, "u", recorded));

    assertEquals(UUID.fromString(recorded), Hook.randomUUID("u"));
  }

  @Test
  void replaysAnAbsentPropertyAsNull() {
    replay(new Event(0, Category.SYSPROP, "p", "null"));

    assertNull(Hook.getProperty("any.key", "p"),
        "a property recorded as absent replays as a real null, not the text null");
  }
}
