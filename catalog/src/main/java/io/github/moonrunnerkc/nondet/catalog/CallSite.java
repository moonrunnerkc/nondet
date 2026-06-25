package io.github.moonrunnerkc.nondet.catalog;

import java.util.Objects;

/**
 * The source coordinates behind a {@link CallSiteId}.
 *
 * <p>The agent registers one of these per instrumented call site so a trace's opaque
 * ids can be rendered back to a class, method, line, and api when a divergence is
 * reported. It is serialized to the registry side file that travels with a trace.
 *
 * @param callSiteId     the stable id, see {@link CallSiteId}, never {@code null}
 * @param declaringClass the declaring class in internal form, never {@code null}
 * @param method         the enclosing method name, never {@code null}
 * @param line           the source line of the call, or a negative value when unknown
 * @param api            the catalog api label, for example {@code System.nanoTime}, never {@code null}
 */
public record CallSite(
    String callSiteId,
    String declaringClass,
    String method,
    int line,
    String api) {

  /**
   * Validates that the reference fields are present.
   *
   * @throws NullPointerException if {@code callSiteId}, {@code declaringClass},
   *     {@code method}, or {@code api} is {@code null}
   */
  public CallSite {
    Objects.requireNonNull(callSiteId, "callSiteId is required for a call site");
    Objects.requireNonNull(declaringClass, "declaringClass is required for a call site");
    Objects.requireNonNull(method, "method is required for a call site");
    Objects.requireNonNull(api, "api label is required for a call site");
  }
}
