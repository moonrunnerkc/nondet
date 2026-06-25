package io.github.moonrunnerkc.nondet.scan;

import io.github.moonrunnerkc.nondet.catalog.Catalog;
import io.github.moonrunnerkc.nondet.catalog.CallSiteId;
import io.github.moonrunnerkc.nondet.catalog.EntropySource;
import java.util.List;
import java.util.Optional;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Tracks the current source line and records a finding for each catalog match.
 *
 * <p>Line numbers come from the {@code LineNumberTable}, so classes compiled without
 * debug information yield findings with line {@code -1}. The visitor reads only; it
 * never rewrites an instruction.
 */
final class EntropyCallVisitor extends MethodVisitor {

  private static final int UNKNOWN_LINE = -1;

  private final Catalog catalog;
  private final String className;
  private final String methodName;
  private final List<Finding> findings;
  private int currentLine = UNKNOWN_LINE;

  EntropyCallVisitor(Catalog catalog, String className, String methodName, List<Finding> findings) {
    super(Opcodes.ASM9);
    this.catalog = catalog;
    this.className = className;
    this.methodName = methodName;
    this.findings = findings;
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    this.currentLine = line;
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
      boolean isInterface) {
    final Optional<EntropySource> source = catalog.match(owner, name, descriptor);
    if (source.isPresent()) {
      final String api = source.get().api();
      final String id = CallSiteId.of(className, methodName, currentLine, api);
      findings.add(new Finding(id, className, methodName, currentLine, source.get().category(), api));
    }
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
  }
}
