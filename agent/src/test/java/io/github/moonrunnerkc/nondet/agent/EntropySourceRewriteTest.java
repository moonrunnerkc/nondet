package io.github.moonrunnerkc.nondet.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.moonrunnerkc.nondet.catalog.Category;
import io.github.moonrunnerkc.nondet.catalog.Event;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Each of the six catalog sources, and the overloads that carry a String argument, must
 * route through the agent runtime after the rewrite and record exactly the value the live
 * call returned. The fixtures return the value they read, so the recorded value and the
 * method's return value are compared directly.
 */
class EntropySourceRewriteTest {

  private static final String PROBES = "probe/EntropyProbes";

  @BeforeEach
  void clearState() {
    Recorder.reset();
    Registry.reset();
  }

  @Test
  void nanoTimeRecordsATimeReadMatchingTheReturn() throws Exception {
    final long returned = (long) invoke("nanoTime");
    final Event recorded = onlyEvent();
    assertEquals(Category.TIME, recorded.category());
    assertEquals(Long.toString(returned), recorded.value());
  }

  @Test
  void currentTimeMillisRecordsATimeReadMatchingTheReturn() throws Exception {
    final long returned = (long) invoke("currentTimeMillis");
    final Event recorded = onlyEvent();
    assertEquals(Category.TIME, recorded.category());
    assertEquals(Long.toString(returned), recorded.value());
  }

  @Test
  void mathRandomRecordsARandomReadMatchingTheReturn() throws Exception {
    final double returned = (double) invoke("random");
    final Event recorded = onlyEvent();
    assertEquals(Category.RANDOM, recorded.category());
    assertEquals(Double.toString(returned), recorded.value());
  }

  @Test
  void randomUuidRecordsARandomReadMatchingTheReturn() throws Exception {
    final UUID returned = (UUID) invoke("uuid");
    final Event recorded = onlyEvent();
    assertEquals(Category.RANDOM, recorded.category());
    assertEquals(returned.toString(), recorded.value());
  }

  @Test
  void getenvWithANameRecordsAnEnvReadMatchingTheReturn() throws Exception {
    final Object returned = invoke("getenvVar", new Class<?>[] {String.class}, "PATH");
    final Event recorded = onlyEvent();
    assertEquals(Category.ENV, recorded.category());
    assertEquals(String.valueOf(returned), recorded.value());
  }

  @Test
  void getPropertyWithAKeyRecordsASyspropReadMatchingTheReturn() throws Exception {
    final Object returned = invoke("property", new Class<?>[] {String.class}, "java.version");
    final Event recorded = onlyEvent();
    assertEquals(Category.SYSPROP, recorded.category());
    assertEquals(String.valueOf(returned), recorded.value());
  }

  @Test
  void getPropertyWithAFallbackRecordsASyspropReadMatchingTheReturn() throws Exception {
    final Object returned = invoke("propertyOrDefault",
        new Class<?>[] {String.class, String.class}, "nondet.absent.property", "fallback-value");
    final Event recorded = onlyEvent();
    assertEquals(Category.SYSPROP, recorded.category());
    assertEquals("fallback-value", returned);
    assertEquals("fallback-value", recorded.value());
  }

  private static Object invoke(String method) throws Exception {
    return invoke(method, new Class<?>[0]);
  }

  private static Object invoke(String method, Class<?>[] paramTypes, Object... args) throws Exception {
    final Class<?> loaded = Fixtures.transformAndLoad(PROBES);
    return loaded.getMethod(method, paramTypes).invoke(null, args);
  }

  private static Event onlyEvent() {
    final List<Event> events = Recorder.snapshot();
    assertEquals(1, events.size(), "the instrumented call must record exactly one event");
    return events.get(0);
  }
}
