package io.github.moonrunnerkc.nondet.cli;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Finds a built agent jar so {@code nondet check} can run without an explicit path.
 *
 * <p>The shade plugin leaves several jars in {@code agent/target}: the runnable agent jar
 * and an {@code original-} copy of the pre-shade jar, plus {@code -sources} and
 * {@code -javadoc} jars when those are built. Only the runnable jar carries the agent
 * manifest, so the others are filtered out and the newest survivor is chosen.
 */
public final class AgentJarLocator {

  private AgentJarLocator() {
  }

  /**
   * Returns the newest runnable agent jar in a directory.
   *
   * <p>Jars whose name starts with {@code original-} or contains {@code -sources} or
   * {@code -javadoc} are ignored. Newest is by last modified time; a missing directory
   * yields an empty result rather than an error.
   *
   * @param directory the directory to search, typically {@code agent/target}, never {@code null}
   * @return the chosen jar, or {@link Optional#empty()} if the directory holds no runnable jar
   * @throws IOException if the directory cannot be listed
   */
  public static Optional<Path> newestJar(Path directory) throws IOException {
    if (!Files.isDirectory(directory)) {
      return Optional.empty();
    }
    try (Stream<Path> files = Files.list(directory)) {
      return files
          .filter(Files::isRegularFile)
          .filter(AgentJarLocator::isRunnableJar)
          .max(Comparator.comparingLong(AgentJarLocator::lastModifiedMillis));
    }
  }

  private static boolean isRunnableJar(Path jar) {
    final String name = jar.getFileName().toString();
    return name.endsWith(".jar")
        && !name.startsWith("original-")
        && !name.contains("-sources")
        && !name.contains("-javadoc");
  }

  private static long lastModifiedMillis(Path jar) {
    try {
      return Files.getLastModifiedTime(jar).toMillis();
    } catch (final IOException cause) {
      throw new UncheckedIOException("failed to read the modified time of " + jar, cause);
    }
  }
}
