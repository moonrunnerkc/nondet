package io.github.moonrunnerkc.nondet.cli;

import io.github.moonrunnerkc.nondet.catalog.CallSite;
import io.github.moonrunnerkc.nondet.catalog.Event;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * The {@code nondet check} subcommand: run a workload N times and diff the entropy reads.
 *
 * <p>Each run is a fresh child JVM with the agent attached and its own trace and registry
 * files. After the runs finish, every run after the first is diffed against the first and
 * the earliest first-divergence is reported with its source location, plus any other call
 * site that differs in some pairing. The exit code is zero when the runs agree, one when
 * they diverge, and two when the runs could not be completed.
 *
 * <p>The number of runs defaults to two and can be raised with {@code --runs} to widen
 * recall, since two runs can agree by chance on a value that only sometimes changes.
 *
 * <p>Child output is captured, not inherited, so the workload's stdout never lands in the
 * report. A failing or trace-less run is reported with its captured output so the failure
 * is diagnosable. Pass {@code --show-workload} to print the captured output on success.
 *
 * <p>The agent jar is taken from {@code --agent-jar} when given, otherwise the newest
 * runnable jar under {@code agent/target} is located automatically.
 *
 * <p>The {@code nondet.max.events} system property, the per run cap on recorded reads, is
 * forwarded to each child JVM when it is set on the command, so a tight loop over an
 * instrumented source cannot grow an unbounded trace. A run that hits the cap is reported
 * as truncated.
 */
@Command(
    name = "check",
    description = "Run a workload N times and report the first entropy read where the runs diverge.")
public final class CheckCommand implements Callable<Integer> {

  private static final int AGREE = 0;
  private static final int DIVERGED = 1;
  private static final int UNAVAILABLE = 2;
  private static final int MIN_RUNS = 2;
  private static final String MAX_EVENTS_PROPERTY = "nondet.max.events";
  private static final Path AGENT_TARGET = Path.of("agent", "target");

  @Option(
      names = {"-a", "--agent", "--agent-jar"},
      paramLabel = "AGENTJAR",
      description = "Path to nondet-agent.jar. If omitted, the newest jar under agent/target is used.")
  private Path agentJar;

  @Option(
      names = {"-cp", "--class-path"},
      paramLabel = "CLASSPATH",
      description = "Class path for the workload, for example examples/target/classes.")
  private String classpath;

  @Option(
      names = {"-r", "--runs"},
      paramLabel = "N",
      description = "How many times to run the workload, at least 2. Default 2.")
  private int runs = MIN_RUNS;

  @Option(
      names = "--show-workload",
      description = "Print each run's captured stdout and stderr before the report.")
  private boolean showWorkload;

  @Parameters(
      index = "0",
      paramLabel = "MAINCLASS",
      description = "Fully qualified main class to run.")
  private String mainClass;

  @Parameters(
      index = "1..*",
      paramLabel = "ARG",
      description = "Arguments passed through to the workload.")
  private List<String> workloadArgs = new ArrayList<>();

  /**
   * Runs the workload N times and reports the first divergence.
   *
   * @return {@link #AGREE} when the runs match, {@link #DIVERGED} when they differ, or
   *     {@link #UNAVAILABLE} when the agent jar is missing or a run fails
   * @throws Exception if a child process cannot be started or a trace cannot be read
   */
  @Override
  public Integer call() throws Exception {
    if (runs < MIN_RUNS) {
      System.err.println("nondet check: --runs must be at least " + MIN_RUNS
          + " so there is something to diff; got " + runs);
      return UNAVAILABLE;
    }
    final Path agent = resolveAgentJar();
    if (agent == null) {
      return UNAVAILABLE;
    }

    final Path workDir = Files.createTempDirectory("nondet-check-");
    final List<RunOutput> outputs = new ArrayList<>();
    for (int index = 1; index <= runs; index++) {
      final RunOutput output = runOnce(agent, workDir, index);
      if (output == null) {
        return UNAVAILABLE;
      }
      outputs.add(output);
    }

    if (showWorkload) {
      printWorkloadOutput(outputs);
    }
    return report(outputs);
  }

