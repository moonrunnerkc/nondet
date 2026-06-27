package io.github.moonrunnerkc.nondet.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Resolves the agent jar for a command, with actionable messages when it cannot.
 *
 * <p>An explicit path is used when given and reported as missing when it is not a file. Otherwise
 * the newest runnable jar under {@code agent/target} is chosen through {@link AgentJarLocator}. The
 * tool name is woven into the messages so {@code check} and {@code replay} each say which command
 * failed, and the fix is always named: build the agent jar, or pass an explicit path.
 */
final class AgentJars {

  private static final Path AGENT_TARGET = Path.of("agent", "target");

  private AgentJars() {
  }

  /**
   * Resolves the agent jar, printing an actionable error and returning {@code null} on failure.
   *
   * @param explicit the {@code --agent-jar} value, or {@code null} to search {@code agent/target}
   * @param debug    whether to echo the resolved jar to stderr
   * @param tool     the subcommand name used in messages, for example {@code check}
   * @return the resolved agent jar, or {@code null} when none could be found
   */
  static Path resolve(Path explicit, boolean debug, String tool) {
    if (explicit != null) {
      if (!Files.isRegularFile(explicit)) {
        System.err.println("nondet " + tool + ": agent jar not found at " + explicit
            + "; build it with: mvn -pl agent package");
        return null;
      }
      return explicit;
    }
    final Optional<Path> located;
    try {
      located = AgentJarLocator.newestJar(AGENT_TARGET);
    } catch (final IOException cause) {
      System.err.println("nondet " + tool + ": could not list " + AGENT_TARGET + ": "
          + cause.getMessage());
      return null;
    }
    if (located.isEmpty()) {
      System.err.println("nondet " + tool + ": no agent jar under " + AGENT_TARGET
          + "; build it with: mvn -pl agent package, or pass --agent-jar <path>");
      return null;
    }
    if (debug) {
      System.err.println("nondet " + tool + ": resolved agent jar " + located.get());
    }
    return located.get();
  }
}
