package io.github.moonrunnerkc.nondet.scan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClassSourceTest {

  @Test
  void readsASingleClassFile() throws IOException {
    final List<ClassEntry> entries = ClassSource.read(ScanFixtures.classFile("NanoTimeProbe"));
    assertEquals(1, entries.size());
    assertTrue(entries.get(0).bytecode().length > 0);
  }

  @Test
  void walksADirectoryForEveryClassFile() throws IOException {
    final List<ClassEntry> entries = ClassSource.read(ScanFixtures.directory());
    assertTrue(entries.size() >= 3,
        "expected at least the three fixture classes but found " + entries.size());
  }

  @Test
  void returnsEntriesSortedByOriginForReproducibility() throws IOException {
    final List<ClassEntry> entries = ClassSource.read(ScanFixtures.directory());
    final List<String> origins = entries.stream().map(ClassEntry::origin).toList();
    final List<String> sorted = origins.stream().sorted().toList();
    assertEquals(sorted, origins);
  }

  @Test
  void rejectsAMissingPath() {
    assertThrows(IOException.class, () -> ClassSource.read(Path.of("does", "not", "exist")));
  }
}
