package io.github.moonrunnerkc.nondet.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/**
 * End to end acceptance for the v0.2.0 pivot: {@code nondet check} on {@code FlakyRetry} must
 * report no causal nondeterminism. FlakyRetry reads {@code System.nanoTime} on every attempt, so
 * the recorded values differ run to run, but it always completes the same number of attempts and
 * prints the same line, so its outcome is stable. The old behaviour flagged the clock read; the
 * new behaviour recognises that the read never changed what the program produced.
 *
 * <p>It runs against the packaged agent jar and the compiled examples, so it is skipped when
 * either is missing. A full {@code mvn -DskipTests package} followed by {@code mvn test} builds
 * both before this runs.
 */
class CheckAcceptanceTest {

  @Test
  void flakyRetryReadsTheClockButIsOutcomeStable() throws Exception {
    final Optional<Path> agentJar = AgentJarLocator.newestJar(agentTargetDir());
    final Path examples = repoRoot().resolve("examples").resolve("target").resolve("classes");
    assumeTrue(agentJar.isPresent(), "agent jar not built; run mvn -pl agent package first");
    assumeTrue(Files.isDirectory(examples), "examples not built; run mvn -pl examples package first");

    final ByteArrayOutputStream captured = new ByteArrayOutputStream();
    final PrintStream original = System.out;
    final int code;
    try {
      System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
      code = new CommandLine(new CheckCommand()).execute(
          "--agent-jar", agentJar.get().toString(),
          "--class-path", examples.toString(),
          "--runs", "3",
          "io.github.moonrunnerkc.nondet.examples.FlakyRetry");
    } finally {
      System.setOut(original);
    }

    final String report = captured.toString(StandardCharsets.UTF_8);
    assertEquals(0, code, "FlakyRetry's clock reads do not change its outcome: " + report);
    assertTrue(report.contains("no causal nondeterminism"), report);
  }

  private static Path agentTargetDir() {
    return repoRoot().resolve("agent").resolve("target");
  }

  private static Path repoRoot() {
    return Paths.get("").toAbsolutePath().getParent();
  }
}
