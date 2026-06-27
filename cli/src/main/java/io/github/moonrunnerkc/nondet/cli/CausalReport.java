package io.github.moonrunnerkc.nondet.cli;

import io.github.moonrunnerkc.nondet.catalog.Bundle;
import io.github.moonrunnerkc.nondet.catalog.CallSite;
import java.util.List;
import java.util.Map;

/**
 * Renders a causal search: the minimal reads that control an outcome and the command that
 * reproduces it.
 *
 * <p>The point of v0.2.0 is to answer which reads actually cause a specific failure, so this report
 * names them with their class, method, line, and api, says they reproduce the failing outcome when
 * forced and that freeing any one of them does not, and hands back a self-verifying repro command.
 * When the search finds no controlling read among the candidates, it says so rather than guessing.
 */
final class CausalReport {

  private static final int FINGERPRINT_PREFIX = 12;

  private CausalReport() {
  }

  /**
   * Renders the result of a causal search.
   *
   * @param result    the search result, never {@code null}
   * @param registry  the call site registry keyed by id, never {@code null}
   * @param classpath the workload class path, or {@code null}/blank when none
   * @param mainClass the workload main class, never {@code null}
   * @param args      the workload arguments, never {@code null}
   * @return the report text with a trailing newline
   */
  static String render(Bisect.Result result, Map<String, CallSite> registry, String classpath,
      String mainClass, List<String> args) {
    if (result.causalKeys().isEmpty()) {
      return "\nnondet check: the outcomes differ, but no recorded entropy read among the candidates\n"
          + "controls them; the cause is outside the six catalog sources or was reached through"
          + " reflection.\n";
    }
    final int count = result.causalKeys().size();
    final StringBuilder out = new StringBuilder("\nnondet check: minimal causal set: " + count
        + (count == 1 ? " read controls the outcome\n\n" : " reads jointly control the outcome\n\n"));
    for (final Bundle.Key key : result.causalKeys()) {
      out.append("  ").append(locationOf(key, registry));
      if (key.perSiteSeq() > 0) {
        out.append("  [read #").append(key.perSiteSeq() + 1).append(" at this site]");
      }
      out.append('\n');
    }
    out.append("\nforced to their failing values these reproduce outcome ")
        .append(shortFingerprint(result.failingFingerprint()))
        .append(count == 1 ? ".\n" : "; freeing any one of them does not.\n");
    out.append("\nrepro bundle: ").append(result.reproBundle()).append('\n')
        .append("reproduce with:\n  ")
        .append(reproCommand(result, classpath, mainClass, args)).append('\n');
    return out.toString();
  }

  private static String reproCommand(Bisect.Result result, String classpath, String mainClass,
      List<String> args) {
    final StringBuilder command = new StringBuilder("nondet replay --bundle ")
        .append(result.reproBundle());
    if (classpath != null && !classpath.isBlank()) {
      command.append(" --class-path ").append(classpath);
    }
    command.append(" --expect ").append(result.failingFingerprint()).append(' ').append(mainClass);
    for (final String arg : args) {
      command.append(' ').append(arg);
    }
    return command.toString();
  }

  private static String locationOf(Bundle.Key key, Map<String, CallSite> registry) {
    final CallSite site = registry.get(key.callSiteId());
    return site == null ? "callSite " + key.callSiteId() : CallSiteFormat.location(site);
  }

  private static String shortFingerprint(String fingerprint) {
    return fingerprint.substring(0, Math.min(FINGERPRINT_PREFIX, fingerprint.length()));
  }
}
