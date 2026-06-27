package io.github.moonrunnerkc.nondet.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The regression gate: every catalog source that can drive an outcome must stay attributable.
 *
 * <p>Each micro fixture lets one catalog read reach its output, so two runs diverge and the read
 * controls the outcome. This drives the real {@code nondet check --minimize} pipeline against each
 * and fails loudly if the cause stops being attributed to the expected api, which is how a future
 * change that breaks attribution gets caught. It needs the built cli and agent jars, so it is
 * skipped when they are missing; a full {@code mvn -DskipTests package} then {@code mvn test} builds
 * them.
 */
class MicroBenchTest {

  @ParameterizedTest(name = "{1} is attributed for {0}")
  @CsvSource({
      "io.github.moonrunnerkc.nondet.evidence.micro.NanoMicro, System.nanoTime",
      "io.github.moonrunnerkc.nondet.evidence.micro.MillisMicro, System.currentTimeMillis",
      "io.github.moonrunnerkc.nondet.evidence.micro.RandomMicro, Math.random",
      "io.github.moonrunnerkc.nondet.evidence.micro.UuidMicro, UUID.randomUUID"
  })
  void aKnownCauseStaysAttributedToItsSource(String mainClass, String expectedApi, @TempDir Path work)
      throws IOException, InterruptedException {
    assumeTrue(cliJar().isPresent() && agentJar().isPresent() && Files.isDirectory(classes()),
        "cli and agent jars must be built; run mvn -DskipTests package first");

    final Result result = check(mainClass, work);

    assertEquals(1, result.exitCode(), "a clock or random read reaching the output must diverge: "
        + result.output());
    assertTrue(result.output().contains("minimal causal set"), result.output());
    assertTrue(result.output().contains(expectedApi),
        "the cause must be attributed to " + expectedApi + ": " + result.output());
  }

  private static Result check(String mainClass, Path work) throws IOException, InterruptedException {
    final List<String> command = new ArrayList<>(List.of(
        javaExecutable(), "-jar", cliJar().orElseThrow().toString(),
        "check", "--minimize",
        "--agent-jar", agentJar().orElseThrow().toString(),
        "--repro-out", work.resolve("repro.bundle").toString(),
        "--class-path", classes().toString(),
        mainClass));
    final Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    return new Result(process.waitFor(), output);
  }

  private static java.util.Optional<Path> cliJar() {
    final Path jar = repoRoot().resolve("cli").resolve("target").resolve("nondet-cli.jar");
    return Files.isRegularFile(jar) ? java.util.Optional.of(jar) : java.util.Optional.empty();
  }

  private static java.util.Optional<Path> agentJar() {
    final Path jar = repoRoot().resolve("agent").resolve("target").resolve("nondet-agent.jar");
    return Files.isRegularFile(jar) ? java.util.Optional.of(jar) : java.util.Optional.empty();
  }

  private static Path classes() {
    return Paths.get("").toAbsolutePath().resolve("target").resolve("classes");
  }

  private static Path repoRoot() {
    return Paths.get("").toAbsolutePath().getParent();
  }

  private static String javaExecutable() {
    return Paths.get(System.getProperty("java.home"), "bin", "java").toString();
  }

  private record Result(int exitCode, String output) {
  }
}
