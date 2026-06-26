package io.github.moonrunnerkc.nondet.cli;

import io.github.moonrunnerkc.nondet.catalog.CallSite;
import io.github.moonrunnerkc.nondet.catalog.Event;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

/**
 * Renders a {@link Divergence} into a human readable report, resolving call site ids to
 * source coordinates through the registry.
 *
 * <p>The two runs are labelled A and B, matching the first and second arguments handed to
 * {@link Diff#first}. When an id is missing from the registry the report falls back to
 * the raw id and category so it still says something useful.
 *
 * <p>A multi-run check extends the base report with two optional sections: the distinct
 * call sites that differ in some run pairing beyond the primary one, and a note for each
 * run that hit the event cap and so contributed only a prefix of its reads. Both sections
 * are empty for the common two-run case, so its output is unchanged.
 */
public final class DivergenceReport {

  private final Divergence divergence;
  private final Map<String, CallSite> callSites;
  private final List<String> additionalSiteIds;
  private final List<Integer> truncatedRuns;
  private final int maxThreads;

  private DivergenceReport(Divergence divergence, Map<String, CallSite> callSites,
      List<String> additionalSiteIds, List<Integer> truncatedRuns, int maxThreads) {
    this.divergence = divergence;
    this.callSites = callSites;
    this.additionalSiteIds = additionalSiteIds;
    this.truncatedRuns = truncatedRuns;
    this.maxThreads = maxThreads;
  }

  /**
   * Renders the message for a check where no run read any entropy source.
   *
   * <p>This is deliberately distinct from a {@link Divergence.Kind#NONE} report. A NONE
   * report means reads happened and every run agreed; this one means nothing in the catalog
   * was read at all, so there was nothing to compare. Either way the check exits zero, but
   * the wording keeps the user from reading silence as a determinism proof.
   *
   * @param runs the number of runs that recorded nothing, at least one
   * @return the report text with a trailing newline
   */
  public static String noReadsObserved(int runs) {
    return "nondet check: no entropy reads were observed across " + runs + " run"
        + (runs == 1 ? "" : "s") + "\n\n"
        + "this is not a proof of determinism; it means nothing in the catalog was read, so\n"
        + "there was nothing to diff. A source reached through reflection, or one outside the\n"
        + "six catalog sources, would not show up here.\n";
  }

  /**
   * Builds a report for a divergence against a registry.
   *
   * @param divergence the divergence to describe, never {@code null}
   * @param callSites  the call site registry, keyed by id, never {@code null}
   * @return a report ready to render
   */
  public static DivergenceReport of(Divergence divergence, Map<String, CallSite> callSites) {
    return new DivergenceReport(divergence, callSites, List.of(), List.of(), 1);
  }

  /**
   * Builds a multi-run report that adds the other diverging sites and truncation notes.
   *
   * @param divergence        the primary divergence, the earliest across run pairings, never {@code null}
   * @param callSites         the call site registry, keyed by id, never {@code null}
   * @param additionalSiteIds ids of call sites that differ in some pairing but are not the
   *     primary site, rendered as a secondary list; empty for a single divergence
   * @param truncatedRuns     the 1-based run numbers whose traces hit the event cap, empty when none
   * @return a report ready to render
   */
  public static DivergenceReport ofRuns(Divergence divergence, Map<String, CallSite> callSites,
      List<String> additionalSiteIds, List<Integer> truncatedRuns) {
    return ofRuns(divergence, callSites, additionalSiteIds, truncatedRuns, 1);
  }

  /**
   * Builds a multi-run report that also notes when more than one thread produced events.
   *
   * @param divergence        the primary divergence, the earliest across run pairings, never {@code null}
   * @param callSites         the call site registry, keyed by id, never {@code null}
   * @param additionalSiteIds ids of call sites that differ in some pairing but are not the
   *     primary site, rendered as a secondary list; empty for a single divergence
   * @param truncatedRuns     the 1-based run numbers whose traces hit the event cap, empty when none
   * @param maxThreads        the most threads any run recorded from; a value above one adds a
   *     best-effort note that cross-thread ordering is approximate
   * @return a report ready to render
   */
  public static DivergenceReport ofRuns(Divergence divergence, Map<String, CallSite> callSites,
      List<String> additionalSiteIds, List<Integer> truncatedRuns, int maxThreads) {
    return new DivergenceReport(divergence, callSites, List.copyOf(additionalSiteIds),
        List.copyOf(truncatedRuns), maxThreads);
  }

