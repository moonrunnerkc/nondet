package io.github.moonrunnerkc.nondet.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.moonrunnerkc.nondet.catalog.Outcome;
import io.github.moonrunnerkc.nondet.catalog.OutcomeComponent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OutcomeCaptureTest {

  @Test
  void capturesIdenticalOutputAsTheSameFingerprint(@TempDir Path dir) throws IOException {
    final Outcome first = OutcomeCapture.capture(run(dir, 1, "done\n", "", null, 0));
    final Outcome second = OutcomeCapture.capture(run(dir, 2, "done\n", "", null, 0));
    assertTrue(first.agreesWith(second));
  }

  @Test
  void aMissingStreamIsReadAsEmptyNotAsAnError(@TempDir Path dir) throws IOException {
    final RunResult run = new RunResult(RunResult.Status.SUCCESS, 1,
        dir.resolve("x.trace"), dir.resolve("x.registry"),
        dir.resolve("absent.out"), dir.resolve("absent.err"), dir.resolve("absent.result"),
        dir.resolve("x.outcome"), 0);
    final Outcome empty = OutcomeCapture.capture(run);
    assertEquals(Outcome.of(0, new byte[0], new byte[0], null).fingerprint(), empty.fingerprint());
  }

  @Test
  void aPublishedResultIsFoldedIntoTheFingerprint(@TempDir Path dir) throws IOException {
    final Outcome withoutResult = OutcomeCapture.capture(run(dir, 1, "same\n", "", null, 0));
    final Outcome withResult = OutcomeCapture.capture(run(dir, 2, "same\n", "", "published", 0));
    assertFalse(withoutResult.agreesWith(withResult));
    assertTrue(withResult.components().contains(OutcomeComponent.DECLARED_RESULT));
  }

  @Test
  void anOutcomeRoundTripsThroughItsFile(@TempDir Path dir) throws IOException {
    final Outcome outcome = OutcomeCapture.capture(run(dir, 1, "out\n", "err\n", "res", 1));
    final Path file = dir.resolve("run1.outcome");
    OutcomeCapture.write(file, outcome);
    final Outcome reloaded = OutcomeCapture.read(file);
    assertEquals(outcome.fingerprint(), reloaded.fingerprint());
    assertEquals(outcome.components(), reloaded.components());
  }

  private static RunResult run(Path dir, int index, String stdout, String stderr, String result,
      int exitCode) throws IOException {
    final Path out = dir.resolve("run" + index + ".out");
    final Path err = dir.resolve("run" + index + ".err");
    final Path res = dir.resolve("run" + index + ".result");
    Files.writeString(out, stdout, StandardCharsets.UTF_8);
    Files.writeString(err, stderr, StandardCharsets.UTF_8);
    if (result != null) {
      Files.writeString(res, result, StandardCharsets.UTF_8);
    }
    return new RunResult(RunResult.Status.SUCCESS, index, dir.resolve("run" + index + ".trace"),
        dir.resolve("run" + index + ".registry"), out, err, res,
        dir.resolve("run" + index + ".outcome"), exitCode);
  }
}
