package io.github.moonrunnerkc.nondet.agent;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.Type;

/**
 * The set of call targets the rewrite is allowed to emit, derived from {@link Hook}.
 *
 * <p>The catalog matches a source by owner and name, so for {@code getenv} and
 * {@code getProperty} a single catalog entry covers every JDK overload. Not every
 * overload has a Hook, though, so the visitor needs a way to tell a supported overload
 * from one it must leave alone. This reflects over Hook's public static methods once and
 * records each as {@code name + descriptor}, the same key the visitor builds from a call
 * site. A call site whose derived Hook key is absent here is passed through unchanged
 * instead of being rewritten to a method that does not exist, which would be a linkage
 * error the moment the instrumented code ran.
 */
final class HookMethods {

  private HookMethods() {
  }

  /**
   * Collects the {@code name + descriptor} key of every public static {@link Hook} method.
   *
   * <p>The descriptor is the JVM method descriptor, for example
   * {@code (Ljava/lang/String;)J}, so the key concatenates to {@code nanoTime(Ljava/lang/String;)J}.
   * A method name never contains {@code (} and a descriptor always starts with it, so the
   * concatenation is unambiguous without a separator.
   *
   * @return an immutable set of supported Hook signature keys
   */
  static Set<String> signatures() {
    final Set<String> signatures = new HashSet<>();
    for (final Method method : Hook.class.getDeclaredMethods()) {
      if (Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers())) {
        signatures.add(method.getName() + Type.getMethodDescriptor(method));
      }
    }
    return Set.copyOf(signatures);
  }
}
