package io.github.moonrunnerkc.nondet.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.moonrunnerkc.nondet.catalog.Bundle;
import io.github.moonrunnerkc.nondet.catalog.BundleEntry;
import io.github.moonrunnerkc.nondet.catalog.Category;
import io.github.moonrunnerkc.nondet.catalog.Event;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The v0.2.0 goal proved on fixtures with a known cause: name the minimal reads that control a
 * failure and hand back a bundle that reproduces it.
 *
 * <p>For each fixture a run is recorded once to learn its call site ids, then a passing and a
 * failing context are synthesised by forcing the controlling read or reads to even or odd values.
 * The search then runs entirely on deterministic replays, so the minimal set and the repro are
 * exact. These need the packaged agent jar and the compiled probes, so each is skipped when the
 * agent jar is missing.
 */
class CausalAttributionAcceptanceTest {

  @Test
  void oneReadControllingABranchIsAttributedAndItsReproReproduces(@TempDir Path workDir)
      throws Exception {
    assumeTrue(agentJar().isPresent(), "agent jar not built; run mvn -pl agent package first");
    final WorkloadRunner runner = runner(workDir, "probe.SingleCauseWorkload");
    final List<String> sites = timeSiteIds(runner.run(1).trace());
    assertEquals(1, sites.size(), "the fixture reads the clock exactly once");
    final String site = sites.get(0);

    final Bundle passing = timeBundle(List.of(entry(0, site, 0, "100")));
    final Bundle failing = timeBundle(List.of(entry(0, site, 0, "101")));
    final Bisect bisect = new Bisect(runner, workDir, passing, failing);
    final Bisect.Result result = bisect.search(workDir.resolve("repro.bundle"));

    assertEquals(List.of(new Bundle.Key(site, 0)), result.causalKeys(),
        "exactly the one controlling read is returned");
    final RunResult fresh = runner.run(99, result.reproBundle());
    assertEquals(result.failingFingerprint(), OutcomeCapture.capture(fresh).fingerprint(),
        "the repro bundle reproduces the failing outcome on a fresh process");
  }

  @Test
  void twoReadsThatJointlyControlTheOutcomeAreBothReturned(@TempDir Path workDir) throws Exception {
    assumeTrue(agentJar().isPresent(), "agent jar not built; run mvn -pl agent package first");
    final WorkloadRunner runner = runner(workDir, "probe.TwoCauseWorkload");
    final List<String> sites = timeSiteIds(runner.run(1).trace());
    assertEquals(2, sites.size(), "the fixture reads the clock twice");
    final Bundle.Key first = new Bundle.Key(sites.get(0), 0);
    final Bundle.Key second = new Bundle.Key(sites.get(1), 0);

    final Bundle passing = timeBundle(List.of(
        entry(0, sites.get(0), 0, "100"), entry(1, sites.get(1), 0, "200")));
    final Bundle failing = timeBundle(List.of(
        entry(0, sites.get(0), 0, "101"), entry(1, sites.get(1), 0, "201")));
    final Bisect bisect = new Bisect(runner, workDir, passing, failing);
    final Bisect.Result result = bisect.search(workDir.resolve("repro.bundle"));

    assertEquals(2, result.causalKeys().size(), "both reads are needed: " + result.causalKeys());
    assertTrue(result.causalKeys().containsAll(List.of(first, second)), result.causalKeys().toString());
    assertFalse(bisect.reproduces(Set.of(first)), "forcing only the first read must not reproduce");
    assertFalse(bisect.reproduces(Set.of(second)), "forcing only the second read must not reproduce");
    assertTrue(bisect.reproduces(Set.of(first, second)), "forcing both reads reproduces");

    final RunResult fresh = runner.run(99, result.reproBundle());
    assertEquals(result.failingFingerprint(), OutcomeCapture.capture(fresh).fingerprint(),
        "the repro bundle reproduces the joint failure on a fresh process");
  }

  private static List<String> timeSiteIds(Path trace) throws Exception {
    final List<String> ids = new ArrayList<>();
    for (final Event event : TraceReader.readTrace(trace)) {
      if (event.category() == Category.TIME) {
        ids.add(event.callSiteId());
      }
    }
    return ids;
  }

  private static BundleEntry entry(long globalSeq, String site, int perSiteSeq, String value) {
    return new BundleEntry(globalSeq, site, perSiteSeq, Category.TIME, value);
  }

  private static Bundle timeBundle(List<BundleEntry> entries) {
    return new Bundle(entries);
  }

  private static WorkloadRunner runner(Path workDir, String mainClass) {
    return new WorkloadRunner(agentJar().orElseThrow(), workDir, testClasses().toString(),
        mainClass, List.of(), 60, 0, false);
  }

  private static Optional<Path> agentJar() {
    try {
      return AgentJarLocator.newestJar(repoRoot().resolve("agent").resolve("target"));
    } catch (final Exception cause) {
      return Optional.empty();
    }
  }

  private static Path testClasses() {
    return Paths.get("").toAbsolutePath().resolve("target").resolve("test-classes");
  }

  private static Path repoRoot() {
    return Paths.get("").toAbsolutePath().getParent();
  }
}
