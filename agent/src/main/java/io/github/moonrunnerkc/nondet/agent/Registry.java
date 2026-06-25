package io.github.moonrunnerkc.nondet.agent;

import io.github.moonrunnerkc.nondet.catalog.CallSite;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the call site metadata for a run and flushes the trace and registry at shutdown.
 *
 * <p>The call site rewrite registers one {@link CallSite} per instrumented site during
 * class transformation, so the opaque ids in a trace can be rendered back to a class,
 * method, line, and api. At JVM shutdown the registered hook writes the recorded events
 * and the call site registry to the paths named by system properties.
 *
 * <p>Output paths come from {@value #TRACE_PROPERTY} and {@value #REGISTRY_PROPERTY}. If
 * the registry path is unset it defaults to the trace path with a {@code .registry}
 * suffix. If the trace path is unset the agent writes nothing and says so on stderr,
 * rather than failing the target.
 */
public final class Registry {

  /** System property naming the trace output file. */
  public static final String TRACE_PROPERTY = "nondet.trace.out";

  /** System property naming the call site registry output file. */
  public static final String REGISTRY_PROPERTY = "nondet.registry.out";

  private static final Map<String, CallSite> CALL_SITES = new ConcurrentHashMap<>();

  private Registry() {
  }

  /**
   * Registers the coordinates for an instrumented call site.
   *
   * <p>The first registration for an id wins; later identical registrations are ignored,
   * so re-transforming a class does not duplicate entries.
   *
   * @param callSite the call site to remember, never {@code null}
   */
  public static void register(CallSite callSite) {
    CALL_SITES.putIfAbsent(callSite.callSiteId(), callSite);
  }

  /**
   * Returns the registered call sites.
   *
   * @return the call sites recorded so far, in no particular order
   */
  public static Collection<CallSite> callSites() {
    return CALL_SITES.values();
  }

  /**
   * Installs the JVM shutdown hook that flushes the trace and registry.
   */
  public static void installShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread(Registry::flush, "nondet-flush"));
  }

  /**
   * Writes the recorded events and call site registry to the configured paths.
   *
   * <p>Visible for testing so the flush can be driven without a real shutdown.
   */
  static void flush() {
    final String tracePath = System.getProperty(TRACE_PROPERTY);
    if (tracePath == null || tracePath.isBlank()) {
      System.err.println("nondet agent: " + TRACE_PROPERTY + " is not set, not writing a trace");
      return;
    }
    final String registryPath = System.getProperty(REGISTRY_PROPERTY, tracePath + ".registry");
    try {
      TraceWriter.writeTrace(Path.of(tracePath), Recorder.snapshot(), Recorder.truncated());
      TraceWriter.writeRegistry(Path.of(registryPath), CALL_SITES.values());
    } catch (final IOException cause) {
      System.err.println("nondet agent: failed to write trace to " + tracePath
          + "; check the path is writable: " + cause.getMessage());
    }
  }

  /**
   * Clears all registered call sites. Visible for testing.
   */
  static void reset() {
    CALL_SITES.clear();
  }
}
