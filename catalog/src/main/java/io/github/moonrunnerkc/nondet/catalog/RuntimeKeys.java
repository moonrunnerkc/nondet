package io.github.moonrunnerkc.nondet.catalog;

/**
 * The system-property names the CLI and the agent use to talk to each other.
 *
 * <p>The CLI launches each workload in a child JVM and configures the agent through {@code -D}
 * system properties: where to write the trace, registry, and result files, how many reads to
 * record, whether to replay a bundle, and so on. The agent reads those same property names back at
 * run time. The two sides have to agree on every name exactly, or a run silently records nothing
 * or replays nothing.
 *
 * <p>Both sides reference the names here so there is one place to change and no chance of drift.
 * This type lives in the catalog module, which is bundled into the agent jar, so the names resolve
 * in the agent runtime as well as in the CLI. It holds constants only and is never instantiated.
 */
public final class RuntimeKeys {

  /** Names the file the agent writes the ordered trace of recorded reads to. */
  public static final String TRACE_OUT = "nondet.trace.out";

  /** Names the file the agent writes the call-site registry to. */
  public static final String REGISTRY_OUT = "nondet.registry.out";

  /** Names the file a workload may write its declared result to, for the outcome fingerprint. */
  public static final String RESULT_OUT = "nondet.result.out";

  /** Carries a declared result value inline when a workload publishes one without a file. */
  public static final String RESULT_VALUE = "nondet.result";

  /** Caps how many reads a single run records before it stops and marks the trace truncated. */
  public static final String MAX_EVENTS = "nondet.max.events";

  /** Turns on the agent's stderr diagnostics. */
  public static final String DEBUG = "nondet.debug";

  /** Selects record mode (unset) or replay mode (set to {@link #MODE_REPLAY}). */
  public static final String MODE = "nondet.mode";

  /** The value of {@link #MODE} that switches the agent into replay. */
  public static final String MODE_REPLAY = "replay";

  /** Names the bundle file the agent replays recorded reads from. */
  public static final String REPLAY_IN = "nondet.replay.in";

  /** When set, pins threaded replay to the recorded global read order. */
  public static final String REPLAY_PIN = "nondet.replay.pin";

  private RuntimeKeys() {
  }
}
