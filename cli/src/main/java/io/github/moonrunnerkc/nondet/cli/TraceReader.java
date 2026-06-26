package io.github.moonrunnerkc.nondet.cli;

import io.github.moonrunnerkc.nondet.catalog.CallSite;
import io.github.moonrunnerkc.nondet.catalog.Event;
import io.github.moonrunnerkc.nondet.catalog.WireFormat;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads trace and registry files written by the agent back into typed records.
 *
 * <p>Both readers use the same {@link WireFormat} the agent writes with, so parsing is
 * the exact inverse of writing. Blank lines are skipped so a trailing newline does not
 * produce a phantom record, and the truncation and thread-count markers are read as
 * metadata rather than events.
 */
public final class TraceReader {

  private TraceReader() {
  }

  /**
   * A trace's events together with the metadata markers the agent appended.
   *
   * @param events      the events in the order they appear, never {@code null}
   * @param truncated   whether the run was truncated, so the events are a prefix of the run
   * @param threadCount the number of threads that produced events, or zero when the trace
   *     carries no thread-count marker
   */
  public record Trace(List<Event> events, boolean truncated, int threadCount) {
  }

  /**
   * Reads a trace file into events, in file order, dropping the truncation marker.
   *
   * @param path the trace file to read, never {@code null}
   * @return the events in the order they appear
   * @throws IOException if the file cannot be read
   * @throws IllegalArgumentException if a line is not a valid trace record
   */
  public static List<Event> readTrace(Path path) throws IOException {
    return read(path).events();
  }

  /**
   * Reads a trace file into its events and truncation flag.
   *
   * @param path the trace file to read, never {@code null}
   * @return the events and whether the trace ended with the truncation marker
   * @throws IOException if the file cannot be read
   * @throws IllegalArgumentException if a line is not a valid trace record
   */
  public static Trace read(Path path) throws IOException {
    final List<Event> events = new ArrayList<>();
    boolean truncated = false;
    int threadCount = 0;
    for (final String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
      if (line.isBlank()) {
        continue;
      }
      if (WireFormat.isTruncationMarker(line)) {
        truncated = true;
      } else if (WireFormat.isThreadCountMarker(line)) {
        threadCount = WireFormat.parseThreadCount(line);
      } else {
        events.add(WireFormat.parseTrace(line));
      }
    }
    return new Trace(List.copyOf(events), truncated, threadCount);
  }

  /**
   * Reads a registry file into a map from call site id to coordinates.
   *
   * @param path the registry file to read, never {@code null}
   * @return a map keyed by call site id, preserving file order for stable iteration
   * @throws IOException if the file cannot be read
   * @throws IllegalArgumentException if a line is not a valid registry record
   */
  public static Map<String, CallSite> readRegistry(Path path) throws IOException {
    final Map<String, CallSite> sites = new LinkedHashMap<>();
    for (final String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
      if (line.isBlank()) {
        continue;
      }
      final CallSite site = WireFormat.parseRegistry(line);
      sites.put(site.callSiteId(), site);
    }
    return sites;
  }
}
