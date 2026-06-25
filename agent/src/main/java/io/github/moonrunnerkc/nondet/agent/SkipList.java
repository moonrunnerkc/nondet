package io.github.moonrunnerkc.nondet.agent;

import java.util.List;

/**
 * Decides which classes the transformer leaves untouched.
 *
 * <p>The JDK and the JVM internals are skipped because instrumenting them risks
 * bootstrap deadlocks and gains nothing the catalog cares about. ASM is skipped because
 * the agent uses it to do the rewrite. Only the agent and catalog packages, the two
 * shaded into the agent jar, are skipped from the tool itself, so the agent never
 * instruments {@link Hook} or {@link Recorder} and recurses into itself.
 *
 * <p>The skip is deliberately narrow. The scanner, cli, examples, and any real workload
 * live outside those two packages and are instrumented, so the checker can record reads
 * in the bundled examples and in user code alike.
 */
public final class SkipList {

  private static final List<String> SKIP_PREFIXES = List.of(
      "java/",
      "jdk/",
      "sun/",
      "com/sun/",
      "org/objectweb/asm/",
      "io/github/moonrunnerkc/nondet/agent/",
      "io/github/moonrunnerkc/nondet/catalog/");

  private SkipList() {
  }

  /**
   * Reports whether a class should be left untouched.
   *
   * @param internalName the class name in internal form, for example {@code com/example/App},
   *     or {@code null} for a class with no name available
   * @return {@code true} when the class must not be instrumented
   */
  public static boolean shouldSkip(String internalName) {
    if (internalName == null) {
      return true;
    }
    for (final String prefix : SKIP_PREFIXES) {
      if (internalName.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }
}
