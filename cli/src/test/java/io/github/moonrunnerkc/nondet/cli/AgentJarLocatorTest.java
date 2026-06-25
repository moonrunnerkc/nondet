package io.github.moonrunnerkc.nondet.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AgentJarLocatorTest {

  private static Path jar(Path dir, String name, long modifiedMillis) throws IOException {
    final Path jar = dir.resolve(name);
    Files.writeString(jar, "");
    Files.setLastModifiedTime(jar, FileTime.fromMillis(modifiedMillis));
    return jar;
  }

  @Test
  void picksTheNewestRunnableJar(@TempDir Path dir) throws IOException {
    jar(dir, "nondet-agent-0.1.0.jar", 1_000L);
    final Path newer = jar(dir, "nondet-agent.jar", 2_000L);

    assertEquals(Optional.of(newer), AgentJarLocator.newestJar(dir));
  }

  @Test
  void ignoresShadeOriginalsAndSourcesAndJavadoc(@TempDir Path dir) throws IOException {
    jar(dir, "original-nondet-agent.jar", 9_000L);
    jar(dir, "nondet-agent-sources.jar", 9_000L);
    jar(dir, "nondet-agent-javadoc.jar", 9_000L);
    final Path runnable = jar(dir, "nondet-agent.jar", 1_000L);

    assertEquals(Optional.of(runnable), AgentJarLocator.newestJar(dir),
        "the runnable jar must win even though it is older than the artifacts that get filtered out");
  }

  @Test
  void returnsEmptyWhenTheDirectoryDoesNotExist() throws IOException {
    assertTrue(AgentJarLocator.newestJar(Path.of("no", "such", "dir")).isEmpty());
  }

  @Test
  void returnsEmptyWhenNoRunnableJarIsPresent(@TempDir Path dir) throws IOException {
    jar(dir, "original-nondet-agent.jar", 1_000L);
    Files.writeString(dir.resolve("notes.txt"), "");

    assertTrue(AgentJarLocator.newestJar(dir).isEmpty());
  }
}
