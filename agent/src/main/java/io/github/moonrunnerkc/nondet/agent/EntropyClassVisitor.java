package io.github.moonrunnerkc.nondet.agent;

import io.github.moonrunnerkc.nondet.catalog.Catalog;
import java.util.Set;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Wires each method through an {@link EntropyMethodVisitor} during transformation.
 *
 * <p>This visitor only captures the class name and delegates to the class writer. The
 * decision to rewrite a call site lives in the method visitor, which also consults the
 * Hook signature set passed through here to skip overloads it cannot safely target.
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
    return new EntropyMethodVisitor(delegate, catalog, className, name, hookSignatures);
  }
}
