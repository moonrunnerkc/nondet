package io.github.moonrunnerkc.nondet.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Renders a child run's captured standard output and standard error for the report.
 *
 * <p>A run's two streams are captured to separate files so each can be fingerprinted on its own.
 * When the checker needs to show what a run printed, for a failure diagnosis or under
 * {@code --show-workload}, it folds those files back into one block here. A stream whose file is
 * missing reads as empty rather than as an error, since a run that printed nothing leaves no file.
 */
final class CapturedOutput {

  private CapturedOutput() {
  }

  /**
   * Joins a run's captured stdout and stderr into a single block.
   *
   * @param result the finished run whose streams to read, never {@code null}
   * @return the combined output, stdout first, with a newline inserted before stderr when stdout
   *     did not already end in one; empty when the run printed nothing
   */
  static String streams(RunResult result) {
    final String out = read(result.stdout());
    final String err = read(result.stderr());
    final StringBuilder combined = new StringBuilder(out);
    if (!err.isEmpty()) {
      if (combined.length() > 0 && combined.charAt(combined.length() - 1) != '\n') {
        combined.append('\n');
      }
      combined.append(err);
    }
    return combined.toString();
  }

  /**
   * Builds the trailing block appended to a failure message.
   *
   * @param result the failed run, never {@code null}
   * @return a sentence noting the run produced no output, or the captured output under a heading
   */
  static String failureBlock(RunResult result) {
    final String output = streams(result);
    if (output.isBlank()) {
      return ". The run produced no output.";
    }
    return ". Captured child output:\n" + output;
  }

  private static String read(Path captured) {
    try {
      return Files.exists(captured) ? Files.readString(captured, StandardCharsets.UTF_8) : "";
    } catch (final IOException cause) {
      return "(could not read captured output at " + captured + ": " + cause.getMessage() + ")";
    }
  }
}