  private int report(List<RunOutput> outputs) throws IOException {
    final List<List<Event>> runEvents = new ArrayList<>();
    final List<Integer> truncatedRuns = new ArrayList<>();
    final Map<String, CallSite> registry = new LinkedHashMap<>();
    for (final RunOutput output : outputs) {
      final TraceReader.Trace trace = TraceReader.read(output.trace());
      runEvents.add(trace.events());
      if (trace.truncated()) {
        truncatedRuns.add(output.index());
      }
      registry.putAll(TraceReader.readRegistry(output.registry()));
    }

    final MultiRunDiff.Result result = MultiRunDiff.analyze(runEvents);
    DivergenceReport.ofRuns(result.primary(), registry, result.additionalSiteIds(), truncatedRuns)
        .printTo(System.out);
    return result.primary().isNone() ? AGREE : DIVERGED;
  }

  private Path resolveAgentJar() throws IOException {
    if (agentJar != null) {
      if (!Files.isRegularFile(agentJar)) {
        System.err.println("nondet check: agent jar not found at " + agentJar
            + "; build it with: mvn -pl agent package");
        return null;
      }
      return agentJar;
    }
    final Optional<Path> located = AgentJarLocator.newestJar(AGENT_TARGET);
    if (located.isEmpty()) {
      System.err.println("nondet check: no agent jar under " + AGENT_TARGET
          + "; build it with: mvn -pl agent package, or pass --agent-jar <path>");
      return null;
    }
    System.err.println("nondet check: using agent jar " + located.get());
    return located.get();
  }

  private RunOutput runOnce(Path agent, Path workDir, int index) throws IOException, InterruptedException {
    final Path trace = workDir.resolve("run" + index + ".trace");
    final Path registry = workDir.resolve("run" + index + ".registry");
    final Path captured = workDir.resolve("run" + index + ".out");

    final List<String> command = new ArrayList<>();
    command.add(javaExecutable());
    command.add("-javaagent:" + agent);
    command.add("-Dnondet.trace.out=" + trace);
    command.add("-Dnondet.registry.out=" + registry);
    final String maxEvents = System.getProperty(MAX_EVENTS_PROPERTY);
    if (maxEvents != null && !maxEvents.isBlank()) {
      command.add("-D" + MAX_EVENTS_PROPERTY + "=" + maxEvents);
    }
    if (classpath != null && !classpath.isBlank()) {
      command.add("-cp");
      command.add(classpath);
    }
    command.add(mainClass);
    command.addAll(workloadArgs);

    final int code = new ProcessBuilder(command)
        .redirectErrorStream(true)
        .redirectOutput(captured.toFile())
        .start()
        .waitFor();
    if (code != 0) {
      System.err.println("nondet check: workload run " + index + " exited with code " + code
          + "; fix the workload so it runs cleanly under the agent before checking it"
          + childOutputBlock(captured));
      return null;
    }
    if (!Files.isRegularFile(trace) || !Files.isRegularFile(registry)) {
      System.err.println("nondet check: run " + index + " produced no trace at " + trace
          + "; confirm the agent attached and that nondet.trace.out is writable"
          + childOutputBlock(captured));
      return null;
    }
    return new RunOutput(index, trace, registry, captured);
  }

  private void printWorkloadOutput(List<RunOutput> outputs) throws IOException {
    for (final RunOutput output : outputs) {
      System.out.println("=== workload output, run " + output.index() + " ===");
      System.out.print(readCaptured(output.captured()));
    }
  }

  private static String childOutputBlock(Path captured) {
    final String output = readCaptured(captured);
    if (output.isBlank()) {
      return ". The run produced no output.";
    }
    return ". Captured child output:\n" + output;
  }

  private static String readCaptured(Path captured) {
    try {
      return Files.exists(captured) ? Files.readString(captured, StandardCharsets.UTF_8) : "";
    } catch (final IOException cause) {
      return "(could not read captured output at " + captured + ": " + cause.getMessage() + ")";
    }
  }

  private static String javaExecutable() {
    return Path.of(System.getProperty("java.home"), "bin", "java").toString();
  }

  private record RunOutput(int index, Path trace, Path registry, Path captured) {
  }
}
