package io.github.moonrunnerkc.nondet.catalog;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes {@link Bundle} files in the shared wire format.
 *
 * <p>A bundle file is self-describing: a {@value #HEADER} marker line, then one
 * {@link WireFormat#formatBundle} record per entry, UTF-8 with LF endings. The header lets a reader
 * reject a file that is not a bundle instead of misparsing it, and lets the format carry a version
 * if it ever changes. Both the agent, which reads a bundle to replay it, and the cli, which writes a
 * bundle from a recorded run, go through this one class so the round trip is exact.
 */
public final class BundleIO {

  /** The first line of every bundle file, identifying the format and its version. */
  public static final String HEADER = "#nondet-bundle 1";

  private BundleIO() {
  }

  /**
   * Writes a bundle to a file, header first, one entry per line.
   *
   * @param path   the destination file; its parent directory must already exist
   * @param bundle the bundle to write, never {@code null}
   * @throws IOException if the file cannot be written
   */
  public static void write(Path path, Bundle bundle) throws IOException {
    try (Writer out = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      out.write(HEADER);
      out.write('\n');
      for (final BundleEntry entry : bundle.entries()) {
        out.write(WireFormat.formatBundle(entry));
        out.write('\n');
      }
    }
  }

  /**
   * Reads a bundle file written by {@link #write}.
   *
   * @param path the bundle file to read, never {@code null}
   * @return the decoded bundle
   * @throws IOException if the file cannot be read
   * @throws IllegalArgumentException if the file is missing its header or a line does not parse
   */
  public static Bundle read(Path path) throws IOException {
    final List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
    final List<BundleEntry> entries = new ArrayList<>();
    boolean sawHeader = false;
    for (final String line : lines) {
      if (line.isBlank()) {
        continue;
      }
      if (!sawHeader) {
        if (!HEADER.equals(line)) {
          throw new IllegalArgumentException(
              "expected a bundle file starting with '" + HEADER + "' but found: " + line);
        }
        sawHeader = true;
        continue;
      }
      entries.add(WireFormat.parseBundle(line));
    }
    if (!sawHeader) {
      throw new IllegalArgumentException(
          "the bundle file " + path + " is empty; it must start with '" + HEADER + "'");
    }
    return new Bundle(entries);
  }
}
