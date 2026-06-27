package io.github.moonrunnerkc.nondet.evidence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs {@code nondet check --minimize} against every target in a corpus and records what it found.
 *
 * <p>The campaign harness: it reads a corpus of programs, runs the real checker against each in a
 * fresh child, and classifies the run from its exit code and the headers check printed. It invents
 * nothing. A target whose outcomes agree is recorded as stable; one whose cause check attributed is
 * recorded with that cause and the repro command verbatim; one that could not run is recorded as
 * such. The collected results feed {@link EvidenceReport}.
 *
 * <p>This is run by hand to produce the evidence report, not as part of the test suite, because it
 * launches many child JVMs and a real-project target needs its code fetched first. The classification
 * is split out so it can be tested without launching anything.
 */
public final class CorpusRunner {

  private static final int DIVERGED = 1;
  private static final String CAUSAL_HEADER = "minimal causal set";
  private static final String FORCED_LINE = "forced to their failing values";
  private static final String REPRODUCE_LINE = "reproduce with:";

  private final Path cliJar;
  private final Path agentJar;
  private final Path workDir;
  private final int runs;

  /**
   * Creates a runner bound to the built jars and a scratch directory.
   *
   * @param cliJar   the built {@code nondet-cli.jar}, never {@code null}
   * @param agentJar the built {@code nondet-agent.jar} to attach, or {@code null} to let check find
   *     it under {@code agent/target}
   * @param workDir  a directory for repro bundles and scratch, never {@code null}
   * @param runs     how many times to run each target, at least two
   */
  public CorpusRunner(Path cliJar, Path agentJar, Path workDir, int runs) {
    this.cliJar = cliJar;
    this.agentJar = agentJar;
    this.workDir = workDir;
    this.runs = runs;
  }

  /**
   * Runs every target and returns the classified results, in corpus order.
   *
   * @param targets the corpus, never {@code null}
   * @return one result per target
   * @throws IOException          if a target cannot be launched
   * @throws InterruptedException if waiting for a target is interrupted
   */
  public List<CheckResult> run(List<CorpusTarget> targets) throws IOException, InterruptedException {
    final List<CheckResult> results = new ArrayList<>(targets.size());
    for (final CorpusTarget target : targets) {
      final Path repro = workDir.resolve(target.name() + "-repro.bundle");
      final List<String> command = command(target, repro);
      final Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
      final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      final int exitCode = process.waitFor();
      results.add(classify(target, exitCode, output));
    }
    return results;
  }

  /**
   * Classifies one finished check run from its exit code and output.
   *
   * @param target   the target that was run, never {@code null}
   * @param exitCode the check exit code
   * @param output   the merged stdout and stderr check produced, never {@code null}
   * @return the classified result
   */
  public static CheckResult classify(CorpusTarget target, int exitCode, String output) {
    if (exitCode != 0 && exitCode != DIVERGED) {
      return new CheckResult(target, CheckResult.Verdict.COULD_NOT_RUN, List.of(), null);
    }
    if (exitCode == 0) {
      return new CheckResult(target, CheckResult.Verdict.OUTCOME_STABLE, List.of(), null);
    }
    if (!output.contains(CAUSAL_HEADER)) {
      return new CheckResult(target, CheckResult.Verdict.DIVERGED_UNATTRIBUTED, List.of(), null);
    }
    return new CheckResult(target, CheckResult.Verdict.CAUSE_ATTRIBUTED,
        causalReads(output), reproCommand(output));
  }

  private static List<String> causalReads(String output) {
    final List<String> reads = new ArrayList<>();
    boolean collecting = false;
    for (final String line : output.lines().toList()) {
      if (line.contains(CAUSAL_HEADER)) {
        collecting = true;
        continue;
      }
      if (collecting) {
        if (line.startsWith(FORCED_LINE)) {
          break;
        }
        if (line.startsWith("  ") && !line.isBlank()) {
          reads.add(line.trim());
        }
      }
    }
    return reads;
  }

  private static String reproCommand(String output) {
    boolean afterReproduce = false;
    for (final String line : output.lines().toList()) {
      if (afterReproduce && !line.isBlank()) {
        return line.trim();
      }
      if (line.startsWith(REPRODUCE_LINE)) {
        afterReproduce = true;
      }
    }
    return null;
  }

  private List<String> command(CorpusTarget target, Path repro) {
    final List<String> command = new ArrayList<>(List.of(
        javaExecutable(), "-jar", cliJar.toString(),
        "check", "--minimize", "--runs", Integer.toString(runs),
        "--repro-out", repro.toString()));
    if (agentJar != null) {
      command.add("--agent-jar");
      command.add(agentJar.toString());
    }
    command.add("--class-path");
    command.add(target.classpath());
    command.add(target.mainClass());
    command.addAll(target.args());
    return command;
  }

  private static String javaExecutable() {
    return Path.of(System.getProperty("java.home"), "bin", "java").toString();
  }

  /**
   * Runs the corpus and writes the report.
   *
   * <p>Arguments:
   * {@code --cli <jar> --corpus <tsv> --out <report.md> --work <dir> [--agent <jar>] [--runs N]}.
   *
   * @param args the command line arguments
   * @throws IOException          if the corpus or report cannot be read or written
   * @throws InterruptedException if a target run is interrupted
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    final Path cliJar = Path.of(option(args, "--cli"));
    final Path corpus = Path.of(option(args, "--corpus"));
    final Path out = Path.of(option(args, "--out"));
    final Path work = Files.createDirectories(Path.of(option(args, "--work")));
    final String agent = optionOr(args, "--agent", null);
    final Path agentJar = agent == null ? null : Path.of(agent);
    final int runs = Integer.parseInt(optionOr(args, "--runs", "2"));

    final List<CorpusTarget> targets = new ArrayList<>();
    for (final String line : Files.readAllLines(corpus, StandardCharsets.UTF_8)) {
      if (!line.isBlank() && !line.startsWith("#")) {
        targets.add(CorpusTarget.parse(line));
      }
    }
    final List<CheckResult> results = new CorpusRunner(cliJar, agentJar, work, runs).run(targets);
    Files.writeString(out, EvidenceReport.render(results, runs), StandardCharsets.UTF_8);
    System.out.println("nondet evidence: wrote " + results.size() + " measured rows to " + out);
  }

  private static String option(String[] args, String name) {
    final String value = optionOr(args, name, null);
    if (value == null) {
      throw new IllegalArgumentException("missing required option " + name
          + "; usage: --cli <jar> --corpus <tsv> --out <report.md> --work <dir> [--runs N]");
    }
    return value;
  }

  private static String optionOr(String[] args, String name, String fallback) {
    for (int i = 0; i + 1 < args.length; i++) {
      if (args[i].equals(name)) {
        return args[i + 1];
      }
    }
    return fallback;
  }
}
