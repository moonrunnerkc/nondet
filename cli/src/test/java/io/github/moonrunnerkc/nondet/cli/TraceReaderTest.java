package io.github.moonrunnerkc.nondet.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.moonrunnerkc.nondet.catalog.CallSite;
import io.github.moonrunnerkc.nondet.catalog.Category;
import io.github.moonrunnerkc.nondet.catalog.Event;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TraceReaderTest {

  @Test
  void readsTraceLinesBackIntoEventsInOrder(@TempDir Path dir) throws IOException {
    final Path trace = dir.resolve("run.trace");
    Files.writeString(trace, "0|TIME|id-a|100\n1|RANDOM|id-b|0.25\n");

    final List<Event> events = TraceReader.readTrace(trace);
    assertEquals(List.of(
        new Event(0, Category.TIME, "id-a", "100"),
        new Event(1, Category.RANDOM, "id-b", "0.25")), events);
  }

  @Test
  void ignoresTrailingBlankLines(@TempDir Path dir) throws IOException {
    final Path trace = dir.resolve("run.trace");
    Files.writeString(trace, "0|TIME|id-a|100\n\n");

    assertEquals(1, TraceReader.readTrace(trace).size());
  }

  @Test
  void readsTheTruncationMarkerAsAFlagNotAnEvent(@TempDir Path dir) throws IOException {
    final Path trace = dir.resolve("run.trace");
    Files.writeString(trace, "0|TIME|id-a|100\n1|TIME|id-a|200\n#truncated\n");

    final TraceReader.Trace result = TraceReader.read(trace);
    assertEquals(2, result.events().size(), "the marker is not an event");
    assertTrue(result.truncated(), "the marker flags the run as truncated");
  }

  @Test
  void aTraceWithoutTheMarkerIsNotTruncated(@TempDir Path dir) throws IOException {
    final Path trace = dir.resolve("run.trace");
    Files.writeString(trace, "0|TIME|id-a|100\n");

    final TraceReader.Trace result = TraceReader.read(trace);
    assertEquals(1, result.events().size());
    assertFalse(result.truncated());
  }

  @Test
  void readsRegistryIntoAMapKeyedById(@TempDir Path dir) throws IOException {
    final Path registry = dir.resolve("run.registry");
    Files.writeString(registry, "id-a|pkg/Demo|run|7|System.nanoTime\n");

    final Map<String, CallSite> sites = TraceReader.readRegistry(registry);
    assertEquals(new CallSite("id-a", "pkg/Demo", "run", 7, "System.nanoTime"), sites.get("id-a"));
  }
}
