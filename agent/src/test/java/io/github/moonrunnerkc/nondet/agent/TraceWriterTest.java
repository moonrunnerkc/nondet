package io.github.moonrunnerkc.nondet.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.moonrunnerkc.nondet.catalog.CallSite;
import io.github.moonrunnerkc.nondet.catalog.Category;
import io.github.moonrunnerkc.nondet.catalog.Event;
import io.github.moonrunnerkc.nondet.catalog.WireFormat;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TraceWriterTest {

  @Test
  void writesOneLfTerminatedLinePerEvent(@TempDir Path dir) throws IOException {
    final Path trace = dir.resolve("trace.txt");
    final List<Event> events = List.of(
        new Event(0, Category.TIME, "id-a", "100"),
        new Event(1, Category.RANDOM, "id-b", "0.5"));

    TraceWriter.writeTrace(trace, events);

    final String text = Files.readString(trace, StandardCharsets.UTF_8);
    assertEquals("0|TIME|id-a|100\n1|RANDOM|id-b|0.5\n", text);
  }

  @Test
  void traceLinesParseBackToTheOriginalEvents(@TempDir Path dir) throws IOException {
    final Path trace = dir.resolve("trace.txt");
    final List<Event> events = List.of(
        new Event(0, Category.ENV, "id-a", "PATH=/usr/bin|/bin"),
        new Event(1, Category.SYSPROP, "id-b", "value\nwith newline"));

    TraceWriter.writeTrace(trace, events);

    final List<Event> readBack = Files.readAllLines(trace, StandardCharsets.UTF_8)
        .stream().map(WireFormat::parseTrace).toList();
    assertEquals(events, readBack);
  }

  @Test
  void registryWritesSortedByCallSiteId(@TempDir Path dir) throws IOException {
    final Path registry = dir.resolve("registry.txt");
    TraceWriter.writeRegistry(registry, List.of(
        new CallSite("ff", "pkg/B", "g", 2, "Math.random"),
        new CallSite("00", "pkg/A", "f", 1, "System.nanoTime")));

    final List<String> ids = Files.readAllLines(registry, StandardCharsets.UTF_8)
        .stream().map(line -> WireFormat.parseRegistry(line).callSiteId()).toList();
    assertEquals(List.of("00", "ff"), ids);
  }
}
