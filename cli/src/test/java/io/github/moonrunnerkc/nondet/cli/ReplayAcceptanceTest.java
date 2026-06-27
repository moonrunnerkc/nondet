package io.github.moonrunnerkc.nondet.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.moonrunnerkc.nondet.catalog.Category;
import io.github.moonrunnerkc.nondet.catalog.Event;
import io.github.moonrunnerkc.nondet.catalog.Outcome;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Replay is the primitive the repro recipe and the causal search stand on, so it must be exact.
 *
 * <p>A run recorded once and replayed many times must reproduce a byte-identical outcome every time,
 * and a read the bundle covers must be served from the bundle rather than the live JDK. Both need
 * the packaged agent jar and the compiled probes, so each is skipped when the agent jar is missing.
 */
class ReplayAcceptanceTest {

  @Test
  void aBundleReplaysToAByteStableOutcomeEveryTime(@TempDir Path workDir) throws Exception {
    assumeTrue(agentJar().isPresent(), "agent jar not built; run mvn -pl agent package first");
    final WorkloadRunner runner = runner(workDir);

    final RunResult recorded = runner.run(1);
    assertTrue(recorded.ok(), "the recording run must complete");
    final Outcome recordedOutcome = OutcomeCapture.capture(recorded);
    final Path bundle = workDir.resolve("repro.bundle");
    Bundles.writeFromTrace(recorded.trace(), bundle);

    for (int i = 2; i <= 6; i++) {
      final RunResult replay = runner.run(i, bundle);
      assertTrue(replay.ok(), "replay run " + i + " must complete");
      assertEquals(recordedOutcome.fingerprint(), OutcomeCapture.capture(replay).fingerprint(),
          "replay " + i + " must reproduce the recorded outcome exactly");
    }
  }

  @Test
  void aBundledReadIsServedFromTheTableNotTheLiveClock(@TempDir Path workDir) throws Exception {
    assumeTrue(agentJar().isPresent(), "agent jar not built; run mvn -pl agent package first");
    final WorkloadRunner runner = runner(workDir);

    final RunResult recorded = runner.run(1);
    final long recordedClock = firstTimeValue(recorded.trace());
    final Path bundle = workDir.resolve("repro.bundle");
    Bundles.writeFromTrace(recorded.trace(), bundle);

    final RunResult replay = runner.run(2, bundle);
    final long replayedClock = firstTimeValue(replay.trace());

    assertEquals(recordedClock, replayedClock,
        "the replay served the recorded clock; a live read would have advanced past it");
  }

  private static long firstTimeValue(Path trace) throws Exception {
    final List<Event> events = TraceReader.readTrace(trace);
    for (final Event event : events) {
      if (event.category() == Category.TIME) {
        return Long.parseLong(event.value());
      }
    }
    throw new AssertionError("the workload recorded no TIME read in " + trace);
  }

  private static WorkloadRunner runner(Path workDir) {
    return new WorkloadRunner(agentJar().orElseThrow(), workDir, testClasses().toString(),
        "probe.OutcomeDivergentWorkload", List.of(), 60, 0, false);
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
