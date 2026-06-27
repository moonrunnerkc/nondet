package io.github.moonrunnerkc.nondet.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BundleIOTest {

  @Test
  void aBundleRoundTripsThroughItsFile(@TempDir Path dir) throws IOException {
    final Bundle bundle = Bundle.fromEvents(List.of(
        new Event(0, Category.TIME, "siteA", "10"),
        new Event(1, Category.SYSPROP, "siteB", "value|with\\separators"),
        new Event(2, Category.TIME, "siteA", "11")));
    final Path file = dir.resolve("run.bundle");

    BundleIO.write(file, bundle);
    final Bundle reloaded = BundleIO.read(file);

    assertEquals(bundle.entries(), reloaded.entries(),
        "escaped separators in a value survive the round trip");
  }

  @Test
  void theFileStartsWithASelfDescribingHeader(@TempDir Path dir) throws IOException {
    final Path file = dir.resolve("run.bundle");
    BundleIO.write(file, Bundle.fromEvents(List.of(new Event(0, Category.TIME, "siteA", "1"))));

    assertTrue(Files.readString(file, StandardCharsets.UTF_8).startsWith(BundleIO.HEADER));
  }

  @Test
  void aFileWithoutTheHeaderIsRejected(@TempDir Path dir) throws IOException {
    final Path file = dir.resolve("not-a-bundle.txt");
    Files.writeString(file, "0|siteA|0|TIME|1\n", StandardCharsets.UTF_8);

    assertThrows(IllegalArgumentException.class, () -> BundleIO.read(file));
  }
}
