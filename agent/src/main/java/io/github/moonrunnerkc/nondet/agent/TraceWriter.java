package io.github.moonrunnerkc.nondet.agent;

import io.github.moonrunnerkc.nondet.catalog.CallSite;
import io.github.moonrunnerkc.nondet.catalog.Event;
import io.github.moonrunnerkc.nondet.catalog.WireFormat;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Writes traces and call site registries in the shared wire format.
 *
 * <p>Both files are UTF-8 with one record per line and LF line endings. Events write in
 * sequence order; registry entries write sorted by call site id. The reader on the cli
 * side uses the same {@link WireFormat}, so the round trip is exact. Output is sorted so
 * two runs over identical input produce byte identical files.
 */
public final class TraceWriter {

  private TraceWriter() {
  }

  /**
   * Writes events to a trace file, one {@code seq|category|callSiteId|value} line each.
   *
   * @param path   the destination file; parent directories must already exist
   * @param events the events to write, written in their given order
   * @throws IOException if the file cannot be written
   */
  public static void writeTrace(Path path, List<Event> events) throws IOException {
    writeTrace(path, events, false);
  }

  /**
   * Writes events to a trace file, appending the truncation marker when the run was capped.
   *
   * @param path      the destination file; parent directories must already exist
   * @param events    the events to write, written in their given order
   * @param truncated whether the run hit the event cap, in which case a trailing
   *     {@link WireFormat#TRUNCATION_MARKER} line is written so a reader knows the trace is
   *     a prefix of what the run read
   * @throws IOException if the file cannot be written
   */
  public static void writeTrace(Path path, List<Event> events, boolean truncated) throws IOException {
    try (Writer out = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      for (final Event event : events) {
        out.write(WireFormat.formatTrace(event));
        out.write('\n');
      }
      if (truncated) {
        out.write(WireFormat.TRUNCATION_MARKER);
        out.write('\n');
      }
    }
  }

  /**
   * Writes call sites to a registry file, one {@code callSiteId|class|method|line|api}
   * line each, sorted by call site id.
   *
   * @param path      the destination file; parent directories must already exist
   * @param callSites the call sites to write
   * @throws IOException if the file cannot be written
   */
  public static void writeRegistry(Path path, Collection<CallSite> callSites) throws IOException {
    final List<CallSite> sorted = new ArrayList<>(callSites);
    sorted.sort(Comparator.comparing(CallSite::callSiteId));
    try (Writer out = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      for (final CallSite callSite : sorted) {
        out.write(WireFormat.formatRegistry(callSite));
        out.write('\n');
      }
    }
  }
}
