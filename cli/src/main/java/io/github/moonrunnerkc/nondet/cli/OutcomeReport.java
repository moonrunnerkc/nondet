package io.github.moonrunnerkc.nondet.cli;

import io.github.moonrunnerkc.nondet.catalog.CallSite;
import io.github.moonrunnerkc.nondet.catalog.Event;
import io.github.moonrunnerkc.nondet.catalog.Outcome;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reports a check in terms of outcomes, not raw reads.
 *
 * <p>A read only matters when it changes what the program produced. So this report first groups the
 * runs by their outcome fingerprint. When every run produced the same outcome there is no causal
 * nondeterminism, even if the clock or random values behind the runs differed; the report says so
 * plainly instead of flagging a read whose value varied but did not matter. When the runs produced
 * different outcomes the report names them and points at the entropy reads that differ between a run
 * of one outcome and a run of another, the candidates a causal search then narrows.
 *
 * <p>The grouping, the labels, and the chosen pair to diff are all assigned in run order, so the
 * report is byte identical across two checks over the same traces.
 */
public final class OutcomeReport {

  private static final int FINGERPRINT_PREFIX = 12;

  /**
   * One run's outcome paired with the entropy it read.
   *
   * @param index    the 1-based run number
   * @param exitCode the run's exit code, kept readable for the report since the outcome only holds
   *     its fingerprint
   * @param outcome  the run's outcome fingerprint, never {@code null}
   * @param events   the entropy reads the run recorded in order, never {@code null}
   */
  public record RunOutcome(int index, int exitCode, Outcome outcome, List<Event> events) {
  }

  private final List<RunOutcome> runs;
  private final Map<String, CallSite> registry;
  private final List<Integer> truncatedRuns;
  private final int maxThreads;

  private OutcomeReport(List<RunOutcome> runs, Map<String, CallSite> registry,
      List<Integer> truncatedRuns, int maxThreads) {
    this.runs = runs;
    this.registry = registry;
    this.truncatedRuns = truncatedRuns;
    this.maxThreads = maxThreads;
  }

  /**
   * Builds a report over the per-run outcomes and traces.
   *
   * @param runs          the runs in run order, at least one, none {@code null}
   * @param registry      the call site registry keyed by id, never {@code null}
   * @param truncatedRuns the 1-based run numbers whose traces hit the event cap, empty when none
   * @param maxThreads    the most threads any run recorded from; above one adds an approximate-order note
   * @return a report ready to render
   */
  public static OutcomeReport of(List<RunOutcome> runs, Map<String, CallSite> registry,
      List<Integer> truncatedRuns, int maxThreads) {
    return new OutcomeReport(List.copyOf(runs), registry, List.copyOf(truncatedRuns), maxThreads);
  }

  /**
   * Reports whether the runs produced more than one outcome.
   *
   * @return {@code true} when at least two runs disagree on outcome, the case the caller exits
   *     non-zero on
   */
  public boolean outcomesDiverged() {
    return groupByOutcome().size() > 1;
  }

  /**
   * Renders the report to text with a trailing newline.
   *
   * @return the report text, deterministic for a given set of runs and registry
   */
  public String render() {
    return (outcomesDiverged() ? renderDiverged() : renderStable())
        + truncationNote() + threadNote();
  }

  /**
   * Writes the rendered report to a stream.
   *
   * @param out the stream to write to, never {@code null}
   */
  public void printTo(PrintStream out) {
    out.print(render());
  }

  private String renderStable() {
    final int count = runs.size();
    final boolean anyReads = runs.stream().anyMatch(run -> !run.events().isEmpty());
    if (!anyReads) {
      return DivergenceReport.noReadsObserved(count) + exitNote();
    }
    final String header = "nondet check: no causal nondeterminism across " + count + " runs\n\n";
    final String body = readsVaried()
        ? "the runs read different entropy values along the way, but every run produced the same\n"
            + "outcome (" + shortFingerprint(runs.get(0).outcome()) + "), so none of those reads"
            + " controlled it.\n"
        : "the runs read the same entropy in the same order and produced the same outcome ("
            + shortFingerprint(runs.get(0).outcome()) + ").\n";
    return header + body + exitNote();
  }

