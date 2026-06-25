package io.github.moonrunnerkc.nondet.agent;

import io.github.moonrunnerkc.nondet.catalog.CallSite;
import io.github.moonrunnerkc.nondet.catalog.CallSiteId;
import io.github.moonrunnerkc.nondet.catalog.Catalog;
import io.github.moonrunnerkc.nondet.catalog.EntropySource;
import java.util.Optional;
import java.util.Set;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Rewrites entropy call sites so each read is recorded at run time.
 *
 * <p>For every {@code visitMethodInsn} whose owner, name, and descriptor match the
 * {@link Catalog}, the original call is replaced by an {@code INVOKESTATIC} to the matching
 * {@link Hook} method. A Hook method mirrors the original signature with the call site id
 * appended as a final {@code String} argument, so the original arguments are already on the
 * stack when the call site is visited; the rewrite pushes the id with {@code LDC} on top of
 * them and then invokes the Hook, which calls the real JDK api, records the value, and
 * returns it unchanged. The id is the stable digest from
 * {@link CallSiteId#of(String, String, int, String)} over the current line. Before emitting
 * the call the visitor registers a {@link CallSite} for that id so a trace can be rendered
 * back to coordinates. Calls that do not match the catalog pass through untouched.
 *
 * <p>The class writer runs with {@code COMPUTE_MAXS}, so the one extra stack slot the id
 * needs is accounted for without manual frame bookkeeping. The {@link Hook} owner lives in
 * the agent package, which the {@link SkipList} skips, so the rewritten call never recurses
 * back through this visitor.
 */
final class EntropyMethodVisitor extends MethodVisitor {

  private static final int UNKNOWN_LINE = -1;

  /** Internal name of the runtime the rewritten call sites jump to. */
  private static final String HOOK_OWNER = "io/github/moonrunnerkc/nondet/agent/Hook";

  /** The descriptor fragment for the call site id parameter the Hook appends. */
  private static final String ID_PARAMETER = "Ljava/lang/String;";

  private final Catalog catalog;
  private final String declaringClass;
  private final String methodName;
  private final Set<String> hookSignatures;
  private int currentLine = UNKNOWN_LINE;

  /**
   * Creates a method visitor for one method body.
   *
   * @param delegate       the downstream visitor that writes the method, never {@code null}
   * @param catalog        the entropy catalog to match call sites against, never {@code null}
   * @param declaringClass the enclosing class in internal form, never {@code null}
   * @param methodName     the method being visited, never {@code null}
   * @param hookSignatures the {@code name + descriptor} keys of every Hook the rewrite may
   *     target, never {@code null}
   */
  EntropyMethodVisitor(MethodVisitor delegate, Catalog catalog, String declaringClass,
      String methodName, Set<String> hookSignatures) {
    super(Opcodes.ASM9, delegate);
    this.catalog = catalog;
    this.declaringClass = declaringClass;
    this.methodName = methodName;
    this.hookSignatures = hookSignatures;
  }

  /**
   * Tracks the current source line so a rewritten call site can be given a stable id.
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
   * Rewrites a matched entropy call to its {@link Hook}, or forwards it unchanged.
   *
   * <p>On a catalog match the original invoke is dropped: the visitor pushes the call site
   * id on top of the original arguments and emits an {@code INVOKESTATIC} to the Hook method
   * of the same name, whose descriptor is the call descriptor with the id parameter
   * appended. A match whose derived Hook does not exist, such as the no-arg
   * {@code System.getenv()} the catalog still matches by owner and name, passes through
   * verbatim rather than becoming a call to a missing method. On a miss the instruction
   * passes through verbatim too.
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
    final Optional<EntropySource> match = catalog.match(owner, name, descriptor);
    if (match.isEmpty()) {
      super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
      return;
    }
    final String hookDescriptor = hookDescriptor(descriptor);
    if (!hookSignatures.contains(name + hookDescriptor)) {
      super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
      return;
    }
    final EntropySource source = match.get();
    final String callSiteId = CallSiteId.of(declaringClass, methodName, currentLine, source.api());
    Registry.register(new CallSite(callSiteId, declaringClass, methodName, currentLine, source.api()));
    super.visitLdcInsn(callSiteId);
    super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_OWNER, name, hookDescriptor, false);
  }

  /**
   * Builds the Hook descriptor for a call descriptor by appending the id parameter.
   *
   * @param callDescriptor the original call site descriptor in JVM form, for example {@code ()J}
   * @return the same descriptor with a trailing {@code String} parameter, for example
   *     {@code (Ljava/lang/String;)J}
   */
  private static String hookDescriptor(String callDescriptor) {
    final int close = callDescriptor.indexOf(')');
    return callDescriptor.substring(0, close) + ID_PARAMETER + callDescriptor.substring(close);
  }
}
