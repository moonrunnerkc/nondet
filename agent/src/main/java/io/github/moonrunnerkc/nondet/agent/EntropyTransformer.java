package io.github.moonrunnerkc.nondet.agent;

import io.github.moonrunnerkc.nondet.catalog.Catalog;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Objects;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * Transforms loaded classes, routing each through the entropy rewrite.
 *
 * <p>Classes the {@link SkipList} rejects are returned unmodified. Everything else is
 * read with ASM and written back through an {@link EntropyClassVisitor}. The class writer
 * runs with {@code COMPUTE_MAXS} so a rewrite that changes stack depth still produces a
 * valid frame. Any failure during transformation returns {@code null}, which tells the
 * JVM to keep the original bytes, so a bad transform can never crash the target program.
 *
 * <p>The set of Hook signatures the rewrite may target is resolved once here, at
 * construction, and handed down to every method visitor. A matched call site whose
 * derived Hook does not exist is left untouched, so an unsupported overload is a safe
 * no-op rather than a dangling call.
 *
 * <p>When {@value #DEBUG_PROPERTY} is set, a class that fails to transform is named on
 * stderr before its original bytes are kept, so a silent pass-through can be traced back
 * to the class that triggered it. The default stays quiet so an instrumented program's
 * output is not polluted.
 */
public final class EntropyTransformer implements ClassFileTransformer {

  /** System property that turns on naming each class that failed to transform. */
  public static final String DEBUG_PROPERTY = "nondet.debug";

  private final Catalog catalog;
  private final Set<String> hookSignatures;

  /**
   * Creates a transformer that matches call sites against the given catalog.
   *
   * @param catalog the entropy catalog, never {@code null}
   * @throws NullPointerException if {@code catalog} is {@code null}
   */
  public EntropyTransformer(Catalog catalog) {
    this.catalog = Objects.requireNonNull(catalog, "catalog is required to transform classes");
    this.hookSignatures = HookMethods.signatures();
  }

  /**
   * Transforms one class, or returns {@code null} to leave it unchanged.
   *
   * @param loader              the defining class loader, may be {@code null} for the bootstrap loader
   * @param className           the class name in internal form, may be {@code null}
   * @param classBeingRedefined the class if this is a redefinition, otherwise {@code null}
   * @param protectionDomain    the protection domain of the class
   * @param classfileBuffer     the input class file bytes
   * @return the rewritten bytes, or {@code null} to keep the original bytes
   */
  @Override
  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
      ProtectionDomain protectionDomain, byte[] classfileBuffer) {
    if (SkipList.shouldSkip(className)) {
      return null;
    }
    try {
      final ClassReader reader = new ClassReader(classfileBuffer);
      final ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
      reader.accept(new EntropyClassVisitor(writer, catalog, hookSignatures), 0);
      return writer.toByteArray();
    } catch (final Throwable failure) {
      if (Boolean.getBoolean(DEBUG_PROPERTY)) {
        final String named = className == null ? "an unnamed class" : className.replace('/', '.');
        System.err.println("nondet agent: kept original bytes for " + named
            + " after a failed transform (" + failure + ")");
      }
      return null;
    }
  }
}