  private String renderDiverged() {
    final Map<String, List<RunOutcome>> groups = groupByOutcome();
    final StringBuilder out = new StringBuilder(
        "nondet check: the runs produced " + groups.size() + " different outcomes\n\n");
    final List<List<RunOutcome>> ordered = new ArrayList<>(groups.values());
    for (int i = 0; i < ordered.size(); i++) {
      final List<RunOutcome> group = ordered.get(i);
      out.append("  outcome ").append((char) ('A' + i))
          .append("  ").append(shortFingerprint(group.get(0).outcome()))
          .append("  exit ").append(exitOf(group))
          .append("  ").append(runList(group)).append('\n');
    }
    out.append('\n').append(candidateReads(ordered.get(0).get(0), ordered.get(1).get(0)));
    return out.toString();
  }

  private String candidateReads(RunOutcome first, RunOutcome second) {
    final Divergence divergence = Diff.first(first.events(), second.events());
    return switch (divergence.kind()) {
      case NONE -> "no recorded entropy read differs between these outcomes, so the cause is outside\n"
          + "the six catalog sources or was reached through reflection.\n";
      case MISMATCH -> "the earliest entropy read that differs between outcome A (run " + first.index()
          + ") and outcome B (run " + second.index() + ") is the leading candidate cause:\n\n"
          + "  " + CallSiteFormat.location(divergence.left(), registry) + "\n"
          + "    run " + first.index() + " read " + divergence.left().value() + "\n"
          + "    run " + second.index() + " read " + divergence.right().value() + "\n\n"
          + "one or more of the reads that differ between these runs controls the outcome.\n";
      case LENGTH -> {
        final boolean firstLonger = divergence.left() != null;
        final Event extra = firstLonger ? divergence.left() : divergence.right();
        final int longer = firstLonger ? first.index() : second.index();
        final int shorter = firstLonger ? second.index() : first.index();
        yield "the outcomes diverge where run " + longer + " read on but run " + shorter
            + " had ended:\n\n"
            + "  " + CallSiteFormat.location(extra, registry) + "  read " + extra.value() + "\n";
      }
    };
  }

  private Map<String, List<RunOutcome>> groupByOutcome() {
    final Map<String, List<RunOutcome>> groups = new LinkedHashMap<>();
    for (final RunOutcome run : runs) {
      groups.computeIfAbsent(run.outcome().fingerprint(), key -> new ArrayList<>()).add(run);
    }
    return groups;
  }

  private boolean readsVaried() {
    final List<Event> baseline = runs.get(0).events();
    for (int i = 1; i < runs.size(); i++) {
      if (!Diff.first(baseline, runs.get(i).events()).isNone()) {
        return true;
      }
    }
    return false;
  }

  private String exitNote() {
    final int exit = exitOf(runs);
    return exit == 0 ? "" : "\nall runs exited with code " + exit + ".\n";
  }

  private static int exitOf(List<RunOutcome> group) {
    // every run in a group shares one outcome fingerprint, which folds in the exit code, so any
    // member's exit code stands for the group. The capturer keeps it, so read it back here.
    return group.get(0).exitCode();
  }

  private static String runList(List<RunOutcome> group) {
    final StringBuilder out = new StringBuilder(group.size() == 1 ? "run " : "runs ");
    for (int i = 0; i < group.size(); i++) {
      out.append(i == 0 ? "" : ", ").append(group.get(i).index());
    }
    return out.toString();
  }

  private static String shortFingerprint(Outcome outcome) {
    final String full = outcome.fingerprint();
    return "fingerprint " + full.substring(0, Math.min(FINGERPRINT_PREFIX, full.length()));
  }

  private String truncationNote() {
    if (truncatedRuns.isEmpty()) {
      return "";
    }
    final boolean single = truncatedRuns.size() == 1;
    final StringBuilder which = new StringBuilder();
    for (int i = 0; i < truncatedRuns.size(); i++) {
      which.append(i == 0 ? "" : ", ").append(truncatedRuns.get(i));
    }
    return "\nnote: " + (single ? "run " : "runs ") + which + " hit the event cap; "
        + (single ? "its trace is" : "their traces are")
        + " a prefix, so this comparison was limited to the recorded reads\n";
  }

  private String threadNote() {
    if (maxThreads <= 1) {
      return "";
    }
    return "\nnote: " + maxThreads + " threads produced events; cross-thread read ordering is"
        + " approximate,\nso a read-level difference may reflect interleaving rather than a real"
        + " divergence\n";
  }
}
