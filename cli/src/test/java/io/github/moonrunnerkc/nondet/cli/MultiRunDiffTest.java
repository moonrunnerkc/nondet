package io.github.moonrunnerkc.nondet.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.moonrunnerkc.nondet.catalog.Category;
import io.github.moonrunnerkc.nondet.catalog.Event;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Aggregating first-divergence across N runs: the baseline is run zero, every later run is
 * diffed against it, the earliest divergence wins, and every other diverging site comes
 * back as a secondary list.
 */
class MultiRunDiffTest {

  private static Event event(long seq, String callSiteId, String value) {
    return new Event(seq, Category.TIME, callSiteId, value);
  }

  @Test
  void reportsNoneWhenEveryRunAgreesWithTheBaseline() {
    final List<Event> run = List.of(event(0, "id-a", "1"), event(1, "id-b", "2"));
    final MultiRunDiff.Result result = MultiRunDiff.analyze(List.of(run, run, run));

    assertEquals(Divergence.Kind.NONE, result.primary().kind());
    assertTrue(result.additionalSiteIds().isEmpty(), "agreement leaves no diverging sites");
  }

  @Test
  void picksTheEarliestDivergenceAcrossPairingsAsPrimary() {
    final List<Event> baseline = List.of(event(0, "id-a", "1"), event(1, "id-b", "2"));
    final List<Event> agreesThenDiffersLate = List.of(event(0, "id-a", "1"), event(1, "id-b", "99"));
    final List<Event> differsEarly = List.of(event(0, "id-a", "7"), event(1, "id-b", "2"));

    final MultiRunDiff.Result result =
        MultiRunDiff.analyze(List.of(baseline, agreesThenDiffersLate, differsEarly));

    assertEquals(Divergence.Kind.MISMATCH, result.primary().kind());
    assertEquals(0, result.primary().index(), "the read #0 divergence is earlier than the read #1 one");
  }

  @Test
  void listsDistinctDivergingSitesBeyondThePrimaryOne() {
    final List<Event> baseline = List.of(event(0, "id-a", "1"), event(1, "id-b", "2"));
    final List<Event> differsAtFirstSite = List.of(event(0, "id-a", "9"), event(1, "id-b", "2"));
    final List<Event> differsAtSecondSite = List.of(event(0, "id-a", "1"), event(1, "id-b", "9"));

    final MultiRunDiff.Result result =
        MultiRunDiff.analyze(List.of(baseline, differsAtFirstSite, differsAtSecondSite));

    assertEquals(0, result.primary().index(), "id-a diverges earliest, so it is primary");
    assertEquals(List.of("id-b"), result.additionalSiteIds(),
        "id-b diverges in the third run and is listed as an additional site");
  }

  @Test
  void rejectsFewerThanTwoRuns() {
    final List<Event> only = List.of(event(0, "id-a", "1"));
    try {
      MultiRunDiff.analyze(List.of(only));
      assertTrue(false, "a single run should not be analyzable");
    } catch (final IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("at least two runs"), expected.getMessage());
    }
  }
}
