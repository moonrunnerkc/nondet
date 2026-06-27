package io.github.moonrunnerkc.nondet.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/**
 * The pivot of v0.2.0: a read is only nondeterminism when it changes the outcome.
 *
 * <p>A workload that reads the clock but produces a constant outcome is reported as
 * outcome-stable; one whose clock reaches the output is reported as outcome-divergent; and a
 * workload whose console is constant but whose declared result varies is divergent through the
 * result channel alone. Each needs the packaged agent jar and the compiled probes, so each is
 * skipped when the agent jar is missing.
 */
class OutcomeCaptureAcceptanceTest {

  @Test
  void aClockReadThatNeverReachesTheOutputIsOutcomeStable() {
    assumeTrue(agentJar().isPresent(), "agent jar not built; run mvn -pl agent package first");
    final Capture run = check("--runs", "3", "probe.OutcomeStableWorkload");

    assertEquals(0, run.code(), run.out());
    assertTrue(run.out().contains("no causal nondeterminism"), run.out());
  }

  @Test
  void aClockReadThatReachesTheOutputIsOutcomeDivergent() {
    assumeTrue(agentJar().isPresent(), "agent jar not built; run mvn -pl agent package first");
    final Capture run = check("--runs", "2", "probe.OutcomeDivergentWorkload");

    assertEquals(1, run.code(), run.out());
    assertTrue(run.out().contains("produced 2 different outcomes"), run.out());
    assertTrue(run.out().contains("System.nanoTime"), run.out());
  }

  @Test
  void aVaryingDeclaredResultMakesAConstantConsoleOutcomeDivergent() {
    assumeTrue(agentJar().isPresent(), "agent jar not built; run mvn -pl agent package first");
    final Capture run = check("--runs", "2", "probe.PublishedResultWorkload");

    assertEquals(1, run.code(), "the declared result varies, so the outcome diverges: " + run.out());
    assertTrue(run.out().contains("different outcomes"), run.out());
  }

  private static Capture check(String... args) {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final ByteArrayOutputStream err = new ByteArrayOutputStream();
    final PrintStream originalOut = System.out;
    final PrintStream originalErr = System.err;
    final String[] full = new String[args.length + 4];
    full[0] = "--agent-jar";
    full[1] = agentJar().orElseThrow().toString();
    full[2] = "--class-path";
    full[3] = testClasses().toString();
    System.arraycopy(args, 0, full, 4, args.length);
    final int code;
    try {
      System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
      System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
      code = new CommandLine(new CheckCommand()).execute(full);
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
