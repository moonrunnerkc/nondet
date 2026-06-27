package io.github.moonrunnerkc.nondet.agent;

import io.github.moonrunnerkc.nondet.catalog.BundleIO;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Switches the runtime between recording live reads and serving recorded ones.
 *
 * <p>One system property decides the mode. With {@value #MODE_PROPERTY} set to
 * {@value #REPLAY_MODE} the agent loads the bundle named by {@value #BUNDLE_PROPERTY} at startup and
 * every rewritten call site serves its recorded value through {@link #serve(String)}. With the
 * property unset the runtime records live reads as before, and {@code serve} returns {@code null} so
 * each Hook falls through to the real JDK api.
 *
 * <p>Pure JDK so it resolves under any class loader. The loaded table is held in a volatile field so
 * the workload threads see it once the premain has installed it.
 */
final class Replay {

  /** System property selecting record or replay mode. */
  static final String MODE_PROPERTY = "nondet.mode";

  /** The value of {@value #MODE_PROPERTY} that turns on replay. */
  static final String REPLAY_MODE = "replay";

  /** System property naming the bundle file to replay. */
  static final String BUNDLE_PROPERTY = "nondet.replay.in";

  private static volatile ReplayTable table;

  private Replay() {
  }

  /**
   * Loads the replay table when the properties ask for replay mode.
   *
   * <p>A missing or unreadable bundle leaves the runtime in record mode and says so on stderr,
   * rather than failing the target program. Called once from the agent premain.
   */
  static void activateFromProperties() {
    if (!REPLAY_MODE.equals(System.getProperty(MODE_PROPERTY))) {
      return;
    }
    final String path = System.getProperty(BUNDLE_PROPERTY);
    if (path == null || path.isBlank()) {
      System.err.println("nondet agent: " + MODE_PROPERTY + "=" + REPLAY_MODE + " but "
          + BUNDLE_PROPERTY + " is not set; running live instead of replaying");
      return;
    }
    try {
      table = ReplayTable.fromBundle(BundleIO.read(Path.of(path)));
    } catch (final IOException cause) {
      System.err.println("nondet agent: could not read the replay bundle at " + path
          + "; running live instead: " + cause.getMessage());
    }
  }

  /**
   * Reports whether the runtime is serving recorded values.
   *
   * @return {@code true} when a replay table is installed
   */
  static boolean active() {
    return table != null;
  }

  /**
   * Returns the next recorded value for a call site, or {@code null} when none should be served.
   *
   * @param callSiteId the call site firing now, never {@code null}
   * @return the recorded value to serve, or {@code null} in record mode or when the site is
   *     exhausted
   */
  static String serve(String callSiteId) {
    final ReplayTable current = table;
    return current == null ? null : current.next(callSiteId);
  }

  /**
   * Installs a replay table directly. Visible for testing.
   *
   * @param replayTable the table to serve from, or {@code null} to return to record mode
   */
  static void install(ReplayTable replayTable) {
    table = replayTable;
  }

  /**
   * Clears any installed replay table, returning to record mode. Visible for testing.
   */
  static void reset() {
    table = null;
  }
}
