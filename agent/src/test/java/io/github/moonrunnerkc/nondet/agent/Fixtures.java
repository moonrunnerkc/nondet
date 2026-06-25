package io.github.moonrunnerkc.nondet.agent;

import io.github.moonrunnerkc.nondet.catalog.Catalog;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Shared plumbing for the bytecode fixture tests: read a real compiled class, run it
 * through the live {@link EntropyTransformer}, and load the rewritten bytes so the
 * instrumented methods actually run and record.
 *
 * <p>Using the real transformer rather than a mock means the tests exercise the same
 * matching, guard, and rewrite the agent uses in production.
 */
final class Fixtures {

  private Fixtures() {
  }

  /**
   * Reads the compiled bytes of a fixture from the test class path.
   *
   * @param internalName the class internal name, for example {@code probe/EntropyProbes}
   * @return the original class bytes
   * @throws Exception if the resource is missing or unreadable
   */
  static byte[] rawBytes(String internalName) throws Exception {
    final Path file = Paths.get(Fixtures.class.getResource("/" + internalName + ".class").toURI());
    return Files.readAllBytes(file);
  }

  /**
   * Transforms class bytes with the default catalog and loads them in a fresh loader.
   *
   * @param internalName the class internal name, for example {@code probe/EntropyProbes}
   * @param original     the bytes to transform
   * @return the loaded, rewritten class
   */
  static Class<?> load(String internalName, byte[] original) {
    final byte[] rewritten = new EntropyTransformer(Catalog.ofDefault())
        .transform(Fixtures.class.getClassLoader(), internalName, null, null, original);
    final String binaryName = internalName.replace('/', '.');
    return new BytesLoader().define(binaryName, rewritten);
  }

  /**
   * Reads, transforms, and loads a fixture in one step.
   *
   * @param internalName the class internal name, for example {@code probe/EntropyProbes}
   * @return the loaded, rewritten class
   * @throws Exception if the fixture cannot be read
   */
  static Class<?> transformAndLoad(String internalName) throws Exception {
    return load(internalName, rawBytes(internalName));
  }

  /**
   * Returns a copy of the class with its line number table removed.
   *
   * <p>Used to prove the rewrite and id derivation still work for a class compiled without
   * debug line information.
   *
   * @param original the class bytes to strip
   * @return equivalent bytes with no {@code LineNumberTable}
   */
  static byte[] stripLineNumbers(byte[] original) {
    final ClassReader reader = new ClassReader(original);
    final ClassWriter writer = new ClassWriter(reader, 0);
    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
      @Override
      public MethodVisitor visitMethod(int access, String name, String descriptor,
          String signature, String[] exceptions) {
        final MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new MethodVisitor(Opcodes.ASM9, delegate) {
          @Override
          public void visitLineNumber(int line, Label start) {
            // drop it, leaving no LineNumberTable for the method
          }
        };
      }
    }, 0);
    return writer.toByteArray();
  }

  private static final class BytesLoader extends ClassLoader {
    Class<?> define(String name, byte[] bytes) {
      return defineClass(name, bytes, 0, bytes.length);
    }
  }
}
