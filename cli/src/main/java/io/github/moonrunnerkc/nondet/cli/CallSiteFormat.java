package io.github.moonrunnerkc.nondet.cli;

import io.github.moonrunnerkc.nondet.catalog.CallSite;
import io.github.moonrunnerkc.nondet.catalog.Event;
import java.util.Map;

/**
 * Renders a call site or an event back to a readable source location through a registry.
 *
 * <p>When an id is missing from the registry the fallback names the raw id so a report still says
 * something useful rather than nothing. Keeping the rendering here lets the divergence and causal
 * reports agree on how a location reads.
 */
final class CallSiteFormat {

  private CallSiteFormat() {
  }

  /**
   * Renders the source location an event came from.
   *
   * @param event    the recorded event, never {@code null}
   * @param registry the call site registry keyed by id, never {@code null}
   * @return a location such as {@code com.example.App.main line 12 (System.nanoTime)}, or a
   *     raw-id fallback when the id is not in the registry
   */
  static String location(Event event, Map<String, CallSite> registry) {
    final CallSite site = registry.get(event.callSiteId());
    if (site == null) {
      return "callSite " + event.callSiteId() + " (" + event.category() + ")";
    }
    return location(site);
  }

  /**
   * Renders the source location of a call site.
   *
   * @param site the call site, never {@code null}
   * @return a location such as {@code com.example.App.main line 12 (System.nanoTime)}
   */
  static String location(CallSite site) {
    final String dotted = site.declaringClass().replace('/', '.');
    final String line = site.line() >= 0 ? "line " + site.line() : "line unknown";
    return dotted + '.' + site.method() + ' ' + line + " (" + site.api() + ")";
  }
}
