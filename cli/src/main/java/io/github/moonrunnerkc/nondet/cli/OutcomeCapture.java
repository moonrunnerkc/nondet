package io.github.moonrunnerkc.nondet.cli;

import io.github.moonrunnerkc.nondet.catalog.Outcome;
import io.github.moonrunnerkc.nondet.catalog.OutcomeComponent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

/**
 * Fingerprints what a run produced and persists that fingerprint beside the trace.
 *
 * <p>The result surface is the run's exit code, its standard output, its standard error, and a
 * declared result the workload may have published to its result file. Reading those bytes and
 * folding them into a {@link Outcome} is all this class does; the hashing itself lives in
 * {@link Outcome} so it stays pure and testable. The persisted file lets {@code check} keep an
 * outcome next to a trace, so a later replay or a kept-traces directory can be read back without
 * re-running the workload.
 */
final class OutcomeCapture {

  private static final char FIELD_SEPARATOR = '|';
  private static final char COMPONENT_SEPARATOR = ',';

  private OutcomeCapture() {
  }

  /**
   * Fingerprints a finished run from its captured files.
   *
   * <p>Missing standard-output or standard-error files are read as empty, since a process that
   * printed nothing leaves an empty stream, not a missing one. A missing result file means the
   * workload published no declared result, so that component is left out of the fingerprint.
   *
   * @param run a finished run, never {@code null}
   * @return the outcome fingerprint over the run's result surface
   * @throws IOException if a captured file exists but cannot be read
   */
  static Outcome capture(RunResult run) throws IOException {
    final byte[] stdout = readOrEmpty(run.stdout());
    final byte[] stderr = readOrEmpty(run.stderr());
    final byte[] declaredResult = Files.isRegularFile(run.result())
        ? Files.readAllBytes(run.result())
        : null;
    return Outcome.of(run.exitCode(), stdout, stderr, declaredResult);
  }

  /**
   * Writes an outcome to a file as a single {@code fingerprint|components} line.
   *
   * @param path    the destination file; its parent directory must already exist
   * @param outcome the outcome to persist, never {@code null}
   * @throws IOException if the file cannot be written
   */
  static void write(Path path, Outcome outcome) throws IOException {
    final StringBuilder line = new StringBuilder(outcome.fingerprint());
    line.append(FIELD_SEPARATOR);
    boolean first = true;
    for (final OutcomeComponent component : outcome.components()) {
      if (!first) {
        line.append(COMPONENT_SEPARATOR);
      }
      line.append(component.name());
      first = false;
    }
    line.append('\n');
    Files.writeString(path, line, StandardCharsets.UTF_8);
  }

  /**
   * Reads an outcome file written by {@link #write}.
   *
   * @param path the outcome file to read, never {@code null}
   * @return the persisted outcome
   * @throws IOException if the file cannot be read
   * @throws IllegalArgumentException if the line is not a {@code fingerprint|components} record
   */
  static Outcome read(Path path) throws IOException {
    final String line = Files.readString(path, StandardCharsets.UTF_8).strip();
    final int sep = line.indexOf(FIELD_SEPARATOR);
    if (sep < 0) {
      throw new IllegalArgumentException(
          "outcome line must be 'fingerprint|components' but had no separator: " + line);
    }
    final String fingerprint = line.substring(0, sep);
    final EnumSet<OutcomeComponent> components = EnumSet.noneOf(OutcomeComponent.class);
    for (final String name : line.substring(sep + 1).split(String.valueOf(COMPONENT_SEPARATOR))) {
      if (!name.isBlank()) {
        components.add(OutcomeComponent.valueOf(name));
      }
    }
    return new Outcome(fingerprint, components);
  }

  private static byte[] readOrEmpty(Path path) throws IOException {
    return Files.isRegularFile(path) ? Files.readAllBytes(path) : new byte[0];
  }
}
