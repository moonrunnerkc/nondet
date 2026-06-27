package io.github.moonrunnerkc.nondet.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Launches one workload run in a fresh child JVM with the agent attached and returns how it ended.
 *
 * <p>The child writes its trace and registry to files this runner names. Its standard output and
 * standard error are captured to separate files rather than inherited, so the workload's own output
 * never reaches the report and so each stream can be fingerprinted on its own. The runner also names
 * a result file and points the child at it through {@code nondet.result.out}, so a workload that
 * wants to publish an explicit result has somewhere to write it. A run that does not finish within
 * the timeout is killed with {@code destroyForcibly} so a hanging workload can never hang the
 * checker; whatever the child printed before it was killed is still captured for diagnosis.
 *
 * <p>This class only runs the child and classifies the outcome into a {@link RunResult}; it never
 * decides exit codes or prints a report. A non-zero child exit is not a failure here, only part of
 * the outcome, so the caller can compare it like any other result.
 */
final class WorkloadRunner {

  private static final String TRACE_PROPERTY = "nondet.trace.out";
  private static final String REGISTRY_PROPERTY = "nondet.registry.out";
  private static final String RESULT_PROPERTY = "nondet.result.out";
  private static final String MAX_EVENTS_PROPERTY = "nondet.max.events";
  private static final String DEBUG_PROPERTY = "nondet.debug";

  private final Path agentJar;
  private final Path workDir;
  private final String classpath;
  private final String mainClass;
  private final List<String> workloadArgs;
  private final long timeoutSeconds;
  private final long maxEvents;
  private final boolean debug;

  /**
   * Creates a runner bound to one workload and its launch options.
   *
   * @param agentJar       the agent jar to attach, never {@code null}
   * @param workDir        the directory the trace, registry, captured, and result files go in
   * @param classpath      the workload class path, or {@code null}/blank to inherit none
   * @param mainClass      the fully qualified main class to run
   * @param workloadArgs   arguments passed through to the workload, never {@code null}
   * @param timeoutSeconds the per-run wall-clock limit in seconds, greater than zero
   * @param maxEvents      the per-run recorded-event cap to forward, or zero to leave the agent
   *     default in place
   * @param debug          whether to set {@code nondet.debug} on the child and echo the child
   *     command line to stderr
   */
  WorkloadRunner(Path agentJar, Path workDir, String classpath, String mainClass,
      List<String> workloadArgs, long timeoutSeconds, long maxEvents, boolean debug) {
    this.agentJar = agentJar;
    this.workDir = workDir;
    this.classpath = classpath;
    this.mainClass = mainClass;
    this.workloadArgs = workloadArgs;
    this.timeoutSeconds = timeoutSeconds;
    this.maxEvents = maxEvents;
    this.debug = debug;
  }

  /**
   * Runs the workload once and classifies how it ended.
   *
   * @param index the 1-based run number, used to name this run's files
   * @return the outcome; {@link RunResult.Status#SUCCESS} whenever the child finished and wrote both
   *     its trace and registry, whatever its exit code
   * @throws IOException          if the child process cannot be started
   * @throws InterruptedException if the wait for the child is interrupted
   */
  RunResult run(int index) throws IOException, InterruptedException {
    return run(index, null);
  }

  /**
   * Runs the workload once in replay mode, serving recorded reads from a bundle.
   *
   * <p>Identical to {@link #run(int)} except the child is launched in replay mode against
   * {@code bundle}, so each rewritten call site returns its recorded value instead of the live JDK
   * one. The run is otherwise classified the same way.
   *
   * @param index  the 1-based run number, used to name this run's files
   * @param bundle the bundle to replay, or {@code null} to run live in record mode
   * @return the outcome of the run
   * @throws IOException          if the child process cannot be started
   * @throws InterruptedException if the wait for the child is interrupted
   */
  RunResult run(int index, Path bundle) throws IOException, InterruptedException {
    final Path trace = workDir.resolve("run" + index + ".trace");
    final Path registry = workDir.resolve("run" + index + ".registry");
    final Path stdout = workDir.resolve("run" + index + ".out");
    final Path stderr = workDir.resolve("run" + index + ".err");
    final Path result = workDir.resolve("run" + index + ".result");
    final Path outcome = workDir.resolve("run" + index + ".outcome");
    final List<String> command = command(trace, registry, result, bundle);
    if (debug) {
      System.err.println("nondet check: run " + index + " command: " + String.join(" ", command));
    }

    final Process process = new ProcessBuilder(command)
        .redirectOutput(stdout.toFile())
        .redirectError(stderr.toFile())
        .start();
    if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      process.waitFor();
      return new RunResult(RunResult.Status.TIMED_OUT, index, trace, registry, stdout, stderr,
          result, outcome, 0);
    }

    final int code = process.exitValue();
    if (!Files.isRegularFile(trace) || !Files.isRegularFile(registry)) {
      return new RunResult(RunResult.Status.NO_TRACE, index, trace, registry, stdout, stderr,
          result, outcome, code);
    }
    return RunResult.success(index, trace, registry, stdout, stderr, result, outcome, code);
  }

  private List<String> command(Path trace, Path registry, Path result, Path bundle) {
    final List<String> command = new ArrayList<>();
    command.add(javaExecutable());
    command.add("-javaagent:" + agentJar);
    command.add("-D" + TRACE_PROPERTY + "=" + trace);
    command.add("-D" + REGISTRY_PROPERTY + "=" + registry);
    command.add("-D" + RESULT_PROPERTY + "=" + result);
    if (bundle != null) {
      command.add("-Dnondet.mode=replay");
      command.add("-Dnondet.replay.in=" + bundle);
    }
    if (maxEvents > 0) {
      command.add("-D" + MAX_EVENTS_PROPERTY + "=" + maxEvents);
    }
    if (debug) {
      command.add("-D" + DEBUG_PROPERTY + "=true");
    }
    if (classpath != null && !classpath.isBlank()) {
      command.add("-cp");
      command.add(classpath);
    }
    command.add(mainClass);
    command.addAll(workloadArgs);
    return command;
  }

  private static String javaExecutable() {
    return Path.of(System.getProperty("java.home"), "bin", "java").toString();
  }
}
