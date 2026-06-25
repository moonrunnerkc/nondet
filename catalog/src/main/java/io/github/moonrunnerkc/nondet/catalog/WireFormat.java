package io.github.moonrunnerkc.nondet.catalog;

import java.util.ArrayList;
import java.util.List;

/**
 * The on disk encoding for trace and registry lines.
 *
 * <p>A trace line is {@code seq|category|callSiteId|value}. A registry line is
 * {@code callSiteId|class|method|line|api}. Both are plain UTF-8, one record per line,
 * LF terminated by the writer. Fields are pipe separated; any pipe, backslash, or line
 * break inside a field is escaped so a raw pipe is always a separator and a record never
 * spans lines. The writer and reader on either side of a trace share this class, so the
 * round trip is exact.
 *
 * <p>A trace may end with the {@value #TRUNCATION_MARKER} marker line when the run hit the
 * event cap. It begins with {@code #}, which no sequence number does, so a reader can tell
 * it apart from a record without ambiguity.
 */
public final class WireFormat {

  private static final char FIELD_SEPARATOR = '|';
  private static final char ESCAPE = '\\';
  private static final int TRACE_FIELDS = 4;
  private static final int REGISTRY_FIELDS = 5;

  /** The trailing trace line that marks a run as truncated at the event cap. */
  public static final String TRUNCATION_MARKER = "#truncated";

  private WireFormat() {
  }

  /**
   * Reports whether a line is the truncation marker rather than a trace record.
   *
   * @param line a raw line from a trace file, never {@code null}
   * @return {@code true} when the line is {@value #TRUNCATION_MARKER}
   */
  public static boolean isTruncationMarker(String line) {
    return TRUNCATION_MARKER.equals(line);
  }

  /**
   * Encodes an event as a single trace line, without the trailing newline.
   *
   * @param event the event to encode, never {@code null}
   * @return the encoded line {@code seq|category|callSiteId|value}
   */
  public static String formatTrace(Event event) {
    return event.seq()
        + String.valueOf(FIELD_SEPARATOR) + event.category().name()
        + FIELD_SEPARATOR + encode(event.callSiteId())
        + FIELD_SEPARATOR + encode(event.value());
  }

  /**
   * Parses one trace line back into an event.
   *
   * @param line a trace line as produced by {@link #formatTrace}, without a trailing newline
   * @return the decoded event
   * @throws IllegalArgumentException if the line does not have four fields or the
   *     sequence or category cannot be parsed
   */
  public static Event parseTrace(String line) {
    final List<String> fields = split(line);
    if (fields.size() != TRACE_FIELDS) {
      throw new IllegalArgumentException(
          "trace line must have " + TRACE_FIELDS + " fields but had " + fields.size() + ": " + line);
    }
    final long seq = parseLong(fields.get(0), "seq", line);
    final Category category = parseCategory(fields.get(1), line);
    return new Event(seq, category, fields.get(2), fields.get(3));
  }

  /**
   * Encodes a call site as a single registry line, without the trailing newline.
   *
   * @param callSite the call site to encode, never {@code null}
   * @return the encoded line {@code callSiteId|class|method|line|api}
   */
  public static String formatRegistry(CallSite callSite) {
    return encode(callSite.callSiteId())
        + FIELD_SEPARATOR + encode(callSite.declaringClass())
        + FIELD_SEPARATOR + encode(callSite.method())
        + FIELD_SEPARATOR + callSite.line()
        + FIELD_SEPARATOR + encode(callSite.api());
  }

  /**
   * Parses one registry line back into a call site.
   *
   * @param line a registry line as produced by {@link #formatRegistry}, without a trailing newline
   * @return the decoded call site
   * @throws IllegalArgumentException if the line does not have five fields or the line
   *     number cannot be parsed
   */
  public static CallSite parseRegistry(String line) {
    final List<String> fields = split(line);
    if (fields.size() != REGISTRY_FIELDS) {
      throw new IllegalArgumentException(
          "registry line must have " + REGISTRY_FIELDS + " fields but had " + fields.size() + ": " + line);
    }
    final int sourceLine = (int) parseLong(fields.get(3), "line", line);
    return new CallSite(fields.get(0), fields.get(1), fields.get(2), sourceLine, fields.get(4));
  }

  private static String encode(String field) {
    final StringBuilder out = new StringBuilder(field.length());
    for (int i = 0; i < field.length(); i++) {
      final char c = field.charAt(i);
      switch (c) {
        case ESCAPE -> out.append(ESCAPE).append(ESCAPE);
        case FIELD_SEPARATOR -> out.append(ESCAPE).append(FIELD_SEPARATOR);
        case '\n' -> out.append(ESCAPE).append('n');
        case '\r' -> out.append(ESCAPE).append('r');
        default -> out.append(c);
      }
    }
    return out.toString();
  }

  private static List<String> split(String line) {
    final List<String> fields = new ArrayList<>();
    final StringBuilder current = new StringBuilder();
    for (int i = 0; i < line.length(); i++) {
      final char c = line.charAt(i);
      if (c == ESCAPE && i + 1 < line.length()) {
        final char next = line.charAt(++i);
        switch (next) {
          case 'n' -> current.append('\n');
          case 'r' -> current.append('\r');
          default -> current.append(next);
        }
      } else if (c == FIELD_SEPARATOR) {
        fields.add(current.toString());
        current.setLength(0);
      } else {
        current.append(c);
      }
    }
    fields.add(current.toString());
    return fields;
  }

  private static long parseLong(String field, String name, String line) {
    try {
      return Long.parseLong(field);
    } catch (final NumberFormatException cause) {
      throw new IllegalArgumentException(
          "expected an integer " + name + " but found '" + field + "' in line: " + line, cause);
    }
  }

  private static Category parseCategory(String field, String line) {
    try {
      return Category.valueOf(field);
    } catch (final IllegalArgumentException cause) {
      throw new IllegalArgumentException(
          "unknown category '" + field + "' in trace line: " + line, cause);
    }
  }
}
