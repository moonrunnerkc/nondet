package io.github.moonrunnerkc.nondet.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.moonrunnerkc.nondet.catalog.Outcome;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/**
 * The {@code replay} subcommand reproduces a recorded outcome and can prove it did.
 *
 * <p>With {@code --expect} the command is self-verifying: it exits zero only when the replay
 * reproduces the named outcome, which is what makes an emitted repro command trustworthy. These need
 * the packaged agent jar and the compiled probes, so each is skipped when the agent jar is missing.
 */
class ReplayCommandTest {

  @Test
  void replayReproducesAndVerifiesTheRecordedOutcome(@TempDir Path workDir) throws Exception {
    assumeTrue(agentJar().isPresent(), "agent jar not built; run mvn -pl agent package first");
    final String expected = recordBundle(workDir);

    final Capture matched = replay("--bundle", workDir.resolve("repro.bundle").toString(),
        "--expect", expected, "probe.OutcomeDivergentWorkload");
    assertEquals(0, matched.code(), matched.out() + matched.err());
    assertTrue(matched.out().contains("reproduced the expected outcome"), matched.out());
  }

  @Test
  void replayFailsWhenItDoesNotReproduceTheExpectedOutcome(@TempDir Path workDir) throws Exception {
    assumeTrue(agentJar().isPresent(), "agent jar not built; run mvn -pl agent package first");
    recordBundle(workDir);

    final Capture mismatch = replay("--bundle", workDir.resolve("repro.bundle").toString(),
        "--expect", "0000000000000000000000000000000000000000000000000000000000000000",
        "probe.OutcomeDivergentWorkload");
    assertEquals(1, mismatch.code(), mismatch.out());
    assertTrue(mismatch.out().contains("did not reproduce"), mismatch.out());
  }

  @Test
  void aMissingBundleIsAUsageError(@TempDir Path workDir) {
    final Capture run = replay("--bundle", workDir.resolve("absent.bundle").toString(),
        "probe.OutcomeDivergentWorkload");
    assertEquals(2, run.code());
    assertTrue(run.err().contains("bundle not found"), run.err());
  }

  private static String recordBundle(Path workDir) throws Exception {
    final WorkloadRunner runner = new WorkloadRunner(agentJar().orElseThrow(), workDir,
        testClasses().toString(), "probe.OutcomeDivergentWorkload", List.of(), 60, 0, false);
    final RunResult recorded = runner.run(1);
    Bundles.writeFromTrace(recorded.trace(), workDir.resolve("repro.bundle"));
    final Outcome outcome = OutcomeCapture.capture(recorded);
    return outcome.fingerprint();
  }

  private static Capture replay(String... args) {
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
      code = new CommandLine(new ReplayCommand()).execute(full);
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
