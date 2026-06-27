package io.github.moonrunnerkc.nondet.evidence;

import java.util.List;

/**
 * Renders measured {@link CheckResult}s into a Markdown evidence report.
 *
 * <p>The report is a table of what the checker actually found per target, followed by the repro
 * command for each attributed cause and an honest note on what the run does and does not show. It
 * carries only measured data: a stable target reads as stable, an unattributed divergence says the
 * cause is outside the catalog, and a target that did not run says so. No counts are invented.
 */
public final class EvidenceReport {

  private EvidenceReport() {
  }

  /**
   * Renders the report for a set of measured results.
   *
   * @param results the measured results in corpus order, never {@code null}
   * @param runs    how many runs per target the campaign used, for the header
   * @return the Markdown report with a trailing newline
   */
  public static String render(List<CheckResult> results, int runs) {
    final StringBuilder out = new StringBuilder();
    out.append("# nondet evidence report\n\n");
    out.append("Every row below is a real `nondet check --minimize` run over ").append(runs)
        .append(" executions of the target. Nothing here is hand written: the verdict comes from the\n")
        .append("run's exit code and the headers check printed, and the repro commands are copied from\n")
        .append("its output. Clock and random values differ between machines and runs, so the exact\n")
        .append("fingerprints in the repro commands are specific to the run that produced this report.\n\n");

    out.append("| Target | Provenance | Verdict | Causal reads |\n");
    out.append("| --- | --- | --- | --- |\n");
    for (final CheckResult result : results) {
      out.append("| ").append(result.target().name())
          .append(" | ").append(result.target().provenance())
          .append(" | ").append(verdict(result.verdict()))
          .append(" | ").append(causalCell(result))
          .append(" |\n");
    }

    final List<CheckResult> attributed = results.stream()
        .filter(result -> result.verdict() == CheckResult.Verdict.CAUSE_ATTRIBUTED)
        .toList();
    if (!attributed.isEmpty()) {
      out.append("\n## Repro commands\n\n");
      out.append("Each command replays the recorded reads and exits zero only if it reproduces the\n")
          .append("failing outcome, so it verifies itself.\n\n");
      for (final CheckResult result : attributed) {
        out.append("### ").append(result.target().name()).append("\n\n```\n")
            .append(result.reproCommand() == null ? "(no command captured)" : result.reproCommand())
            .append("\n```\n\n");
      }
    }

    out.append(notes());
    return out.toString();
  }

  private static String causalCell(CheckResult result) {
    return switch (result.verdict()) {
      case CAUSE_ATTRIBUTED -> result.causalReads().isEmpty()
          ? "(attributed, see below)"
          : String.join("; ", result.causalReads());
      case OUTCOME_STABLE -> "none: the runs agreed on outcome";
      case DIVERGED_UNATTRIBUTED -> "none recorded: cause outside the six catalog sources";
      case COULD_NOT_RUN -> "not measured: the target did not run";
    };
  }

  private static String verdict(CheckResult.Verdict verdict) {
    return switch (verdict) {
      case CAUSE_ATTRIBUTED -> "cause attributed";
      case OUTCOME_STABLE -> "outcome stable";
      case DIVERGED_UNATTRIBUTED -> "diverged, unattributed";
      case COULD_NOT_RUN -> "could not run";
    };
  }

  private static String notes() {
    return """
        ## What this shows, and what it does not

        - A "cause attributed" row means check ran the target several times, the outcomes differed,
          and delta-debugging narrowed the differing reads to the minimal set that controls the
          outcome, then wrote a repro bundle that reproduces it.
        - An "outcome stable" row is not a failure of the tool: it is the no-false-positive case. The
          reads varied between runs but never changed what the program produced, so there is nothing
          to attribute.
        - A "diverged, unattributed" row is honest about a limit: the outcomes differed but no read in
          the six-source catalog controlled them, so the cause is elsewhere, for example a JDK clock
          reached below the catalog or thread scheduling.
        - The tool produces the repro bundle and the verified repro command. Opening a fix and getting
          it merged upstream is a real-world outcome a repro enables, not something this harness does
          on its own.
        """;
  }
}
