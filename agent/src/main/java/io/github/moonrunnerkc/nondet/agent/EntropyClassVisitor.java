package io.github.moonrunnerkc.nondet.agent;

import io.github.moonrunnerkc.nondet.catalog.Catalog;
import java.util.Set;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Wires each method through the rewrite visitors during transformation.
 *
 * <p>This visitor only captures the class name and delegates to the class writer. Each method body
 * is run through two visitors in a chain: the {@link EntropyMethodVisitor} rewrites direct catalog
 * calls, and the {@link ReflectiveInvokeVisitor} behind it rewrites {@code Method.invoke} calls the
 * direct rewrite passed through. They act on different instructions, so the order only decides which
 * looks first; neither disturbs the other's work.
 */
final class EntropyClassVisitor extends ClassVisitor {

  private final Catalog catalog;
  private final Set<String> hookSignatures;
  private String className = "";

  EntropyClassVisitor(ClassVisitor delegate, Catalog catalog, Set<String> hookSignatures) {
    super(Opcodes.ASM9, delegate);
    this.catalog = catalog;
    this.hookSignatures = hookSignatures;
  }

  @Override
  public void visit(int version, int access, String name, String signature,
      String superName, String[] interfaces) {
    this.className = name;
    super.visit(version, access, name, signature, superName, interfaces);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String descriptor,
      String signature, String[] exceptions) {
    final MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
    final MethodVisitor reflective = new ReflectiveInvokeVisitor(delegate, className, name);
    return new EntropyMethodVisitor(reflective, catalog, className, name, hookSignatures);
  }
}
