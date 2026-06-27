package io.github.moonrunnerkc.nondet.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.moonrunnerkc.nondet.catalog.BundleIO;
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
 * End to end of {@code check --minimize}: a workload whose clock reaches its output diverges on
 * every pair of runs, and the single clock read controls the outcome, so the causal search
 * deterministically returns that read and writes a usable repro bundle.
 *
 * <p>Needs the packaged agent jar and the compiled probes, so it is skipped when the agent jar is
 * missing.
 */
class CheckMinimizeTest {

  @Test
  void minimizeNamesTheCausalReadAndWritesAReproBundle(@TempDir Path dir) throws Exception {
    assumeTrue(agentJar().isPresent(), "agent jar not built; run mvn -pl agent package first");
    final Path repro = dir.resolve("repro.bundle");

    final Capture run = check(
        "--runs", "2",
        "--minimize",
        "--repro-out", repro.toString(),
        "probe.OutcomeDivergentWorkload");

    assertEquals(1, run.code(), run.out() + run.err());
    assertTrue(run.out().contains("minimal causal set"), run.out());
    assertTrue(run.out().contains("System.nanoTime"), run.out());
    assertTrue(run.out().contains("reproduce with"), run.out());
    assertTrue(Files.isRegularFile(repro), "the repro bundle is written to --repro-out");
    assertTrue(BundleIO.read(repro).entries().size() >= 1, "the repro bundle has the causal read");
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
