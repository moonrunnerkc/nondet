package probe;

import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Real workload methods outside the tool's packages, one per case the rewrite must handle.
 *
 * <p>Each method returns the entropy value it reads so a test can assert the recorded value
 * is exactly what the instrumented call returned. The class is compiled normally, so it
 * carries a line number table; tests that need a missing table strip it from these bytes.
 */
public final class EntropyProbes {

  private EntropyProbes() {
  }

  /**
   * Reads the nanosecond clock once.
   *
   * @return the value of {@link System#nanoTime()}
   */
  public static long nanoTime() {
    return System.nanoTime();
  }

  /**
   * Reads the wall clock once.
   *
   * @return the value of {@link System#currentTimeMillis()}
   */
  public static long currentTimeMillis() {
    return System.currentTimeMillis();
  }

  /**
   * Draws one pseudo random double.
   *
   * @return the value of {@link Math#random()}
   */
  public static double random() {
    return Math.random();
  }

  /**
   * Generates one random UUID.
   *
   * @return a fresh {@link UUID#randomUUID()}
   */
  public static UUID uuid() {
    return UUID.randomUUID();
  }

  /**
   * Reads one environment variable by name.
   *
   * @param name the variable to read
   * @return the value of {@link System#getenv(String)}, possibly {@code null}
   */
  public static String getenvVar(String name) {
    return System.getenv(name);
  }

  /**
   * Reads the whole environment, the overload the agent does not instrument.
   *
   * @return the number of environment entries
   */
  public static int fullEnv() {
    return System.getenv().size();
  }

  /**
   * Reads one system property by key.
   *
   * @param key the property to read
   * @return the value of {@link System#getProperty(String)}, possibly {@code null}
   */
  public static String property(String key) {
    return System.getProperty(key);
  }

  /**
   * Reads one system property with a fallback.
   *
   * @param key      the property to read
   * @param fallback the value returned when the property is unset
   * @return the value of {@link System#getProperty(String, String)}
   */
  public static String propertyOrDefault(String key, String fallback) {
    return System.getProperty(key, fallback);
  }

  /**
   * Reads the clock from inside a lambda body, which compiles to a synthetic method.
   *
   * @return the value the lambda read from {@link System#nanoTime()}
   */
  public static long insideLambda() {
    final LongSupplier clock = () -> System.nanoTime();
    return clock.getAsLong();
  }

  /**
   * Reads the clock twice on one source line, so both reads share a call site.
   *
   * @return the sum of the two {@link System#nanoTime()} reads
   */
  public static long twoOnOneLine() {
    return System.nanoTime() + System.nanoTime();
  }

  /**
   * Reads the clock reflectively, the path the agent cannot see.
   *
   * @return the value of {@link System#nanoTime()} obtained through reflection
   * @throws Exception if the reflective lookup or invocation fails
   */
  public static long reflectiveNanoTime() throws Exception {
    return (long) System.class.getMethod("nanoTime").invoke(null);
  }

  /**
   * Reads the clock in a bounded loop, used to drive the event cap.
   *
   * @param times how many reads to perform
   * @return the number of reads requested
   */
  public static int spin(int times) {
    long sink = 0;
    for (int i = 0; i < times; i++) {
      sink += System.nanoTime();
    }
    return sink == Long.MIN_VALUE ? -1 : times;
  }
}
