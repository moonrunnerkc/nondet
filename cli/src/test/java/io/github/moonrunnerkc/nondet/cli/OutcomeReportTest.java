package io.github.moonrunnerkc.nondet.cli;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.moonrunnerkc.nondet.catalog.CallSite;
import io.github.moonrunnerkc.nondet.catalog.Category;
import io.github.moonrunnerkc.nondet.catalog.Event;
import io.github.moonrunnerkc.nondet.catalog.Outcome;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OutcomeReportTest {

  private static final String SITE_ID = "00aa11bb22cc33dd";
  private static final Map<String, CallSite> REGISTRY = Map.of(
      SITE_ID, new CallSite(SITE_ID, "com/example/App", "main", 12, "System.nanoTime"));

  private static Outcome outcome(String stdout) {
    return Outcome.of(0, stdout.getBytes(StandardCharsets.UTF_8), new byte[0], null);
  }

  private static Event read(long seq, String value) {
    return new Event(seq, Category.TIME, SITE_ID, value);
  }

  @Test
  void identicalOutcomesWithVaryingReadsAreNoCausalNondeterminism() {
    final OutcomeReport report = OutcomeReport.of(List.of(
        new OutcomeReport.RunOutcome(1, 0, outcome("same"), List.of(read(0, "100"))),
        new OutcomeReport.RunOutcome(2, 0, outcome("same"), List.of(read(0, "200")))),
        REGISTRY, List.of(), 1);

    assertFalse(report.outcomesDiverged());
    final String text = report.render();
    assertTrue(text.contains("no causal nondeterminism"), text);
    assertTrue(text.contains("read different entropy values"), text);
  }

  @Test
  void identicalOutcomesWithNoReadsSayNothingWasRead() {
    final OutcomeReport report = OutcomeReport.of(List.of(
        new OutcomeReport.RunOutcome(1, 0, outcome("same"), List.of()),
        new OutcomeReport.RunOutcome(2, 0, outcome("same"), List.of())),
        REGISTRY, List.of(), 1);

    assertFalse(report.outcomesDiverged());
    assertTrue(report.render().contains("no entropy reads were observed"), report.render());
  }

  @Test
  void differingOutcomesPointAtTheCandidateRead() {
    final OutcomeReport report = OutcomeReport.of(List.of(
        new OutcomeReport.RunOutcome(1, 0, outcome("even"), List.of(read(0, "100"))),
        new OutcomeReport.RunOutcome(2, 0, outcome("odd"), List.of(read(0, "101")))),
        REGISTRY, List.of(), 1);

    assertTrue(report.outcomesDiverged());
    final String text = report.render();
    assertTrue(text.contains("different outcomes"), text);
    assertTrue(text.contains("com.example.App.main line 12 (System.nanoTime)"), text);
    assertTrue(text.contains("read 100") && text.contains("read 101"), text);
  }

  @Test
  void differingOutcomesWithNoRecordedReadsBlameSomethingOutsideTheCatalog() {
    final OutcomeReport report = OutcomeReport.of(List.of(
        new OutcomeReport.RunOutcome(1, 0, outcome("a"), List.of()),
        new OutcomeReport.RunOutcome(2, 0, outcome("b"), List.of())),
        REGISTRY, List.of(), 1);

    assertTrue(report.outcomesDiverged());
    assertTrue(report.render().contains("outside\nthe six catalog sources or was reached through reflection"),
        report.render());
  }

  @Test
  void aTruncatedRunIsNotedInTheReport() {
    final OutcomeReport report = OutcomeReport.of(List.of(
        new OutcomeReport.RunOutcome(1, 0, outcome("same"), List.of(read(0, "1"))),
        new OutcomeReport.RunOutcome(2, 0, outcome("same"), List.of(read(0, "1")))),
        REGISTRY, List.of(1), 1);

    assertTrue(report.render().contains("hit the event cap"), report.render());
  }

  @Test
  void multipleThreadsAddAnApproximateOrderingNote() {
    final OutcomeReport report = OutcomeReport.of(List.of(
        new OutcomeReport.RunOutcome(1, 0, outcome("same"), List.of(read(0, "1"))),
        new OutcomeReport.RunOutcome(2, 0, outcome("same"), List.of(read(0, "1")))),
        REGISTRY, List.of(), 3);

    assertTrue(report.render().contains("3 threads produced events"), report.render());
  }
}
