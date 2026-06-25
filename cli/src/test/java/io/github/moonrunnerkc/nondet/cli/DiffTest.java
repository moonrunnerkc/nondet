package io.github.moonrunnerkc.nondet.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.moonrunnerkc.nondet.catalog.Category;
import io.github.moonrunnerkc.nondet.catalog.Event;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Behavior of first-divergence: the walk reports the first index where two runs disagree
 * on a call site or value, a length divergence when one run is longer, or agreement.
 */
class DiffTest {

  private static Event event(long seq, String callSiteId, String value) {
    return new Event(seq, Category.TIME, callSiteId, value);
  }

  @Test
  void reportsNoneWhenBothRunsReadTheSameEntropyInTheSameOrder() {
    final List<Event> run = List.of(event(0, "id-a", "1"), event(1, "id-b", "2"));
    assertEquals(Divergence.Kind.NONE, Diff.first(run, run).kind());
  }

  @Test
  void reportsTheFirstIndexWhereValuesDisagree() {
    final List<Event> first = List.of(event(0, "id-a", "1"), event(1, "id-b", "2"));
    final List<Event> second = List.of(event(0, "id-a", "1"), event(1, "id-b", "99"));

    final Divergence divergence = Diff.first(first, second);
    assertEquals(Divergence.Kind.MISMATCH, divergence.kind());
    assertEquals(1, divergence.index());
  }

  @Test
  void reportsTheFirstIndexWhereCallSitesDisagree() {
    final List<Event> first = List.of(event(0, "id-a", "1"));
    final List<Event> second = List.of(event(0, "id-x", "1"));

    final Divergence divergence = Diff.first(first, second);
    assertEquals(Divergence.Kind.MISMATCH, divergence.kind());
    assertEquals(0, divergence.index());
  }

  @Test
  void reportsALengthDivergenceWhenOneRunIsLonger() {
    final List<Event> first = List.of(event(0, "id-a", "1"), event(1, "id-b", "2"));
    final List<Event> second = List.of(event(0, "id-a", "1"));

    final Divergence divergence = Diff.first(first, second);
    assertEquals(Divergence.Kind.LENGTH, divergence.kind());
    assertEquals(1, divergence.index());
  }
}
