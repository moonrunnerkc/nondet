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
 * End to end acceptance for the manual work: {@code nondet check} on {@code FlakyRetry}
 * must report a divergence at the {@code System.nanoTime} call site, a TIME source.
 *
 * <p>It runs against the packaged agent jar and the compiled examples, so it is skipped
 * when either is missing. A full {@code mvn -DskipTests package} followed by {@code mvn
 * test} builds both before this runs.
 */
class CheckAcceptanceTest {

  @Test
  void checkReportsTheNanoTimeDivergenceInFlakyRetry() throws Exception {
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
          "io.github.moonrunnerkc.nondet.examples.FlakyRetry");
    } finally {
      System.setOut(original);
    }

    final String report = captured.toString(StandardCharsets.UTF_8);
    assertEquals(1, code, "two runs of FlakyRetry must diverge");
    assertTrue(report.contains("divergence"), report);
    assertTrue(report.contains("FlakyRetry"), report);
    assertTrue(report.contains("System.nanoTime"), report);
  }

  private static Path agentTargetDir() {
    return repoRoot().resolve("agent").resolve("target");
  }

  private static Path repoRoot() {
    return Paths.get("").toAbsolutePath().getParent();
  }
}
