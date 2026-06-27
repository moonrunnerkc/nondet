package io.github.moonrunnerkc.nondet.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * End to end behavior of multi-run checking and the captured child IO. A three-run check
 * still pins the nanoTime divergence in FlakyRetry, a three-run check of an entropy-free
 * workload reports agreement, and the workload's own stdout never leaks into the report.
 *
 * <p>The tests need the packaged agent jar and compiled fixtures, so each is skipped when a
 * prerequisite is missing. A full {@code mvn -DskipTests package} followed by {@code mvn
 * test} builds them first.
 */
class CheckMultiRunTest {

  @Test
  void threeRunsOfFlakyRetryAreOutcomeStable() {
    final Path examples = repoRoot().resolve("examples").resolve("target").resolve("classes");
    assumeTrue(agentJar().isPresent(), "agent jar not built; run mvn -pl agent package first");
    assumeTrue(Files.isDirectory(examples), "examples not built; run mvn -pl examples package first");

    final Capture run = check(
        "--agent-jar", agentJar().get().toString(),
        "--class-path", examples.toString(),
        "--runs", "3",
        "io.github.moonrunnerkc.nondet.examples.FlakyRetry");

    assertEquals(0, run.code(), "FlakyRetry always reaches the same result: " + run.out());
    assertTrue(run.out().contains("no causal nondeterminism"), run.out());
  }

  @Test
  void threeRunsOfAnEntropyFreeWorkloadReportNoReadsObserved() {
    assumeTrue(agentJar().isPresent(), "agent jar not built; run mvn -pl agent package first");

    final Capture run = check(
        "--agent-jar", agentJar().get().toString(),
        "--class-path", testClasses().toString(),
        "--runs", "3",
        "probe.DeterministicWorkload");

    assertEquals(0, run.code(), "a workload that reads nothing exits zero");
    assertTrue(run.out().contains("no entropy reads were observed across 3 runs"), run.out());
    assertTrue(run.out().contains("not a proof of determinism"), run.out());
  }

  @Test
  void aWorkloadReadingAStablePropertyReportsNoDivergence() {
    assumeTrue(agentJar().isPresent(), "agent jar not built; run mvn -pl agent package first");

    final Capture run = check(
        "--agent-jar", agentJar().get().toString(),
        "--class-path", testClasses().toString(),
        "--runs", "3",
        "probe.StablePropertyWorkload");

    assertEquals(0, run.code(), "a stable property read agrees across runs");
    assertTrue(run.out().contains("no causal nondeterminism"), run.out());
  }

  @Test
  void theReportDoesNotContainWorkloadStdout() {
    final Path examples = repoRoot().resolve("examples").resolve("target").resolve("classes");
    assumeTrue(agentJar().isPresent(), "agent jar not built; run mvn -pl agent package first");
    assumeTrue(Files.isDirectory(examples), "examples not built; run mvn -pl examples package first");

    final Capture run = check(
        "--agent-jar", agentJar().get().toString(),
        "--class-path", examples.toString(),
        "io.github.moonrunnerkc.nondet.examples.FlakyRetry");

    assertTrue(run.out().startsWith("nondet check:"), run.out());
    assertFalse(run.out().lines().anyMatch(line -> line.equals("8")),
        "the FlakyRetry attempt count printed by the workload must not be in the report");
  }

  @Test
  void maxEventsCapIsForwardedSoAChildIsReportedAsTruncated() {
    final Path examples = repoRoot().resolve("examples").resolve("target").resolve("classes");
    assumeTrue(agentJar().isPresent(), "agent jar not built; run mvn -pl agent package first");
    assumeTrue(Files.isDirectory(examples), "examples not built; run mvn -pl examples package first");

    final Capture run = check(
        "--agent-jar", agentJar().get().toString(),
        "--class-path", examples.toString(),
        "--max-events", "1",
        "--runs", "3",
        "io.github.moonrunnerkc.nondet.examples.FlakyRetry");

    assertTrue(run.out().contains("hit the event cap"), run.out());
    assertTrue(run.out().contains("limited to the recorded reads"), run.out());
  }

  @Test
  void showWorkloadOptsIntoPrintingCapturedOutput() {
    final Path examples = repoRoot().resolve("examples").resolve("target").resolve("classes");
    assumeTrue(agentJar().isPresent(), "agent jar not built; run mvn -pl agent package first");
    assumeTrue(Files.isDirectory(examples), "examples not built; run mvn -pl examples package first");

    final Capture run = check(
        "--agent-jar", agentJar().get().toString(),
        "--class-path", examples.toString(),
        "--show-workload",
        "io.github.moonrunnerkc.nondet.examples.FlakyRetry");

    assertTrue(run.out().contains("=== workload output, run 1 ==="), run.out());
    assertTrue(run.out().lines().anyMatch(line -> line.equals("8")),
        "with --show-workload the captured attempt count is printed");
  }

  private static Capture check(String... args) {
    final ByteArrayOutputStream captured = new ByteArrayOutputStream();
    final PrintStream original = System.out;
    final int code;
    try {
      System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
      code = new CommandLine(new CheckCommand()).execute(args);
    } finally {
      System.setOut(original);
    }
    return new Capture(code, captured.toString(StandardCharsets.UTF_8));
  }

  private static Optional<Path> agentJar() {
    try {
      return AgentJarLocator.newestJar(repoRoot().resolve("agent").resolve("target"));
    } catch (final Exception cause) {
      return Optional.empty();
    }
  }

  private static Path testClasses() {
    return Paths.get("").toAbsolutePath().resolve("target").resolve("test-classes");
  }

  private static Path repoRoot() {
    return Paths.get("").toAbsolutePath().getParent();
  }

  private record Capture(int code, String out) {
  }
}
