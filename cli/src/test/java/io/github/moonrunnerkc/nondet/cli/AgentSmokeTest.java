package io.github.moonrunnerkc.nondet.cli;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Proves the agent orchestration end to end, independent of the rewrite and the diff.
 *
 * <p>It launches one child JVM with the agent attached against a trivial workload and
 * checks that a trace and a registry sidecar are written and parse cleanly. Both may be
 * empty while the rewrite is pass-through; the point is that the plumbing runs.
 *
 * <p>The test needs the packaged agent jar, so it is skipped when {@code agent/target} has
 * no jar yet. Build it first with {@code mvn -pl agent package} (a full
 * {@code mvn -DskipTests package} does this).
 */
class AgentSmokeTest {

  @Test
  void agentAttachesAndWritesAParseableTraceAndRegistry(@TempDir Path workDir) throws Exception {
    final Optional<Path> agentJar = AgentJarLocator.newestJar(agentTargetDir());
    assumeTrue(agentJar.isPresent(), "agent jar not built; run mvn -pl agent package first");

    final Path trace = workDir.resolve("smoke.trace");
    final Path registry = workDir.resolve("smoke.registry");
    final int code = runWorkload(agentJar.get(), trace, registry);

    assertEquals(0, code, "the workload must run cleanly under the agent");
    assertTrue(Files.isRegularFile(trace), "the agent must write a trace file");
    assertTrue(Files.isRegularFile(registry), "the agent must write a registry sidecar");
    assertDoesNotThrow(() -> TraceReader.readTrace(trace), "the trace must parse");
    assertDoesNotThrow(() -> TraceReader.readRegistry(registry), "the registry must parse");
  }

  private static int runWorkload(Path agentJar, Path trace, Path registry)
      throws IOException, InterruptedException {
    final List<String> command = List.of(
        javaExecutable(),
        "-javaagent:" + agentJar,
        "-Dnondet.trace.out=" + trace,
        "-Dnondet.registry.out=" + registry,
        "-cp", testClasses().toString(),
        "probe.SmokeWorkload");
    return new ProcessBuilder(command).inheritIO().start().waitFor();
  }

  private static Path agentTargetDir() {
    return moduleRoot().getParent().resolve("agent").resolve("target");
  }

  private static Path testClasses() {
    return moduleRoot().resolve("target").resolve("test-classes");
  }

  private static Path moduleRoot() {
    return Paths.get("").toAbsolutePath();
  }

  private static String javaExecutable() {
    return Paths.get(System.getProperty("java.home"), "bin", "java").toString();
  }
}
