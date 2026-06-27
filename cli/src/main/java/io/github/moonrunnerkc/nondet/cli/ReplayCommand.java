package io.github.moonrunnerkc.nondet.cli;

import io.github.moonrunnerkc.nondet.catalog.Outcome;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * The {@code nondet replay} subcommand: run a workload once against a recorded bundle.
 *
 * <p>It launches one child JVM in replay mode, so every rewritten call site returns the value the
 * bundle recorded for it instead of the live JDK value. The run's outcome is fingerprinted and
 * printed. With {@code --expect} the command checks the run reproduced a known outcome and exits
 * non-zero when it did not, which is what makes a repro command self-verifying: the same bundle and
 * the same expected fingerprint reproduce the same outcome every time, or the command says they did
 * not.
 *
 * <p>The exit code is zero when the run completed (and matched {@code --expect} if given), one when
 * an expected outcome was not reproduced, two on a usage error, and three on an execution or IO
 * error.
 */
@Command(
    name = "replay",
    mixinStandardHelpOptions = true,
    description = "Replay a recorded bundle and report the outcome it reproduces.",
    footerHeading = "%nExample:%n",
    footer = {
      "  nondet replay --bundle repro.bundle --class-path build/classes \\",
      "    --expect 5f2a1c8b... com.example.Main"
    })
public final class ReplayCommand implements Callable<Integer> {

  private static final int OK = 0;
  private static final int NOT_REPRODUCED = 1;
  private static final int USAGE = 2;
  private static final int EXEC_ERROR = 3;

  @Option(
      names = {"-b", "--bundle"},
      required = true,
      paramLabel = "BUNDLE",
      description = "The recorded bundle to replay.")
  private Path bundle;

  @Option(
      names = {"-a", "--agent", "--agent-jar"},
      paramLabel = "AGENTJAR",
      description = "Path to nondet-agent.jar. If omitted, the newest jar under agent/target is used.")
  private Path agentJar;

  @Option(
      names = {"-cp", "--class-path"},
      paramLabel = "CLASSPATH",
      description = "Class path for the workload, for example build/classes.")
  private String classpath;

  @Option(
      names = {"-t", "--timeout"},
      paramLabel = "SECONDS",
      description = "Wall-clock limit; a run that exceeds it is killed. Default 60.")
  private long timeoutSeconds = 60;

  @Option(
      names = "--expect",
      paramLabel = "FINGERPRINT",
      description = "Fail unless the replay reproduces this outcome fingerprint.")
  private String expect;

  @Option(
      names = "--keep-traces",
      paramLabel = "DIR",
      description = "Keep the replay's trace, registry, and outcome in DIR instead of deleting them.")
  private Path keepTraces;

  @Option(
      names = {"-v", "--debug"},
      description = "Print diagnostics to stderr and set nondet.debug on the child.")
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
   * Replays the bundle once and reports the outcome.
   *
   * @return {@link #OK} (0) when the run completed and matched any expectation, {@link #NOT_REPRODUCED}
   *     (1) when {@code --expect} was given but not reproduced, {@link #USAGE} (2) on a bad option, or
   *     {@link #EXEC_ERROR} (3) when the run could not be completed
   */
  @Override
  public Integer call() {
    if (!Files.isRegularFile(bundle)) {
      System.err.println("nondet replay: bundle not found at " + bundle
          + "; pass the path written by a check, or record one first");
      return USAGE;
    }
    if (timeoutSeconds <= 0) {
      System.err.println("nondet replay: --timeout must be a positive number of seconds; got "
          + timeoutSeconds);
      return USAGE;
    }
    final Path agent = AgentJars.resolve(agentJar, debug, "replay");
    if (agent == null) {
      return EXEC_ERROR;
    }
    try {
      return runAndReport(agent);
    } catch (final IOException cause) {
      System.err.println("nondet replay: could not read the run's output: " + cause.getMessage());
      return EXEC_ERROR;
    } catch (final InterruptedException cause) {
      Thread.currentThread().interrupt();
      System.err.println("nondet replay: interrupted while waiting for the run to finish");
      return EXEC_ERROR;
    }
  }

  private int runAndReport(Path agent) throws IOException, InterruptedException {
    final boolean keep = keepTraces != null;
    final Path workDir = keep ? Files.createDirectories(keepTraces)
        : Files.createTempDirectory("nondet-replay-");
    try {
      final WorkloadRunner runner = new WorkloadRunner(agent, workDir, classpath, mainClass,
          workloadArgs, timeoutSeconds, 0, debug);
      final RunResult result = runner.run(1, bundle);
      if (!result.ok()) {
        reportFailure(result);
        return EXEC_ERROR;
      }
      final Outcome outcome = OutcomeCapture.capture(result);
      OutcomeCapture.write(result.outcome(), outcome);
      System.out.println("nondet replay: outcome " + outcome.fingerprint());
      if (expect == null) {
        return OK;
      }
      if (expect.equals(outcome.fingerprint())) {
        System.out.println("nondet replay: reproduced the expected outcome");
        return OK;
      }
      System.out.println("nondet replay: did not reproduce the expected outcome " + expect);
      return NOT_REPRODUCED;
    } finally {
      if (keep) {
        System.err.println("nondet replay: kept the replay run in " + workDir);
      } else {
        WorkDirs.deleteRecursively(workDir);
      }
    }
  }

  private void reportFailure(RunResult result) {
    switch (result.status()) {
      case TIMED_OUT -> System.err.println("nondet replay: the run did not finish within "
          + timeoutSeconds + "s and was killed; raise --timeout or make the workload terminate");
      case NO_TRACE -> System.err.println("nondet replay: the run produced no trace at "
          + result.trace() + "; confirm the agent attached and that nondet.trace.out is writable");
      case SUCCESS -> {
        // a successful run is never reported as a failure
      }
    }
  }
}
