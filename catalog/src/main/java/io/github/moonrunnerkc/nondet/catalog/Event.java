package io.github.moonrunnerkc.nondet.catalog;

import java.util.Objects;

/**
 * One recorded read from an entropy source during a traced run.
 *
 * <p>This is the shared currency between the agent that records traces and the cli
 * that diffs them. It lives in the catalog module because the cli diffs events but
 * does not depend on the agent.
 *
 * @param seq        the global sequence number, assigned in the order reads happen
 * @param category   the kind of entropy that produced the value
 * @param callSiteId the stable id of the originating call site, see {@link CallSiteId}
 * @param value      the read value rendered as text, never {@code null}
 */
public record Event(long seq, Category category, String callSiteId, String value) {

  /**
   * Validates that the reference fields are present.
   *
   * @throws NullPointerException if {@code category}, {@code callSiteId}, or
   *     {@code value} is {@code null}
   */
  public Event {
    Objects.requireNonNull(category, "category is required for an event");
    Objects.requireNonNull(callSiteId, "callSiteId is required for an event");
    Objects.requireNonNull(value, "value is required for an event; use an empty string for no value");
  }
}
