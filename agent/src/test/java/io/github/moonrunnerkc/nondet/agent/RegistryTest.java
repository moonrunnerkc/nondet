package io.github.moonrunnerkc.nondet.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.moonrunnerkc.nondet.catalog.CallSite;
import io.github.moonrunnerkc.nondet.catalog.Category;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RegistryTest {

  @BeforeEach
  void clearState() {
    Recorder.reset();
    Registry.reset();
  }

  @AfterEach
  void clearProperties() {
    System.clearProperty(Registry.TRACE_PROPERTY);
    System.clearProperty(Registry.REGISTRY_PROPERTY);
  }

  @Test
  void flushWritesBothTraceAndRegistryToConfiguredPaths(@TempDir Path dir) throws IOException {
    final Path trace = dir.resolve("run.trace");
    final Path registry = dir.resolve("run.registry");
    System.setProperty(Registry.TRACE_PROPERTY, trace.toString());
    System.setProperty(Registry.REGISTRY_PROPERTY, registry.toString());
    Registry.register(new CallSite("id-a", "pkg/Demo", "run", 7, "System.nanoTime"));
    Recorder.record(Category.TIME, "id-a", "42");

    Registry.flush();

    assertEquals("0|TIME|id-a|42\n#threads 1\n", Files.readString(trace),
        "the flush records the one recording thread after the events");
    assertEquals("id-a|pkg/Demo|run|7|System.nanoTime\n", Files.readString(registry));
  }

  @Test
  void registryPathDefaultsToTheTracePathPlusSuffix(@TempDir Path dir) throws IOException {
    final Path trace = dir.resolve("run.trace");
    System.setProperty(Registry.TRACE_PROPERTY, trace.toString());
    Registry.register(new CallSite("id-a", "pkg/Demo", "run", 7, "System.nanoTime"));

    Registry.flush();

    assertTrue(Files.exists(Path.of(trace + ".registry")),
        "registry should default to the trace path plus .registry");
  }

  @Test
  void flushWritesNothingWhenNoTracePathIsSet(@TempDir Path dir) {
    Registry.register(new CallSite("id-a", "pkg/Demo", "run", 7, "System.nanoTime"));

    Registry.flush();

    assertFalse(Files.exists(dir.resolve("run.trace")), "no trace path means no output file");
  }
}
