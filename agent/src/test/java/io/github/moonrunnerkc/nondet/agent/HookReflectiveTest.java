package io.github.moonrunnerkc.nondet.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.moonrunnerkc.nondet.catalog.Bundle;
import io.github.moonrunnerkc.nondet.catalog.CallSite;
import io.github.moonrunnerkc.nondet.catalog.CallSiteId;
import io.github.moonrunnerkc.nondet.catalog.Category;
import io.github.moonrunnerkc.nondet.catalog.Event;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HookReflectiveTest {

  @BeforeEach
  void clearState() {
    Recorder.reset();
    Registry.reset();
    Replay.reset();
  }

  @AfterEach
  void clearReplay() {
    Replay.reset();
  }

  @Test
  void recordsAReflectiveNanoTimeAttributedToTheCaller() throws Exception {
    final Method nanoTime = System.class.getMethod("nanoTime");
    final Object value = Hook.reflectInvoke(nanoTime, null, new Object[0], "pkg/Caller", "run", 5);

    assertInstanceOf(Long.class, value, "the reflective call returns the real boxed value");
    final List<Event> events = Recorder.snapshot();
    assertEquals(1, events.size(), "the reflective read is recorded");
    assertEquals(Category.TIME, events.get(0).category());
    final CallSite site = Registry.callSites().iterator().next();
    assertEquals("System.nanoTime (reflective)", site.api(),
        "the read is attributed to the reflective caller with the api marked reflective");
    assertEquals("pkg/Caller", site.declaringClass());
  }

  @Test
  void servesAReflectiveNanoTimeUnderReplay() throws Exception {
    final String id = CallSiteId.of("pkg/Caller", "run", 5, "System.nanoTime@reflective");
    Replay.install(ReplayTable.fromBundle(
        Bundle.fromEvents(List.of(new Event(0, Category.TIME, id, "777")))));
    final Method nanoTime = System.class.getMethod("nanoTime");

    final Object value = Hook.reflectInvoke(nanoTime, null, new Object[0], "pkg/Caller", "run", 5);

    assertEquals(777L, value, "replay serves the recorded value, not the live clock");
  }

  @Test
  void passesANonCatalogReflectiveCallStraightThrough() throws Exception {
    final Method length = String.class.getMethod("length");
    final Object value = Hook.reflectInvoke(length, "abc", new Object[0], "pkg/Caller", "run", 7);

    assertEquals(3, value, "a non-catalog reflective call still returns its real value");
    assertTrue(Recorder.snapshot().isEmpty(), "a non-catalog reflective call records nothing");
  }
}
