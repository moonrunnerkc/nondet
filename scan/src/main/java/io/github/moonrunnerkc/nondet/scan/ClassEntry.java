package io.github.moonrunnerkc.nondet.scan;

import java.util.Objects;

/**
 * A single class file to scan, paired with where it came from.
 *
 * <p>{@code origin} is a human readable location, a file path or a {@code jar!entry}
 * reference, used only in diagnostics. The scanner reads the real class and method
 * names from {@code bytecode}, so {@code origin} need not be a valid class name.
 *
 * @param origin   where the bytes were read from, for diagnostics, never {@code null}
 * @param bytecode the raw class file bytes, never {@code null}
 */
public record ClassEntry(String origin, byte[] bytecode) {

  /**
   * Validates that both fields are present.
   *
   * @throws NullPointerException if {@code origin} or {@code bytecode} is {@code null}
   */
  public ClassEntry {
    Objects.requireNonNull(origin, "origin is required for a class entry");
    Objects.requireNonNull(bytecode, "bytecode is required for a class entry");
  }
}
