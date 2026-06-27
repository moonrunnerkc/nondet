package io.github.moonrunnerkc.nondet.agent;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Rewrites {@code Method.invoke} call sites so a catalog source reached through reflection is
 * recorded.
 *
 * <p>The direct-call rewrite reads a catalog target straight off the bytecode operands, which a
 * reflective call does not carry: {@code someMethod.invoke(receiver, args)} names neither the owner
 * nor the method until run time. So this visitor replaces every {@code invokevirtual} of
 * {@code java.lang.reflect.Method.invoke} with a static call to {@link Hook#reflectInvoke}, pushing
 * the reflective caller's class, method, and line on top of the existing operands. The Hook resolves
 * the real target at run time and records it only when it is a catalog source, attributing the read
 * to this caller.
 *
 * <p>This runs alongside the direct-call rewrite. It is given the downstream visitor and only acts
 * on {@code Method.invoke}; every other instruction passes through, including the catalog calls the
 * direct rewrite already handled before delegating here.
 */
final class ReflectiveInvokeVisitor extends MethodVisitor {

  private static final int UNKNOWN_LINE = -1;
  private static final String HOOK_OWNER = "io/github/moonrunnerkc/nondet/agent/Hook";
  private static final String METHOD_OWNER = "java/lang/reflect/Method";
  private static final String INVOKE_NAME = "invoke";
  private static final String INVOKE_DESCRIPTOR =
      "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;";
  private static final String REFLECT_HOOK_DESCRIPTOR = "(Ljava/lang/reflect/Method;Ljava/lang/Object;"
      + "[Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/Object;";

  private final String declaringClass;
  private final String methodName;
  private int currentLine = UNKNOWN_LINE;

  /**
   * Creates a visitor for one method body.
   *
   * @param delegate       the downstream visitor that writes the method, never {@code null}
   * @param declaringClass the enclosing class in internal form, never {@code null}
   * @param methodName     the method being visited, never {@code null}
   */
  ReflectiveInvokeVisitor(MethodVisitor delegate, String declaringClass, String methodName) {
    super(Opcodes.ASM9, delegate);
    this.declaringClass = declaringClass;
    this.methodName = methodName;
  }

  /**
   * Tracks the current source line so a rewritten reflective call can be attributed to it.
   *
   * @param line  the source line number
   * @param start the label where the line begins
   */
  @Override
  public void visitLineNumber(int line, Label start) {
    this.currentLine = line;
    super.visitLineNumber(line, start);
  }

  /**
   * Rewrites a {@code Method.invoke} call to {@link Hook#reflectInvoke}, or forwards it unchanged.
   *
   * @param opcode      the invoke opcode
   * @param owner       the owner class in internal form
   * @param name        the invoked method name
   * @param descriptor  the invoked method descriptor
   * @param isInterface whether the owner is an interface
   */
  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
      boolean isInterface) {
    if (opcode == Opcodes.INVOKEVIRTUAL && METHOD_OWNER.equals(owner) && INVOKE_NAME.equals(name)
        && INVOKE_DESCRIPTOR.equals(descriptor)) {
      super.visitLdcInsn(declaringClass);
      super.visitLdcInsn(methodName);
      super.visitLdcInsn(currentLine);
      super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_OWNER, "reflectInvoke",
          REFLECT_HOOK_DESCRIPTOR, false);
      return;
    }
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
  }
}
