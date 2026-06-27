package io.github.moonrunnerkc.nondet.cli;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Deletes a checker work directory and everything under it.
 *
 * <p>A check, a replay, and a causal search each run children in a scratch directory of traces,
 * registries, captured streams, and bundles. When the run is not kept, the directory is removed
 * depth first so a non-empty directory never blocks its own deletion.
 */
final class WorkDirs {

  private WorkDirs() {
  }

  /**
   * Removes a directory tree, ignoring a directory that is already gone.
   *
   * @param dir the directory to delete, never {@code null}
   * @throws IOException if a file under the directory cannot be deleted
   */
  static void deleteRecursively(Path dir) throws IOException {
    if (!Files.exists(dir)) {
      return;
    }
    try (Stream<Path> entries = Files.walk(dir)) {
      entries.sorted(Comparator.reverseOrder()).forEach(path -> {
        try {
          Files.deleteIfExists(path);
        } catch (final IOException cause) {
          throw new UncheckedIOException("failed to delete temp file " + path, cause);
        }
      });
    }
  }
}
