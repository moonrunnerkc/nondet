package probe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A workload whose visible output is constant but whose declared result varies.
 *
 * <p>It prints the same line every run, so standard output, standard error, and the exit code
 * never differ. It then publishes a clock-derived value through the result channel by writing to
 * the file named by {@code nondet.result.out}. That makes the declared result the only part of the
 * outcome that changes between runs, which proves the result channel is fingerprinted: the checker
 * must still call the run outcome-divergent even though everything on the console agrees.
 */
public final class PublishedResultWorkload {

  private PublishedResultWorkload() {
  }

  /**
   * Prints a constant line and publishes a clock-derived declared result.
   *
   * @param args ignored
   * @throws IOException if the result file cannot be written
   */
  public static void main(String[] args) throws IOException {
    System.out.println("console-output-is-constant");
    final String resultPath = System.getProperty("nondet.result.out");
    if (resultPath != null) {
      Files.writeString(Path.of(resultPath), "result-" + System.nanoTime(), StandardCharsets.UTF_8);
    }
  }
}
