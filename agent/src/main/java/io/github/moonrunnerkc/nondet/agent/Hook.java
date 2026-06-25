package io.github.moonrunnerkc.nondet.agent;

import io.github.moonrunnerkc.nondet.catalog.Category;
import java.util.UUID;

/**
 * The runtime targets the rewritten call sites jump to, one per supported catalog api.
 *
 * <p>Each method calls the real JDK api, records the value through {@link Recorder},
 * and returns the value unchanged, so behavior is identical to the original call. The
 * call site id is the last parameter because the rewrite pushes it onto the stack after
 * the original arguments, just before the {@code invokestatic}.
 *
 * <p>There is exactly one Hook method per supported overload of a catalog source. The
 * agent reflects over these methods at startup and only rewrites a call site when a Hook
 * with the matching name and derived descriptor exists, so an overload with no Hook,
 * such as the no-arg {@code System.getenv()} that returns the whole environment, is left
 * untouched rather than rewritten to a method that does not exist.
 *
 * <p>Pure JDK on purpose: the instrumented program may run these under any class loader,
 * so they depend only on the JDK and the catalog types bundled beside them.
 */
public final class Hook {

  private Hook() {
  }

  /**
   * Records and returns {@code System.nanoTime}.
   *
   * @param callSiteId the originating call site id
   * @return the value of {@link System#nanoTime()}
   */
  public static long nanoTime(String callSiteId) {
    final long value = System.nanoTime();
    Recorder.record(Category.TIME, callSiteId, Long.toString(value));
    return value;
  }

  /**
   * Records and returns {@code System.currentTimeMillis}.
   *
   * @param callSiteId the originating call site id
   * @return the value of {@link System#currentTimeMillis()}
   */
  public static long currentTimeMillis(String callSiteId) {
    final long value = System.currentTimeMillis();
    Recorder.record(Category.TIME, callSiteId, Long.toString(value));
    return value;
  }

  /**
   * Records and returns {@code Math.random}.
   *
   * @param callSiteId the originating call site id
   * @return the value of {@link Math#random()}
   */
  public static double random(String callSiteId) {
    final double value = Math.random();
    Recorder.record(Category.RANDOM, callSiteId, Double.toString(value));
    return value;
  }

  /**
   * Records and returns {@code UUID.randomUUID}.
   *
   * @param callSiteId the originating call site id
   * @return a fresh {@link UUID#randomUUID()}
   */
  public static UUID randomUUID(String callSiteId) {
    final UUID value = UUID.randomUUID();
    Recorder.record(Category.RANDOM, callSiteId, value.toString());
    return value;
  }

  /**
   * Records and returns {@code System.getenv(name)}.
   *
   * @param name       the environment variable name
   * @param callSiteId the originating call site id
   * @return the value of {@link System#getenv(String)}, possibly {@code null}
   */
  public static String getenv(String name, String callSiteId) {
    final String value = System.getenv(name);
    Recorder.record(Category.ENV, callSiteId, String.valueOf(value));
    return value;
  }

  /**
   * Records and returns {@code System.getProperty(key)}.
   *
   * @param key        the system property name
   * @param callSiteId the originating call site id
   * @return the value of {@link System#getProperty(String)}, possibly {@code null}
   */
  public static String getProperty(String key, String callSiteId) {
    final String value = System.getProperty(key);
    Recorder.record(Category.SYSPROP, callSiteId, String.valueOf(value));
    return value;
  }

  /**
   * Records and returns {@code System.getProperty(key, fallback)}.
   *
   * @param key        the system property name
   * @param fallback   the value returned when the property is unset
   * @param callSiteId the originating call site id
   * @return the value of {@link System#getProperty(String, String)}
   */
  public static String getProperty(String key, String fallback, String callSiteId) {
    final String value = System.getProperty(key, fallback);
    Recorder.record(Category.SYSPROP, callSiteId, String.valueOf(value));
    return value;
  }
}
