package io.github.moonrunnerkc.nondet.cli;

import io.github.moonrunnerkc.nondet.catalog.Bundle;
import io.github.moonrunnerkc.nondet.catalog.CallSite;
import io.github.moonrunnerkc.nondet.catalog.Outcome;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * The {@code nondet check} subcommand: run a workload N times and compare what it produced.
 *
 * <p>Each run is a fresh child JVM with the agent attached. After the runs finish, the checker
 * fingerprints what each one produced, its exit code, output, and any declared result, and groups
 * the runs by that fingerprint. When every run produced the same outcome there is no causal
 * nondeterminism to report, even when the clocks behind the runs differed. When the runs produced
 * different outcomes the checker reports them and points at the entropy reads that differ between
 * them. A run that hangs past {@code --timeout} or writes no trace is a harness failure, reported
 * with its captured output and the execution-error code, never a hang or a stack trace.
 *
 * <p>The exit code is zero when the runs agree on outcome or read nothing, one when their outcomes
 * differ, two on a usage error, and three on an execution or IO error.
 */
@Command(
    name = "check",
    mixinStandardHelpOptions = true,
    description = "Run a workload N times and report the entropy reads that change its outcome.",
    footerHeading = "%nExample:%n",
    footer = {
      "  nondet check --class-path examples/target/classes \\",
      "    io.github.moonrunnerkc.nondet.examples.FlakyRetry"
    })
public final class CheckCommand implements Callable<Integer> {

