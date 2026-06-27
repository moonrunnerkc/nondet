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
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/**
 * The bad paths each end in a clear message and the right exit code, never a hang, a stack
 * trace, or a silent empty diff. Exit codes follow the contract: 2 usage, 3 execution error.
 *
 * <p>The runtime cases need the packaged agent jar and the compiled test probes, so they are
 * skipped when the agent jar is missing; the pure usage and missing-input cases run always.
 */
class CheckFailureModeTest {

  @Test
  void runsBelowTwoIsAUsageError() {
    final Capture run = check("--runs", "1", "probe.StablePropertyWorkload");
    assertEquals(2, run.code());
    assertTrue(run.err().contains("--runs must be at least 2"), run.err());
  }

  @Test
  void nonPositiveTimeoutIsAUsageError() {
    final Capture run = check("--timeout", "0", "probe.StablePropertyWorkload");
    assertEquals(2, run.code());
    assertTrue(run.err().contains("--timeout must be a positive number"), run.err());
  }

  @Test
  void aMissingAgentJarIsAnActionableExecutionError() {
    final Capture run = check(
        "--agent-jar", "/does/not/exist/nondet-agent.jar",
        "probe.StablePropertyWorkload");
    assertEquals(3, run.code());
    assertTrue(run.err().contains("agent jar not found"), run.err());
    assertTrue(run.err().contains("mvn -pl agent package"), run.err());
  }

  @Test
  void aWorkloadThatAlwaysFailsTheSameWayIsOutcomeStable() {
    assumeTrue(agentJar().isPresent(), "agent jar not built; run mvn -pl agent package first");
    final Capture run = check(
        "--agent-jar", agentJar().get().toString(),
        "--class-path", testClasses().toString(),
        "probe.FailingWorkload");

    assertEquals(0, run.code(), run.out());
    assertTrue(run.out().contains("no entropy reads were observed"), run.out());
    assertTrue(run.out().contains("exited with code 1"), run.out());
  }

  @Test
  void aHaltedChildLeavesNoTraceAndIsAnExecutionError() {
    assumeTrue(agentJar().isPresent(), "agent jar not built; run mvn -pl agent package first");
    final Capture run = check(
        "--agent-jar", agentJar().get().toString(),
        "--class-path", testClasses().toString(),
        "probe.HaltingWorkload");

    assertEquals(3, run.code());
    assertTrue(run.err().contains("produced no trace"), run.err());
  }

  @Test
  void aHangingWorkloadIsKilledAtTheTimeout() {
    assumeTrue(agentJar().isPresent(), "agent jar not built; run mvn -pl agent package first");
    final Capture run = check(
        "--agent-jar", agentJar().get().toString(),
        "--class-path", testClasses().toString(),
        "--timeout", "2",
        "probe.HangingWorkload");

    assertEquals(3, run.code());
    assertTrue(run.err().contains("did not finish within 2s"), run.err());
  }

  @Test
  void keepTracesRetainsEachRunsFiles(@TempDir Path keep) {
    assumeTrue(agentJar().isPresent(), "agent jar not built; run mvn -pl agent package first");
    final Capture run = check(
        "--agent-jar", agentJar().get().toString(),
        "--class-path", testClasses().toString(),
        "--keep-traces", keep.toString(),
        "probe.StablePropertyWorkload");

    assertEquals(0, run.code(), run.out() + run.err());
    assertTrue(Files.isRegularFile(keep.resolve("run1.trace")), "run 1 trace is kept");
    assertTrue(Files.isRegularFile(keep.resolve("run2.registry")), "run 2 registry is kept");
  }

  @Test
  void aMultiThreadedWorkloadNotesApproximateOrdering() {
    assumeTrue(agentJar().isPresent(), "agent jar not built; run mvn -pl agent package first");
    final Capture run = check(
        "--agent-jar", agentJar().get().toString(),
        "--class-path", testClasses().toString(),
        "probe.ThreadedWorkload");

    assertTrue(run.code() == 0 || run.code() == 1, "a threaded run still completes: " + run.err());
    assertTrue(run.out().contains("threads produced events"), run.out());
  }

  private static Capture check(String... args) {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final ByteArrayOutputStream err = new ByteArrayOutputStream();
    final PrintStream originalOut = System.out;
    final PrintStream originalErr = System.err;
    final int code;
    try {
      System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
      System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
      code = new CommandLine(new CheckCommand()).execute(args);
    } finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
    }
    return new Capture(code, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
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

  private record Capture(int code, String out, String err) {
  }
}
