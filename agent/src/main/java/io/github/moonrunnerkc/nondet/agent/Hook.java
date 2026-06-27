package io.github.moonrunnerkc.nondet.agent;

import io.github.moonrunnerkc.nondet.catalog.Category;
import java.util.UUID;

/**
 * The runtime targets the rewritten call sites jump to, one per supported catalog api.
 *
 * <p>In record mode each method calls the real JDK api, records the value through
 * {@link Recorder}, and returns the value unchanged, so behavior is identical to the original call.
 * In replay mode the method instead serves the recorded value for this call site through
 * {@link Replay}, records that it served it, and returns it, so the rewritten site returns the
 * recorded value rather than the live one. When replay has no value for a site the method falls
 * through to the live api, so a read the bundle does not cover still works.
 *
 * <p>The call site id is the last parameter because the rewrite pushes it onto the stack after the
 * original arguments, just before the {@code invokestatic}. There is exactly one Hook method per
 * supported overload of a catalog source; the agent reflects over these methods at startup and only
 * rewrites a call site when a matching Hook exists.
 *
 * <p>Pure JDK on purpose: the instrumented program may run these under any class loader, so they
 * depend only on the JDK and the catalog types bundled beside them.
 */
public final class Hook {

  private static final String NULL_VALUE = "null";

  private Hook() {
  }

  /**
   * Records and returns {@code System.nanoTime}, or serves the recorded value under replay.
   *
   * @param callSiteId the originating call site id
   * @return the value of {@link System#nanoTime()}, or the recorded value when replaying
   */
  public static long nanoTime(String callSiteId) {
    final String served = Replay.serve(callSiteId);
    if (served != null) {
      Recorder.record(Category.TIME, callSiteId, served);
      return Long.parseLong(served);
    }
    final long value = System.nanoTime();
    Recorder.record(Category.TIME, callSiteId, Long.toString(value));
    return value;
  }

  /**
   * Records and returns {@code System.currentTimeMillis}, or serves the recorded value under replay.
   *
   * @param callSiteId the originating call site id
   * @return the value of {@link System#currentTimeMillis()}, or the recorded value when replaying
   */
  public static long currentTimeMillis(String callSiteId) {
    final String served = Replay.serve(callSiteId);
    if (served != null) {
      Recorder.record(Category.TIME, callSiteId, served);
      return Long.parseLong(served);
    }
    final long value = System.currentTimeMillis();
    Recorder.record(Category.TIME, callSiteId, Long.toString(value));
    return value;
  }

  /**
   * Records and returns {@code Math.random}, or serves the recorded value under replay.
   *
   * @param callSiteId the originating call site id
   * @return the value of {@link Math#random()}, or the recorded value when replaying
   */
  public static double random(String callSiteId) {
    final String served = Replay.serve(callSiteId);
    if (served != null) {
      Recorder.record(Category.RANDOM, callSiteId, served);
      return Double.parseDouble(served);
    }
    final double value = Math.random();
    Recorder.record(Category.RANDOM, callSiteId, Double.toString(value));
    return value;
  }

  /**
   * Records and returns {@code UUID.randomUUID}, or serves the recorded value under replay.
   *
   * @param callSiteId the originating call site id
   * @return a fresh {@link UUID#randomUUID()}, or the recorded UUID when replaying
   */
  public static UUID randomUUID(String callSiteId) {
    final String served = Replay.serve(callSiteId);
    if (served != null) {
      Recorder.record(Category.RANDOM, callSiteId, served);
      return UUID.fromString(served);
    }
    final UUID value = UUID.randomUUID();
    Recorder.record(Category.RANDOM, callSiteId, value.toString());
    return value;
  }

  /**
   * Records and returns {@code System.getenv(name)}, or serves the recorded value under replay.
   *
   * @param name       the environment variable name
   * @param callSiteId the originating call site id
   * @return the value of {@link System#getenv(String)}, possibly {@code null}, or the recorded value
   *     when replaying
   */
  public static String getenv(String name, String callSiteId) {
    final String served = Replay.serve(callSiteId);
    if (served != null) {
      Recorder.record(Category.ENV, callSiteId, served);
      return decodeNull(served);
    }
    final String value = System.getenv(name);
    Recorder.record(Category.ENV, callSiteId, String.valueOf(value));
    return value;
  }

  /**
   * Records and returns {@code System.getProperty(key)}, or serves the recorded value under replay.
   *
   * @param key        the system property name
   * @param callSiteId the originating call site id
   * @return the value of {@link System#getProperty(String)}, possibly {@code null}, or the recorded
   *     value when replaying
   */
  public static String getProperty(String key, String callSiteId) {
    final String served = Replay.serve(callSiteId);
    if (served != null) {
      Recorder.record(Category.SYSPROP, callSiteId, served);
      return decodeNull(served);
    }
    final String value = System.getProperty(key);
    Recorder.record(Category.SYSPROP, callSiteId, String.valueOf(value));
    return value;
  }

  /**
   * Records and returns {@code System.getProperty(key, fallback)}, or serves the recorded value
   * under replay.
   *
   * @param key        the system property name
   * @param fallback   the value returned when the property is unset
   * @param callSiteId the originating call site id
   * @return the value of {@link System#getProperty(String, String)}, or the recorded value when
   *     replaying
   */
  public static String getProperty(String key, String fallback, String callSiteId) {
    final String served = Replay.serve(callSiteId);
    if (served != null) {
      Recorder.record(Category.SYSPROP, callSiteId, served);
      return decodeNull(served);
    }
    final String value = System.getProperty(key, fallback);
    Recorder.record(Category.SYSPROP, callSiteId, String.valueOf(value));
    return value;
  }

  /**
   * Maps the recorded text back to {@code null} when it stands for an absent value.
   *
   * <p>A {@code null} environment variable or property is recorded as the text {@code "null"} by
   * {@link String#valueOf(Object)}, so replay maps that text back to a real {@code null}. A property
   * whose actual value is the four characters {@code null} cannot be told apart from an absent one,
   * which is the one ambiguity this encoding carries.
   */
  private static String decodeNull(String served) {
    return NULL_VALUE.equals(served) ? null : served;
  }
}
