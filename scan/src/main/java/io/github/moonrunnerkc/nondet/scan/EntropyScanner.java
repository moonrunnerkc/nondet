package io.github.moonrunnerkc.nondet.scan;

import io.github.moonrunnerkc.nondet.catalog.Catalog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.objectweb.asm.ClassReader;

/**
 * Scans class files for calls that match the entropy catalog.
 *
 * <p>The scan is read only. It never modifies bytecode and never loads the scanned
 * classes; it walks the constant pool and method bodies with ASM and records a
 * {@link Finding} for every call site whose target is in the catalog. Output order is
 * sorted and therefore reproducible.
 */
public final class EntropyScanner {

  private final Catalog catalog;

  /**
   * Creates a scanner that matches against the given catalog.
   *
   * @param catalog the entropy catalog to match call sites against, never {@code null}
   * @throws NullPointerException if {@code catalog} is {@code null}
   */
  public EntropyScanner(Catalog catalog) {
    this.catalog = Objects.requireNonNull(catalog, "catalog is required to scan");
  }

  /**
   * Scans many class entries and returns every entropy call site found, sorted.
   *
   * @param classes the class files to scan, never {@code null}
   * @return all findings across the inputs, in {@link Finding} sort order
   */
  public List<Finding> scan(List<ClassEntry> classes) {
    final List<Finding> all = new ArrayList<>();
    for (final ClassEntry entry : classes) {
      all.addAll(scan(entry));
    }
    Collections.sort(all);
    return all;
  }

  /**
   * Scans a single class entry.
   *
   * @param entry the class file to scan, never {@code null}
   * @return the entropy call sites in this class, in {@link Finding} sort order
   * @throws IllegalArgumentException if the bytes are not a readable class file
   */
  public List<Finding> scan(ClassEntry entry) {
    final List<Finding> findings = new ArrayList<>();
    final ClassReader reader = readerFor(entry);
    reader.accept(new ScanClassVisitor(catalog, findings), ClassReader.SKIP_FRAMES);
    Collections.sort(findings);
    return findings;
  }

  private static ClassReader readerFor(ClassEntry entry) {
    try {
      return new ClassReader(entry.bytecode());
    } catch (final RuntimeException cause) {
      throw new IllegalArgumentException(
          "not a readable class file: " + entry.origin() + "; check the path points at compiled .class output",
          cause);
    }
  }
}
