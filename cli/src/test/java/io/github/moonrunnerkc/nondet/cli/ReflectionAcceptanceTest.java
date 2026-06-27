package io.github.moonrunnerkc.nondet.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.moonrunnerkc.nondet.catalog.CallSite;
import io.github.moonrunnerkc.nondet.catalog.Category;
import io.github.moonrunnerkc.nondet.catalog.Event;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Real code reaches entropy through reflection, so the checker must see it there too.
 *
 * <p>A clock read dispatched through {@code Method.invoke} carries none of the bytecode operands the
 * direct rewrite matches on, so v0.1.0 could not see it. These prove the reflective read is recorded
 * and attributed to its caller, and that it replays from a bundle like any direct read. Both need
 * the packaged agent jar and the compiled probes, so each is skipped when the agent jar is missing.
 */
class ReflectionAcceptanceTest {

  @Test
  void aReflectiveNanoTimeReadIsRecordedAndAttributedToTheCaller(@TempDir Path workDir)
      throws Exception {
    assumeTrue(agentJar().isPresent(), "agent jar not built; run mvn -pl agent package first");
    final WorkloadRunner runner = runner(workDir);
    final RunResult recorded = runner.run(1);
    assertTrue(recorded.ok(), "the reflective workload must complete");

    final List<Event> events = TraceReader.readTrace(recorded.trace());
    assertTrue(events.stream().anyMatch(event -> event.category() == Category.TIME),
        "the reflective clock read is recorded");
    final Map<String, CallSite> registry = TraceReader.readRegistry(recorded.registry());
    assertTrue(registry.values().stream().anyMatch(site ->
            site.api().equals("System.nanoTime (reflective)")
                && site.declaringClass().equals("probe/ReflectiveNanoWorkload")),
        "the read is attributed to the reflective caller, marked reflective: " + registry.values());
  }

  @Test
  void aReflectiveReadIsServedFromTheBundleUnderReplay(@TempDir Path workDir) throws Exception {
    assumeTrue(agentJar().isPresent(), "agent jar not built; run mvn -pl agent package first");
    final WorkloadRunner runner = runner(workDir);
    final RunResult recorded = runner.run(1);
    final long recordedClock = firstTimeValue(recorded.trace());
    final Path bundle = workDir.resolve("repro.bundle");
    Bundles.writeFromTrace(recorded.trace(), bundle);

    final RunResult replay = runner.run(2, bundle);
    assertEquals(recordedClock, firstTimeValue(replay.trace()),
        "the reflective read replays its recorded value, which a live clock would not match");
  }

  private static long firstTimeValue(Path trace) throws Exception {
    for (final Event event : TraceReader.readTrace(trace)) {
      if (event.category() == Category.TIME) {
        return Long.parseLong(event.value());
      }
    }
    throw new AssertionError("no reflective clock read was recorded in " + trace);
  }

  private static WorkloadRunner runner(Path workDir) {
    return new WorkloadRunner(agentJar().orElseThrow(), workDir, testClasses().toString(),
        "probe.ReflectiveNanoWorkload", List.of(), 60, 0, false);
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
