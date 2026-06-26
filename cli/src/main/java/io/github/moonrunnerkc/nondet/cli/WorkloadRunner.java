package io.github.moonrunnerkc.nondet.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Launches one workload run in a fresh child JVM with the agent attached and returns how it
 * ended.
 *
 * <p>The child writes its trace and registry to files this runner names, and its merged
 * stdout and stderr are captured to a file rather than inherited, so the workload's own
 * output never reaches the report. A run that does not finish within the timeout is killed
 * with {@code destroyForcibly} so a hanging workload can never hang the checker. Whatever the
 * child printed before it was killed is still in the captured file for diagnosis.
 *
 * <p>This class only runs the child and classifies the outcome into a {@link RunResult}; it
 * never decides exit codes or prints a report. The caller owns the messages so it can phrase
 * them against the options the user actually passed.
 */
final class WorkloadRunner {

  private static final String TRACE_PROPERTY = "nondet.trace.out";
  private static final String REGISTRY_PROPERTY = "nondet.registry.out";
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
   * @param workDir        the directory the trace, registry, and captured files go in
   * @param classpath      the workload class path, or {@code null}/blank to inherit none
   * @param mainClass      the fully qualified main class to run
   * @param workloadArgs   arguments passed through to the workload, never {@code null}
   * @param timeoutSeconds the per-run wall-clock limit in seconds, greater than zero
   * @param maxEvents      the per-run recorded-event cap to forward, or zero to leave the
   *     agent default in place
   * @param debug          whether to set {@code nondet.debug} on the child and echo the
   *     child command line to stderr
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
   * @return the outcome, success only when the child exited zero and wrote both files
   * @throws IOException          if the child process cannot be started
   * @throws InterruptedException if the wait for the child is interrupted
   */
  RunResult run(int index) throws IOException, InterruptedException {
    final Path trace = workDir.resolve("run" + index + ".trace");
    final Path registry = workDir.resolve("run" + index + ".registry");
    final Path captured = workDir.resolve("run" + index + ".out");
    final List<String> command = command(trace, registry);
    if (debug) {
      System.err.println("nondet check: run " + index + " command: " + String.join(" ", command));
    }

    final Process process = new ProcessBuilder(command)
        .redirectErrorStream(true)
        .redirectOutput(captured.toFile())
        .start();
    if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      process.waitFor();
      return new RunResult(RunResult.Status.TIMED_OUT, index, trace, registry, captured, 0);
    }

    final int code = process.exitValue();
    if (code != 0) {
      return new RunResult(RunResult.Status.FAILED_EXIT, index, trace, registry, captured, code);
    }
    if (!Files.isRegularFile(trace) || !Files.isRegularFile(registry)) {
      return new RunResult(RunResult.Status.NO_TRACE, index, trace, registry, captured, 0);
    }
    return RunResult.success(index, trace, registry, captured);
  }

  private List<String> command(Path trace, Path registry) {
    final List<String> command = new ArrayList<>();
    command.add(javaExecutable());
    command.add("-javaagent:" + agentJar);
    command.add("-D" + TRACE_PROPERTY + "=" + trace);
    command.add("-D" + REGISTRY_PROPERTY + "=" + registry);
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
