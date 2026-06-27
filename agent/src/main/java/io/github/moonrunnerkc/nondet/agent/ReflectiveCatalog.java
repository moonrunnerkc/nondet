package io.github.moonrunnerkc.nondet.agent;

import io.github.moonrunnerkc.nondet.catalog.Catalog;
import io.github.moonrunnerkc.nondet.catalog.EntropySource;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Matches a reflectively invoked {@link Method} against the entropy catalog.
 *
 * <p>The catalog matches a call by its owner, name, and JVM descriptor, which the static rewrite
 * reads straight off the bytecode. A reflective call has no bytecode operands to read, only a
 * {@link Method} object at run time, so this derives the same owner, name, and descriptor from that
 * object and asks the catalog. The descriptor is built with plain JDK reflection, no ASM, so this
 * stays usable from the pure-JDK runtime.
 */
final class ReflectiveCatalog {

  private static final Catalog CATALOG = Catalog.ofDefault();

  private ReflectiveCatalog() {
  }

  /**
   * Returns the catalog source a reflectively invoked method is, if any.
   *
   * @param method the method about to be invoked through reflection, never {@code null}
   * @return the matching entropy source, or {@link Optional#empty()} when the method is not entropy
   */
  static Optional<EntropySource> match(Method method) {
    final String owner = method.getDeclaringClass().getName().replace('.', '/');
    return CATALOG.match(owner, method.getName(), descriptorOf(method));
  }

  private static String descriptorOf(Method method) {
    final StringBuilder descriptor = new StringBuilder("(");
    for (final Class<?> parameter : method.getParameterTypes()) {
      descriptor.append(typeDescriptor(parameter));
    }
    return descriptor.append(')').append(typeDescriptor(method.getReturnType())).toString();
  }

  private static String typeDescriptor(Class<?> type) {
    if (type.isArray()) {
      return type.getName().replace('.', '/');
    }
    if (!type.isPrimitive()) {
      return 'L' + type.getName().replace('.', '/') + ';';
    }
    return switch (type.getName()) {
      case "void" -> "V";
      case "boolean" -> "Z";
      case "byte" -> "B";
      case "char" -> "C";
      case "short" -> "S";
      case "int" -> "I";
      case "long" -> "J";
      case "float" -> "F";
      case "double" -> "D";
      default -> throw new IllegalStateException("unknown primitive type " + type.getName());
    };
  }
}
