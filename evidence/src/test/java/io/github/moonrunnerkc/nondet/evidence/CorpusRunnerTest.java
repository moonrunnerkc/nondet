package io.github.moonrunnerkc.nondet.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CorpusRunnerTest {

  private static final CorpusTarget TARGET =
      new CorpusTarget("Demo", "bundled", "cp", "Demo", List.of());

  private static final String ATTRIBUTED_OUTPUT = """
      nondet check: the runs produced 2 different outcomes

        outcome A  fingerprint aaaaaaaaaaaa  exit 0  run 1
        outcome B  fingerprint bbbbbbbbbbbb  exit 0  run 2

      the earliest entropy read that differs between outcome A (run 1) and outcome B (run 2) is the leading candidate cause:

        Demo.run line 5 (System.nanoTime)
          run 1 read 1
          run 2 read 2

      one or more of the reads that differ between these runs controls the outcome.

      nondet check: minimal causal set: 1 read controls the outcome

        Demo.run line 5 (System.nanoTime)

      forced to their failing values these reproduce outcome bbbbbbbbbbbb.

      repro bundle: /tmp/demo-repro.bundle
      reproduce with:
        nondet replay --bundle /tmp/demo-repro.bundle --class-path cp --expect bbbb Demo
      """;

  @Test
  void anAttributedDivergenceIsClassifiedWithItsReadAndReproCommand() {
    final CheckResult result = CorpusRunner.classify(TARGET, 1, ATTRIBUTED_OUTPUT);

    assertEquals(CheckResult.Verdict.CAUSE_ATTRIBUTED, result.verdict());
    assertEquals(List.of("Demo.run line 5 (System.nanoTime)"), result.causalReads(),
        "the minimal causal set is lifted, not the candidate block before it");
    assertEquals("nondet replay --bundle /tmp/demo-repro.bundle --class-path cp --expect bbbb Demo",
        result.reproCommand());
  }

  @Test
  void aZeroExitIsAStableOutcome() {
    final CheckResult result = CorpusRunner.classify(TARGET, 0,
        "nondet check: no causal nondeterminism across 2 runs\n");
    assertEquals(CheckResult.Verdict.OUTCOME_STABLE, result.verdict());
    assertTrue(result.causalReads().isEmpty());
  }

  @Test
  void aDivergenceWithNoCatalogReadIsUnattributed() {
    final CheckResult result = CorpusRunner.classify(TARGET, 1,
        "nondet check: the runs produced 2 different outcomes\n\n"
            + "the outcomes differ, but no recorded entropy read among the candidates\ncontrols them;"
            + " the cause is outside the six catalog sources or was reached through reflection.\n");
    assertEquals(CheckResult.Verdict.DIVERGED_UNATTRIBUTED, result.verdict());
  }

  @Test
  void anExecutionErrorIsRecordedAsNotMeasured() {
    final CheckResult result = CorpusRunner.classify(TARGET, 3, "nondet check: run 1 produced no trace\n");
    assertEquals(CheckResult.Verdict.COULD_NOT_RUN, result.verdict());
  }

  @Test
  void aCorpusLineParsesIntoATargetWithArgs() {
    final CorpusTarget parsed = CorpusTarget.parse("Name\trepo @ abc123\tbuild/classes\tcom.example.Main\tone\ttwo");
    assertEquals("Name", parsed.name());
    assertEquals("repo @ abc123", parsed.provenance());
    assertEquals("com.example.Main", parsed.mainClass());
    assertEquals(List.of("one", "two"), parsed.args());
  }

  @Test
  void theReportContainsARowPerTargetAndTheReproForAnAttributedCause() {
    final CheckResult attributed = CorpusRunner.classify(TARGET, 1, ATTRIBUTED_OUTPUT);
    final CheckResult stable = new CheckResult(
        new CorpusTarget("Stable", "bundled", "cp", "Stable", List.of()),
        CheckResult.Verdict.OUTCOME_STABLE, List.of(), null);

    final String report = EvidenceReport.render(List.of(attributed, stable), 2);

    assertTrue(report.contains("| Demo | bundled | cause attributed |"), report);
    assertTrue(report.contains("| Stable | bundled | outcome stable |"), report);
    assertTrue(report.contains("nondet replay --bundle /tmp/demo-repro.bundle"), report);
    assertTrue(report.contains("no-false-positive"), "the report explains the stable case honestly");
  }
}
