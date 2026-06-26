package io.github.moonrunnerkc.nondet.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.moonrunnerkc.nondet.catalog.CallSite;
import io.github.moonrunnerkc.nondet.catalog.Category;
import io.github.moonrunnerkc.nondet.catalog.Event;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DivergenceReportTest {

  private final Map<String, CallSite> registry = Map.of(
      "id-a", new CallSite("id-a", "pkg/Demo", "run", 13, "System.nanoTime"),
      "id-b", new CallSite("id-b", "pkg/Demo", "later", 27, "Math.random"));

  @Test
  void noneSaysTheRunsAgree() {
    final String text = DivergenceReport.of(Divergence.none(), registry).render();
    assertEquals("nondet check: no divergence; both runs read the same entropy in the same order\n", text);
  }

  @Test
  void mismatchResolvesTheCallSiteAndShowsBothValues() {
    final Event left = new Event(2, Category.TIME, "id-a", "100");
    final Event right = new Event(2, Category.TIME, "id-a", "215");
    final String text = DivergenceReport.of(Divergence.mismatch(2, left, right), registry).render();

    assertTrue(text.contains("first divergence at read #2"), text);
    assertTrue(text.contains("pkg.Demo.run line 13 (System.nanoTime)"), text);
    assertTrue(text.contains("read 100"), text);
    assertTrue(text.contains("read 215"), text);
  }

  @Test
  void lengthNamesTheLongerRun() {
    final Event extra = new Event(3, Category.RANDOM, "id-a", "0.5");
    final String text = DivergenceReport.of(Divergence.length(3, extra, null), registry).render();

    assertTrue(text.contains("diverge in length at read #3"), text);
    assertTrue(text.contains("run A read 0.5"), text);
    assertTrue(text.contains("run B had already ended"), text);
  }

  @Test
  void fallsBackToTheRawIdWhenTheCallSiteIsUnknown() {
    final Event left = new Event(0, Category.ENV, "missing", "x");
    final Event right = new Event(0, Category.ENV, "missing", "y");
    final String text = DivergenceReport.of(Divergence.mismatch(0, left, right), registry).render();

    assertTrue(text.contains("callSite missing (ENV)"), text);
  }

  @Test
  void rendersAnUnknownLineGracefullyWhenTheCallSiteHadNoLineTable() {
    final Map<String, CallSite> noLine = Map.of(
        "id-x", new CallSite("id-x", "pkg/Demo", "read", -1, "System.nanoTime"));
    final Event left = new Event(0, Category.TIME, "id-x", "100");
    final Event right = new Event(0, Category.TIME, "id-x", "200");
    final String text = DivergenceReport.of(Divergence.mismatch(0, left, right), noLine).render();

    assertTrue(text.contains("line unknown"), text);
    assertFalse(text.contains("line -1"), text);
  }

  @Test
  void multiRunReportListsTheOtherDivergingSites() {
    final Event left = new Event(0, Category.TIME, "id-a", "100");
    final Event right = new Event(0, Category.TIME, "id-a", "215");
    final String text = DivergenceReport
        .ofRuns(Divergence.mismatch(0, left, right), registry, List.of("id-b"), List.of())
        .render();

    assertTrue(text.contains("other call sites that differ in at least one run pairing"), text);
    assertTrue(text.contains("pkg.Demo.later line 27 (Math.random)"), text);
  }

  @Test
  void multiRunReportNotesWhichRunsWereTruncated() {
    final Event left = new Event(0, Category.TIME, "id-a", "100");
    final Event right = new Event(0, Category.TIME, "id-a", "215");
    final String text = DivergenceReport
        .ofRuns(Divergence.mismatch(0, left, right), registry, List.of(), List.of(2, 3))
        .render();

    assertTrue(text.contains("runs 2, 3 hit the event cap"), text);
    assertTrue(text.contains("limited to the recorded reads"), text);
  }

  @Test
  void multiRunReportNotesApproximateOrderingWhenSeveralThreadsRecorded() {
    final Event left = new Event(0, Category.TIME, "id-a", "100");
    final Event right = new Event(0, Category.TIME, "id-a", "215");
    final String text = DivergenceReport
        .ofRuns(Divergence.mismatch(0, left, right), registry, List.of(), List.of(), 4)
        .render();

    assertTrue(text.contains("4 threads produced events"), text);
    assertTrue(text.contains("cross-thread read ordering is approximate"), text);
  }

  @Test
  void aSingleThreadedReportHasNoThreadNote() {
    final Event left = new Event(0, Category.TIME, "id-a", "100");
    final Event right = new Event(0, Category.TIME, "id-a", "215");
    final String text = DivergenceReport
        .ofRuns(Divergence.mismatch(0, left, right), registry, List.of(), List.of(), 1)
        .render();

    assertFalse(text.contains("threads produced events"), text);
  }

  @Test
  void noReadsObservedIsDistinctFromAgreement() {
    final String text = DivergenceReport.noReadsObserved(3);
    assertTrue(text.contains("no entropy reads were observed across 3 runs"), text);
    assertTrue(text.contains("not a proof of determinism"), text);
    assertFalse(text.contains("no divergence"), "the no-reads message must not read as agreement");
  }

  @Test
  void multiRunReportWithNoExtrasMatchesThePlainReport() {
    final Event left = new Event(2, Category.TIME, "id-a", "100");
    final Event right = new Event(2, Category.TIME, "id-a", "215");
    final Divergence divergence = Divergence.mismatch(2, left, right);

    final String plain = DivergenceReport.of(divergence, registry).render();
    final String multi = DivergenceReport.ofRuns(divergence, registry, List.of(), List.of()).render();
    assertEquals(plain, multi, "no extra sites and no truncation must render identically to the plain report");
  }
}
