package io.github.moonrunnerkc.nondet.evidence;

import java.util.List;
import java.util.Objects;

/**
 * One entry in the evidence corpus: a program to run {@code nondet check} against.
 *
 * <p>A target names where its code came from so the report can cite provenance, the class path and
 * main class to run it, and any arguments. The provenance is free text, typically a repository and
 * a pinned commit for external code, or a note like "bundled example" for code in this repo.
 *
 * @param name       a short label for the target, used as a column and to name its repro bundle
 * @param provenance where the code came from, for example a repo and pinned commit, never {@code null}
 * @param classpath  the class path to run the target with, never {@code null}
 * @param mainClass  the fully qualified main class to run, never {@code null}
 * @param args       arguments passed to the target, never {@code null}
 */
public record CorpusTarget(String name, String provenance, String classpath, String mainClass,
    List<String> args) {

  /**
   * Validates the fields and copies the argument list.
   *
   * @throws NullPointerException if any reference field is {@code null}
   */
  public CorpusTarget {
    Objects.requireNonNull(name, "name is required for a corpus target");
    Objects.requireNonNull(provenance, "provenance is required for a corpus target");
    Objects.requireNonNull(classpath, "classpath is required for a corpus target");
    Objects.requireNonNull(mainClass, "mainClass is required for a corpus target");
    args = List.copyOf(args);
  }

  /**
   * Parses one tab-separated corpus line.
   *
   * <p>The line is {@code name<TAB>provenance<TAB>classpath<TAB>mainClass} with any further
   * tab-separated fields taken as the target's arguments. Leading and trailing whitespace on the
   * first four fields is trimmed; arguments are kept verbatim.
   *
   * @param line a corpus line, never {@code null}
   * @return the parsed target
   * @throws IllegalArgumentException if the line has fewer than four fields
   */
  public static CorpusTarget parse(String line) {
    final String[] fields = line.split("\t", -1);
    if (fields.length < 4) {
      throw new IllegalArgumentException(
          "a corpus line needs name, provenance, classpath, and mainClass, tab separated: " + line);
    }
    final List<String> args = fields.length > 4
        ? List.of(java.util.Arrays.copyOfRange(fields, 4, fields.length))
        : List.of();
    return new CorpusTarget(fields[0].trim(), fields[1].trim(), fields[2].trim(), fields[3].trim(),
        args);
  }
}
