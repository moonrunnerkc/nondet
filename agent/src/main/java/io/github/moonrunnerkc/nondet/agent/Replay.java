package io.github.moonrunnerkc.nondet.agent;

import io.github.moonrunnerkc.nondet.catalog.Bundle;
import io.github.moonrunnerkc.nondet.catalog.BundleIO;
import io.github.moonrunnerkc.nondet.catalog.RuntimeKeys;
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
 * <p>When {@value #PIN_PROPERTY} is set, replay serves through a {@link ThreadPin} that hands out the
 * recorded values in their recorded global order, pinning a threaded run's interleaving. Without it,
 * replay serves each site's values in order through a {@link ReplayTable} but lets the threads reach
 * the reads in any order, which is the right default for a single-threaded run or a minimization
 * trial whose control flow may differ from the recording.
 *
 * <p>Pure JDK so it resolves under any class loader. The loaded table is held in a volatile field so
 * the workload threads see it once the premain has installed it.
 */
final class Replay {

  /** System property selecting record or replay mode. */
  static final String MODE_PROPERTY = RuntimeKeys.MODE;

  /** The value of {@value #MODE_PROPERTY} that turns on replay. */
  static final String REPLAY_MODE = RuntimeKeys.MODE_REPLAY;

  /** System property naming the bundle file to replay. */
  static final String BUNDLE_PROPERTY = RuntimeKeys.REPLAY_IN;

  /** System property that pins threaded read order to the recorded global sequence under replay. */
  static final String PIN_PROPERTY = RuntimeKeys.REPLAY_PIN;

  private static volatile ReplayTable table;
  private static volatile ThreadPin pin;

  private Replay() {
  }

  /**
   * Loads the replay table or pin when the properties ask for replay mode.
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
      final Bundle bundle = BundleIO.read(Path.of(path));
      if (Boolean.getBoolean(PIN_PROPERTY)) {
        pin = ThreadPin.fromBundle(bundle);
      } else {
        table = ReplayTable.fromBundle(bundle);
      }
    } catch (final IOException cause) {
      System.err.println("nondet agent: could not read the replay bundle at " + path
          + "; running live instead: " + cause.getMessage());
    }
  }

  /**
   * Reports whether the runtime is serving recorded values.
   *
   * @return {@code true} when a replay table or pin is installed
   */
  static boolean active() {
    return table != null || pin != null;
  }

  /**
   * Returns the next recorded value for a call site, or {@code null} when none should be served.
   *
   * @param callSiteId the call site firing now, never {@code null}
   * @return the recorded value to serve, or {@code null} in record mode or when the site is
   *     exhausted
   */
  static String serve(String callSiteId) {
    final ThreadPin pinned = pin;
    if (pinned != null) {
      return pinned.serve(callSiteId);
    }
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
    pin = null;
  }

  /**
   * Installs a thread pin directly. Visible for testing.
   *
   * @param threadPin the pin to serve from, or {@code null} to clear it
   */
  static void installPin(ThreadPin threadPin) {
    pin = threadPin;
    table = null;
  }

  /**
   * Clears any installed replay table or pin, returning to record mode. Visible for testing.
   */
  static void reset() {
    table = null;
    pin = null;
  }
}