  private static final int AGREE = 0;
  private static final int DIVERGED = 1;
  private static final int USAGE = 2;
  private static final int EXEC_ERROR = 3;
  private static final int MIN_RUNS = 2;

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
      names = {"-t", "--timeout"},
      paramLabel = "SECONDS",
      description = "Per-run wall-clock limit; a run that exceeds it is killed. Default 60.")
  private long timeoutSeconds = 60;

  @Option(
      names = "--max-events",
      paramLabel = "N",
      description = "Cap on recorded reads per run, forwarded to each child. Default 1000000.")
  private long maxEvents;

  @Option(
      names = "--keep-traces",
      paramLabel = "DIR",
      description = "Keep each run's trace and registry in DIR instead of deleting temp files.")
  private Path keepTraces;

  @Option(
      names = "--show-workload",
      description = "Print each run's captured stdout and stderr before the report.")
  private boolean showWorkload;

  @Option(
      names = "--minimize",
      description = "When the outcomes differ, find the minimal set of reads that cause the divergence "
          + "and write a repro bundle.")
  private boolean minimize;

  @Option(
      names = "--repro-out",
      paramLabel = "BUNDLE",
      description = "Where to write the repro bundle under --minimize. Default nondet-repro.bundle.")
  private Path reproOut = Path.of("nondet-repro.bundle");

  @Option(
      names = {"-v", "--debug"},
      description = "Print diagnostics to stderr and set nondet.debug on each child.")
  private boolean debug;

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
   * @return {@link #AGREE} (0) when the runs agree on outcome or nothing was read, {@link #DIVERGED}
   *     (1) when their outcomes differ, {@link #USAGE} (2) on a bad option value, or
   *     {@link #EXEC_ERROR} (3) when a run cannot be completed or a trace cannot be read
   */
  @Override
  public Integer call() {
    if (runs < MIN_RUNS) {
      System.err.println("nondet check: --runs must be at least " + MIN_RUNS
          + " so there is something to diff; got " + runs);
      return USAGE;
    }
    if (timeoutSeconds <= 0) {
      System.err.println("nondet check: --timeout must be a positive number of seconds; got "
          + timeoutSeconds);
      return USAGE;
    }
    final Path agent = resolveAgentJar();
    if (agent == null) {
      return EXEC_ERROR;
    }
    try {
      return runAndReport(agent);
    } catch (final IOException cause) {
      System.err.println("nondet check: could not read a run's output; "
          + "check the trace directory is readable: " + cause.getMessage());
      return EXEC_ERROR;
    } catch (final InterruptedException cause) {
      Thread.currentThread().interrupt();
      System.err.println("nondet check: interrupted while waiting for a run to finish");
      return EXEC_ERROR;
    }
  }

  private int runAndReport(Path agent) throws IOException, InterruptedException {
    final boolean keep = keepTraces != null;
    final Path workDir = keep ? Files.createDirectories(keepTraces)
        : Files.createTempDirectory("nondet-check-");
    try {
      final WorkloadRunner runner = new WorkloadRunner(agent, workDir, classpath, mainClass,
          workloadArgs, timeoutSeconds, maxEvents, debug);
      final List<RunResult> outputs = new ArrayList<>();
      for (int index = 1; index <= runs; index++) {
        final RunResult result = runner.run(index);
        if (!result.ok()) {
          reportFailure(result);
          return EXEC_ERROR;
        }
        outputs.add(result);
      }
      if (showWorkload) {
        printWorkloadOutput(outputs);
      }
      return report(outputs, runner, workDir);
    } finally {
      if (keep) {
        System.err.println("nondet check: kept traces in " + workDir);
      } else {
        WorkDirs.deleteRecursively(workDir);
      }
    }
  }

  private int report(List<RunResult> outputs, WorkloadRunner runner, Path workDir)
      throws IOException {
    final List<OutcomeReport.RunOutcome> runOutcomes = new ArrayList<>();
    final List<Integer> truncatedRuns = new ArrayList<>();
    final Map<String, CallSite> registry = new LinkedHashMap<>();
    int maxThreads = 0;
    for (final RunResult output : outputs) {
      final TraceReader.Trace trace = TraceReader.read(output.trace());
      final Map<String, CallSite> runRegistry = TraceReader.readRegistry(output.registry());
      final Outcome outcome = OutcomeCapture.capture(output);
      OutcomeCapture.write(output.outcome(), outcome);
      runOutcomes.add(new OutcomeReport.RunOutcome(
          output.index(), output.exitCode(), outcome, trace.events()));
      if (trace.truncated()) {
        truncatedRuns.add(output.index());
      }
      maxThreads = Math.max(maxThreads, trace.threadCount());
      registry.putAll(runRegistry);
      if (debug) {
        System.err.println("nondet check: run " + output.index() + " recorded "
            + trace.events().size() + " events from " + trace.threadCount() + " thread(s) across "
            + distinctClasses(runRegistry) + " instrumented class(es); outcome "
            + outcome.fingerprint());
      }
    }

    final OutcomeReport report = OutcomeReport.of(runOutcomes, registry, truncatedRuns, maxThreads);
    report.printTo(System.out);
    if (!report.outcomesDiverged()) {
      return AGREE;
    }
    if (minimize) {
      try {
        minimizeAndReport(runner, workDir, runOutcomes, registry);
      } catch (final RuntimeException cause) {
        System.err.println("nondet check: could not complete the causal search: "
            + cause.getMessage());
      }
    }
    return DIVERGED;
  }

  private void minimizeAndReport(WorkloadRunner runner, Path workDir,
      List<OutcomeReport.RunOutcome> runOutcomes, Map<String, CallSite> registry)
      throws IOException {
    final OutcomeReport.RunOutcome baseline = runOutcomes.get(0);
    final OutcomeReport.RunOutcome failing = firstDifferentOutcome(runOutcomes, baseline);
    final Bundle passingBundle = Bundle.fromEvents(baseline.events());
    final Bundle failingBundle = Bundle.fromEvents(failing.events());
    final Bisect bisect = new Bisect(runner, workDir, passingBundle, failingBundle);
    final Bisect.Result result = bisect.search(reproOut.toAbsolutePath());
    System.out.print(CausalReport.render(result, registry, classpath, mainClass, workloadArgs));
    if (!result.failingFingerprint().equals(failing.outcome().fingerprint())) {
      System.out.println("\nnote: replaying the recorded reads did not reproduce the observed"
          + " outcome exactly,\nso part of the cause is outside the recorded reads, likely a"
          + " reflective read or thread ordering");
    }
    if (debug) {
      System.err.println("nondet check: causal search ran " + result.replays() + " replays");
    }
  }

  private static OutcomeReport.RunOutcome firstDifferentOutcome(
      List<OutcomeReport.RunOutcome> runOutcomes, OutcomeReport.RunOutcome baseline) {
    for (final OutcomeReport.RunOutcome candidate : runOutcomes) {
      if (!candidate.outcome().agreesWith(baseline.outcome())) {
        return candidate;
      }
    }
    throw new IllegalStateException("outcomes diverged but no run differs from the baseline; "
        + "this is a bug in the divergence check");
  }

  private void reportFailure(RunResult result) {
    final String block = childOutputBlock(result);
    switch (result.status()) {
      case TIMED_OUT -> System.err.println("nondet check: run " + result.index()
          + " did not finish within " + timeoutSeconds + "s and was killed; raise --timeout or "
          + "make the workload terminate" + block);
      case NO_TRACE -> System.err.println("nondet check: run " + result.index()
          + " produced no trace at " + result.trace() + "; confirm the agent attached and that "
          + "nondet.trace.out is writable" + block);
      case SUCCESS -> {
        // a successful run is never reported as a failure
      }
    }
  }

  private Path resolveAgentJar() {
    return AgentJars.resolve(agentJar, debug, "check");
  }

  private void printWorkloadOutput(List<RunResult> outputs) {
    for (final RunResult output : outputs) {
      System.out.println("=== workload output, run " + output.index() + " ===");
      System.out.print(capturedStreams(output));
    }
  }

  private static int distinctClasses(Map<String, CallSite> registry) {
    final Set<String> classes = new HashSet<>();
    for (final CallSite site : registry.values()) {
      classes.add(site.declaringClass());
    }
    return classes.size();
  }

  private static String childOutputBlock(RunResult result) {
    final String output = capturedStreams(result);
    if (output.isBlank()) {
      return ". The run produced no output.";
    }
    return ". Captured child output:\n" + output;
  }

  private static String capturedStreams(RunResult result) {
    final String out = readCaptured(result.stdout());
    final String err = readCaptured(result.stderr());
    final StringBuilder combined = new StringBuilder(out);
    if (!err.isEmpty()) {
      if (combined.length() > 0 && combined.charAt(combined.length() - 1) != '\n') {
        combined.append('\n');
      }
      combined.append(err);
    }
    return combined.toString();
  }

  private static String readCaptured(Path captured) {
    try {
      return Files.exists(captured) ? Files.readString(captured, StandardCharsets.UTF_8) : "";
    } catch (final IOException cause) {
      return "(could not read captured output at " + captured + ": " + cause.getMessage() + ")";
    }
  }

}
