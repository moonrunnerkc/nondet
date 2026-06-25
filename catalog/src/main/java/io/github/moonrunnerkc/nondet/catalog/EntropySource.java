package io.github.moonrunnerkc.nondet.catalog;

import java.util.Objects;

/**
 * One entry in the entropy catalog: a JDK method whose return value is a source of
 * nondeterminism.
 *
 * <p>The owner and name use JVM internal form so they compare directly against the
 * operands ASM reports at a call site. The owner is a slash separated internal name
 * such as {@code java/lang/System}; the name is the bare method name such as
 * {@code nanoTime}.
 *
 * <p>{@code descriptor} is the method descriptor in JVM form, for example
 * {@code ()J}. When {@code descriptor} is {@code null} the source matches every
 * overload of {@code owner.name}, which is how the catalog covers the two
 * {@code getenv} and two {@code getProperty} signatures with a single entry.
 *
 * @param category the kind of entropy this source introduces
 * @param owner    the declaring class in internal form, never {@code null}
 * @param name     the method name, never {@code null}
 * @param descriptor the JVM method descriptor, or {@code null} to match any overload
 * @param api      a human readable label such as {@code System.nanoTime}, never {@code null}
 */
public record EntropySource(
    Category category,
    String owner,
    String name,
    String descriptor,
    String api) {

  /**
   * Validates that the required coordinates are present.
   *
   * @throws NullPointerException if {@code category}, {@code owner}, {@code name},
   *     or {@code api} is {@code null}
   */
  public EntropySource {
    Objects.requireNonNull(category, "category is required for an entropy source");
    Objects.requireNonNull(owner, "owner is required, for example java/lang/System");
    Objects.requireNonNull(name, "name is required, for example nanoTime");
    Objects.requireNonNull(api, "api label is required, for example System.nanoTime");
  }

  /**
   * Reports whether a call site invokes this entropy source.
   *
   * <p>Owner and name must match exactly. The descriptor must match only when this
   * source pins one; a {@code null} descriptor matches every overload.
   *
   * @param callOwner      the call site owner in internal form, for example {@code java/lang/System}
   * @param callName       the invoked method name
   * @param callDescriptor the invoked method descriptor in JVM form
   * @return {@code true} when this source describes the given call, {@code false} otherwise
   */
  public boolean matches(String callOwner, String callName, String callDescriptor) {
    if (!owner.equals(callOwner) || !name.equals(callName)) {
      return false;
    }
    return descriptor == null || descriptor.equals(callDescriptor);
  }
}