  /**
   * Renders the divergence to text with a trailing newline.
   *
   * @return the report text, deterministic for a given divergence and registry
   */
  public String render() {
    return base() + additionalSites() + truncationNote() + threadNote();
  }

  /**
   * Writes the rendered report to a stream.
   *
   * @param out the stream to write to, never {@code null}
   */
  public void printTo(PrintStream out) {
    out.print(render());
  }

  private String base() {
    return switch (divergence.kind()) {
      case NONE -> "nondet check: no divergence; both runs read the same entropy in the same order\n";
      case MISMATCH -> renderMismatch();
      case LENGTH -> renderLength();
    };
  }

  private String renderMismatch() {
    final Event left = divergence.left();
    final Event right = divergence.right();
    final boolean sameSite = left.callSiteId().equals(right.callSiteId());
    final String header = sameSite
        ? "nondet check: first divergence at read #" + divergence.index() + "\n"
        : "nondet check: divergence detected at read #" + divergence.index()
            + ", root likely outside the catalog or in scheduling\n";
    final String confidence = sameSite
        ? "\nhigh confidence: this call site read different values across the two runs\n"
        : "\nlower confidence: the runs disagree on which entropy source was read here, so they"
            + " forked earlier on something this catalog does not record\n";
    return header + "\n"
        + "  run A: " + location(left) + "  read " + left.value() + "\n"
        + "  run B: " + location(right) + "  read " + right.value() + "\n"
        + confidence;
  }

  private String renderLength() {
    final boolean firstRanLonger = divergence.left() != null;
    final Event extra = firstRanLonger ? divergence.left() : divergence.right();
    final String longer = firstRanLonger ? "A" : "B";
    final String shorter = firstRanLonger ? "B" : "A";
    return "nondet check: runs diverge in length at read #" + divergence.index() + "\n\n"
        + "  run " + longer + " read " + extra.value() + " at " + location(extra)
        + "; run " + shorter + " had already ended\n";
  }

  private String additionalSites() {
    if (additionalSiteIds.isEmpty()) {
      return "";
    }
    final StringBuilder out = new StringBuilder(
        "\nother call sites that differ in at least one run pairing:\n");
    for (final String id : additionalSiteIds) {
      out.append("  ").append(locationById(id)).append('\n');
    }
    return out.toString();
  }

  private String truncationNote() {
    if (truncatedRuns.isEmpty()) {
      return "";
    }
    final boolean single = truncatedRuns.size() == 1;
    final StringBuilder runs = new StringBuilder();
    for (int i = 0; i < truncatedRuns.size(); i++) {
      runs.append(i == 0 ? "" : ", ").append(truncatedRuns.get(i));
    }
    return "\nnote: " + (single ? "run " : "runs ") + runs + " hit the event cap; "
        + (single ? "its trace is" : "their traces are")
        + " a prefix, so the divergence search above was limited to the recorded reads\n";
  }

  private String threadNote() {
    if (maxThreads <= 1) {
      return "";
    }
    return "\nnote: " + maxThreads + " threads produced events; cross-thread read ordering is"
        + " approximate,\nso the divergence above may reflect interleaving rather than a"
        + " real difference\n";
  }

  private String location(Event event) {
    final CallSite site = callSites.get(event.callSiteId());
    if (site == null) {
      return "callSite " + event.callSiteId() + " (" + event.category() + ")";
    }
    return formatSite(site);
  }

  private String locationById(String callSiteId) {
    final CallSite site = callSites.get(callSiteId);
    return site == null ? "callSite " + callSiteId : formatSite(site);
  }

  private static String formatSite(CallSite site) {
    final String dotted = site.declaringClass().replace('/', '.');
    final String line = site.line() >= 0 ? "line " + site.line() : "line unknown";
    return dotted + '.' + site.method() + ' ' + line + " (" + site.api() + ")";
  }
}
