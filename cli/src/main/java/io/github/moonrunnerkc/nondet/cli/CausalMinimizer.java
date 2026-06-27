package io.github.moonrunnerkc.nondet.cli;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Delta-debugging minimization over a set of candidate reads.
 *
 * <p>Given the candidate reads that differ between a passing and a failing context, and a test that
 * says whether forcing a chosen subset to its failing values reproduces the failure, this returns
 * the smallest subset that still reproduces it. It is the ddmin algorithm: split the candidates,
 * try each part on its own, then try each part removed, and narrow until no single part can be
 * dropped without losing the failure. The result is one-minimal, so removing any one read from it
 * no longer reproduces.
 *
 * <p>It is deterministic. The candidates are taken in the order given, the splits are contiguous,
 * and the parts are tried in order, so the same candidates and the same test always yield the same
 * minimal set and the same sequence of trials. The test is expected to hold for the full candidate
 * set; the caller checks the empty set separately to rule out a cause outside the candidates.
 */
final class CausalMinimizer {

  private CausalMinimizer() {
  }

  /**
   * Returns the smallest subset of candidates that still reproduces the failure.
   *
   * @param candidates  the candidate reads, in a stable order, never {@code null}
   * @param reproduces  the test: true when forcing the given subset to failing values reproduces
   *     the failing outcome; must return true for the full candidate set
   * @param <T>         the candidate type
   * @return the one-minimal reproducing subset, in candidate order
   */
  static <T> List<T> minimize(List<T> candidates, Predicate<Set<T>> reproduces) {
    List<T> current = new ArrayList<>(candidates);
    int parts = 2;
    while (current.size() >= 2) {
      final List<List<T>> chunks = split(current, Math.min(parts, current.size()));
      final List<T> reducedToChunk = firstReproducing(chunks, reproduces);
      if (reducedToChunk != null) {
        current = reducedToChunk;
        parts = 2;
        continue;
      }
      final List<T> reducedToComplement = firstReproducingComplement(current, chunks, reproduces);
      if (reducedToComplement != null) {
        current = reducedToComplement;
        parts = Math.max(parts - 1, 2);
        continue;
      }
      if (parts >= current.size()) {
        break;
      }
      parts = Math.min(current.size(), 2 * parts);
    }
    return current;
  }

  private static <T> List<T> firstReproducing(List<List<T>> chunks, Predicate<Set<T>> reproduces) {
    for (final List<T> chunk : chunks) {
      if (reproduces.test(new LinkedHashSet<>(chunk))) {
        return chunk;
      }
    }
    return null;
  }

  private static <T> List<T> firstReproducingComplement(List<T> current, List<List<T>> chunks,
      Predicate<Set<T>> reproduces) {
    if (chunks.size() < 2) {
      return null;
    }
    for (final List<T> chunk : chunks) {
      final List<T> complement = new ArrayList<>(current);
      complement.removeAll(chunk);
      if (!complement.isEmpty() && reproduces.test(new LinkedHashSet<>(complement))) {
        return complement;
      }
    }
    return null;
  }

  private static <T> List<List<T>> split(List<T> items, int parts) {
    final List<List<T>> chunks = new ArrayList<>(parts);
    final int size = items.size();
    int start = 0;
    for (int i = 0; i < parts; i++) {
      final int end = start + (size - start) / (parts - i);
      chunks.add(new ArrayList<>(items.subList(start, end)));
      start = end;
    }
    return chunks;
  }
}
