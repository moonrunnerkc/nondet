package io.github.moonrunnerkc.nondet.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.moonrunnerkc.nondet.catalog.Catalog;
import io.github.moonrunnerkc.nondet.catalog.Category;
import io.github.moonrunnerkc.nondet.catalog.Event;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Behavior of the call site rewrite: a matched entropy call must route through the agent
 * runtime and record exactly the value it returned.
 */
class EntropyMethodVisitorTest {

  @BeforeEach
  void clearState() {
    Recorder.reset();
    Registry.reset();
  }

  @Test
  void rewritesNanoTimeSoInvocationRecordsTheReturnedValue() throws Exception {
    final byte[] original = Files.readAllBytes(fixtureClassFile());
    final byte[] rewritten = new EntropyTransformer(Catalog.ofDefault())
        .transform(getClass().getClassLoader(), "probe/NanoSite", null, null, original);

    final Class<?> loaded = new BytesLoader().define("probe.NanoSite", rewritten);
    final long returned = (long) loaded.getMethod("read").invoke(null);

    final List<Event> events = Recorder.snapshot();
    assertEquals(1, events.size(), "the instrumented call must record exactly one event");
    assertEquals(1, Registry.callSites().size(), "the rewrite must register the call site");
    final Event recorded = events.get(0);
    assertEquals(Category.TIME, recorded.category(), "a System.nanoTime read is a TIME source");
    assertEquals(Long.toString(returned), recorded.value(),
        "the recorded value must be exactly what the instrumented call returned");
  }

  @Test
  void registersTheCallSiteWithItsSourceCoordinates() throws Exception {
    final byte[] original = Files.readAllBytes(fixtureClassFile());
    new EntropyTransformer(Catalog.ofDefault())
        .transform(getClass().getClassLoader(), "probe/NanoSite", null, null, original);

    assertEquals("probe/NanoSite", Registry.callSites().iterator().next().declaringClass());
  }

  private static Path fixtureClassFile() throws Exception {
    return Paths.get(EntropyMethodVisitorTest.class.getResource("/probe/NanoSite.class").toURI());
  }

  private static final class BytesLoader extends ClassLoader {
    Class<?> define(String name, byte[] bytes) {
      return defineClass(name, bytes, 0, bytes.length);
    }
  }
}
