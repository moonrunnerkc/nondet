package io.github.moonrunnerkc.nondet.scan;

import io.github.moonrunnerkc.nondet.catalog.Catalog;
import java.util.List;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Captures the class name and hands each method to an {@link EntropyCallVisitor}.
 *
 * <p>Package private: the scanner owns the ASM visitor chain and nothing outside this
 * module should construct it.
 */
final class ScanClassVisitor extends ClassVisitor {

  private final Catalog catalog;
  private final List<Finding> findings;
  private String className = "";

  ScanClassVisitor(Catalog catalog, List<Finding> findings) {
    super(Opcodes.ASM9);
    this.catalog = catalog;
    this.findings = findings;
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
    return new EntropyCallVisitor(catalog, className, name, findings);
  }
}
