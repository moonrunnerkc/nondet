package io.github.moonrunnerkc.nondet.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A threaded workload's outcome must come back identical under replay, even though its reads came
 * from several threads at once.
 *
 * <p>The recording is genuinely multi-threaded, then the bundle replays to the same outcome both in
 * the default value mode and with read order pinned to the recorded global sequence. Needs the
 * packaged agent jar and the compiled probes, so each is skipped when the agent jar is missing.
 */
class ThreadedReplayAcceptanceTest {

  @Test
  void aThreadedOutcomeReproducesUnderReplay(@TempDir Path workDir) throws Exception {
    assumeTrue(agentJar().isPresent(), "agent jar not built; run mvn -pl agent package first");
    final WorkloadRunner runner = runner(workDir);
    final RunResult recorded = runner.run(1);
    assertTrue(recorded.ok(), "the threaded workload must complete");
    assertTrue(TraceReader.read(recorded.trace()).threadCount() > 1,
        "the recording is genuinely multi-threaded");
    final String recordedOutcome = OutcomeCapture.capture(recorded).fingerprint();
    final Path bundle = workDir.resolve("repro.bundle");
    Bundles.writeFromTrace(recorded.trace(), bundle);

    final RunResult replay = runner.run(2, bundle);
    assertEquals(recordedOutcome, OutcomeCapture.capture(replay).fingerprint(),
        "the order-independent threaded outcome reproduces under value replay");
  }

  @Test
  void aThreadedOutcomeReproducesUnderPinnedReplay(@TempDir Path workDir) throws Exception {
    assumeTrue(agentJar().isPresent(), "agent jar not built; run mvn -pl agent package first");
    final WorkloadRunner runner = runner(workDir);
    final RunResult recorded = runner.run(1);
    final String recordedOutcome = OutcomeCapture.capture(recorded).fingerprint();
    final Path bundle = workDir.resolve("repro.bundle");
    Bundles.writeFromTrace(recorded.trace(), bundle);

    final RunResult pinned = runner.run(2, bundle, true);
    assertTrue(pinned.ok(), "a pinned threaded replay completes without deadlocking");
    assertEquals(recordedOutcome, OutcomeCapture.capture(pinned).fingerprint(),
        "the threaded outcome reproduces with read order pinned to the recorded sequence");
  }

  private static WorkloadRunner runner(Path workDir) {
    return new WorkloadRunner(agentJar().orElseThrow(), workDir, testClasses().toString(),
        "probe.ThreadedSumWorkload", List.of(), 60, 0, false);
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
}
