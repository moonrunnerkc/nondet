package io.github.moonrunnerkc.nondet.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.moonrunnerkc.nondet.catalog.Catalog;
import io.github.moonrunnerkc.nondet.catalog.Event;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A class the transformer cannot read must not crash or abort the run: its original bytes
 * are kept and later classes still instrument. The failure is silent by default and named
 * on stderr only when {@code nondet.debug} is set.
 */
class EntropyTransformerTest {

  private static final byte[] NOT_A_CLASS = {0x00, 0x01, 0x02, 0x03, 0x04};

  @BeforeEach
  void clearState() {
    Recorder.reset();
    Registry.reset();
  }

  @Test
  void unreadableBytesAreKeptUnchangedRatherThanThrowing() {
    final byte[] result = new EntropyTransformer(Catalog.ofDefault())
        .transform(getClass().getClassLoader(), "pkg/Broken", null, null, NOT_A_CLASS);
    assertNull(result, "a failed transform returns null so the JVM keeps the original bytes");
  }

  @Test
  void aClassThatFailsToTransformDoesNotStopALaterClassFromInstrumenting() throws Exception {
    final EntropyTransformer transformer = new EntropyTransformer(Catalog.ofDefault());
    assertNull(transformer.transform(getClass().getClassLoader(), "pkg/Broken", null, null, NOT_A_CLASS));

    final Class<?> probes = Fixtures.transformAndLoad("probe/EntropyProbes");
    probes.getMethod("nanoTime").invoke(null);

    final List<Event> events = Recorder.snapshot();
    assertEquals(1, events.size(), "the valid class still instruments after a failed transform");
  }

  @Test
  void debugNamesTheClassThatFailedToTransform() {
    final ByteArrayOutputStream captured = new ByteArrayOutputStream();
    final PrintStream original = System.err;
    System.setProperty(EntropyTransformer.DEBUG_PROPERTY, "true");
    try {
      System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));
      new EntropyTransformer(Catalog.ofDefault())
          .transform(getClass().getClassLoader(), "pkg/Broken", null, null, NOT_A_CLASS);
    } finally {
      System.setErr(original);
      System.clearProperty(EntropyTransformer.DEBUG_PROPERTY);
    }
    assertTrue(captured.toString(StandardCharsets.UTF_8).contains("pkg.Broken"),
        captured.toString(StandardCharsets.UTF_8));
  }

  @Test
  void quietByDefaultWhenAClassFailsToTransform() {
    final ByteArrayOutputStream captured = new ByteArrayOutputStream();
    final PrintStream original = System.err;
    try {
      System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));
      new EntropyTransformer(Catalog.ofDefault())
          .transform(getClass().getClassLoader(), "pkg/Broken", null, null, NOT_A_CLASS);
    } finally {
      System.setErr(original);
    }
    assertFalse(captured.toString(StandardCharsets.UTF_8).contains("pkg.Broken"),
        "without nondet.debug a failed transform is silent");
  }
}
