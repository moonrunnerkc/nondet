package io.github.moonrunnerkc.nondet.catalog;

import java.util.List;
import java.util.Optional;

/**
 * The frozen v0.1.0 set of entropy sources, shared by the static scanner and the
 * dynamic agent so both attribute a call site to the same source and id.
 *
 * <p>{@link #DEFAULT} holds exactly six sources. Growing it is the main scope risk
 * for the tool and is gated on explicit approval, so the list is hard coded here
 * rather than configurable.
 */
public final class Catalog {

  /**
   * The six v0.1.0 entropy sources, in a fixed declaration order.
   *
   * <p>Order is stable so any iteration over the catalog is reproducible. The
   * {@code getenv} and {@code getProperty} entries carry a {@code null} descriptor so
   * each covers both of its JDK overloads.
   */
  public static final List<EntropySource> DEFAULT = List.of(
      new EntropySource(Category.TIME, "java/lang/System", "nanoTime", "()J", "System.nanoTime"),
      new EntropySource(Category.TIME, "java/lang/System", "currentTimeMillis", "()J", "System.currentTimeMillis"),
      new EntropySource(Category.RANDOM, "java/lang/Math", "random", "()D", "Math.random"),
      new EntropySource(Category.RANDOM, "java/util/UUID", "randomUUID", "()Ljava/util/UUID;", "UUID.randomUUID"),
      new EntropySource(Category.ENV, "java/lang/System", "getenv", null, "System.getenv"),
      new EntropySource(Category.SYSPROP, "java/lang/System", "getProperty", null, "System.getProperty"));

  private final List<EntropySource> sources;

  private Catalog(List<EntropySource> sources) {
    this.sources = List.copyOf(sources);
  }

  /**
   * Returns a catalog backed by the frozen {@link #DEFAULT} source set.
   *
   * @return a catalog over the six v0.1.0 entropy sources
   */
  public static Catalog ofDefault() {
    return new Catalog(DEFAULT);
  }

  /**
   * Returns the sources this catalog matches against, in declaration order.
   *
   * @return an unmodifiable list of entropy sources
   */
  public List<EntropySource> sources() {
    return sources;
  }

  /**
   * Finds the catalog source for a call site, if any.
   *
   * <p>The first source in declaration order whose {@link EntropySource#matches}
   * accepts the call wins. The catalog has no two sources that match the same call,
   * so the result is unambiguous.
   *
   * @param owner      the call site owner in internal form, for example {@code java/lang/System}
   * @param name       the invoked method name
   * @param descriptor the invoked method descriptor in JVM form
   * @return the matching source, or {@link Optional#empty()} when the call is not entropy
   */
  public Optional<EntropySource> match(String owner, String name, String descriptor) {
    for (final EntropySource source : sources) {
      if (source.matches(owner, name, descriptor)) {
        return Optional.of(source);
      }
    }
    return Optional.empty();
  }
}
