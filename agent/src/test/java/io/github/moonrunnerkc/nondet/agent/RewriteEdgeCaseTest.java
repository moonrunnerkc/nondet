package io.github.moonrunnerkc.nondet.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.moonrunnerkc.nondet.catalog.CallSite;
import io.github.moonrunnerkc.nondet.catalog.CallSiteId;
import io.github.moonrunnerkc.nondet.catalog.Category;
import io.github.moonrunnerkc.nondet.catalog.Event;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Rewrite behavior at the edges: an unsupported overload, an entropy call buried in a
 * lambda, two reads on one line, a class with no line table, a reflective call, and a run
 * that overflows the event cap. Each uses real compiled bytecode through the live
 * transformer.
 */
class RewriteEdgeCaseTest {

  private static final String PROBES = "probe/EntropyProbes";
  private static final String NANO_SITE = "probe/NanoSite";

  @BeforeEach
  void clearState() {
    Recorder.reset();
    Registry.reset();
  }

  @AfterEach
  void clearCapProperty() {
    System.clearProperty(Recorder.MAX_EVENTS_PROPERTY);
    Recorder.reset();
  }

  @Test
  void unsupportedNoArgGetenvRunsWithoutLinkageErrorAndRecordsNothing() throws Exception {
    final Class<?> loaded = Fixtures.transformAndLoad(PROBES);

    final int entryCount = (int) loaded.getMethod("fullEnv").invoke(null);

    assertTrue(entryCount >= 0, "the unrewritten getenv() must still return the real environment");
    assertTrue(Recorder.snapshot().isEmpty(), "the guard leaves getenv() untouched, so it records nothing");
  }

  @Test
  void anEntropyCallInsideALambdaIsStillRewrittenAndRecorded() throws Exception {
    final Class<?> loaded = Fixtures.transformAndLoad(PROBES);

    final long returned = (long) loaded.getMethod("insideLambda").invoke(null);

    final List<Event> events = Recorder.snapshot();
    assertEquals(1, events.size(), "the synthetic lambda method is visited and its read recorded");
    assertEquals(Category.TIME, events.get(0).category());
    assertEquals(Long.toString(returned), events.get(0).value());
    assertTrue(Registry.callSites().stream().anyMatch(site -> site.method().startsWith("lambda$")),
        "the recorded call site lives in the synthetic lambda method");
  }

  @Test
  void twoReadsOnOneSourceLineBothRecordUnderASharedCallSite() throws Exception {
    final Class<?> loaded = Fixtures.transformAndLoad(PROBES);

    final long sum = (long) loaded.getMethod("twoOnOneLine").invoke(null);

    final List<Event> events = Recorder.snapshot();
    assertEquals(2, events.size(), "both reads on the line are recorded");
    assertEquals(events.get(0).callSiteId(), events.get(1).callSiteId(),
        "two reads on one line share a call site id, which is acceptable");
    assertEquals(Long.parseLong(events.get(0).value()) + Long.parseLong(events.get(1).value()), sum,
        "the recorded values are the two reads the method summed");
  }

  @Test
  void aClassWithoutALineTableStillRewritesWithAStableUnknownLineId() throws Exception {
    final byte[] stripped = Fixtures.stripLineNumbers(Fixtures.rawBytes(NANO_SITE));
    final Class<?> loaded = Fixtures.load(NANO_SITE, stripped);

    final long returned = (long) loaded.getMethod("read").invoke(null);

    final List<Event> events = Recorder.snapshot();
    assertEquals(1, events.size(), "a missing line table does not stop the rewrite");
    final String expectedId = CallSiteId.of("probe/NanoSite", "read", -1, "System.nanoTime");
    assertEquals(expectedId, events.get(0).callSiteId(),
        "the id derives from an unknown line and is stable");
    final CallSite site = Registry.callSites().iterator().next();
    assertEquals(-1, site.line(), "an absent line is recorded as unknown, not invented");
    assertEquals(Long.toString(returned), events.get(0).value());
  }

  @Test
  void aReflectiveEntropyCallIsNotInstrumented() throws Exception {
    final Class<?> loaded = Fixtures.transformAndLoad(PROBES);

    loaded.getMethod("reflectiveNanoTime").invoke(null);

    assertTrue(Recorder.snapshot().isEmpty(),
        "a System.nanoTime reached through Method.invoke is the known reflection blind spot");
  }

  @Test
  void aRunThatOverflowsTheEventCapRecordsExactlyTheCapAndFlagsTruncation() throws Exception {
    System.setProperty(Recorder.MAX_EVENTS_PROPERTY, "5");
    Recorder.reset();
    final Class<?> loaded = Fixtures.transformAndLoad(PROBES);

    loaded.getMethod("spin", int.class).invoke(null, 200);

    assertEquals(5, Recorder.snapshot().size(), "recording stops at the cap even under a tight loop");
    assertTrue(Recorder.truncated(), "overflowing the cap marks the run truncated");
    assertFalse(Recorder.snapshot().isEmpty());
  }
}
