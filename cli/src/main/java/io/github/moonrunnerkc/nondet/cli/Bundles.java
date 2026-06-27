package io.github.moonrunnerkc.nondet.cli;

import io.github.moonrunnerkc.nondet.catalog.Bundle;
import io.github.moonrunnerkc.nondet.catalog.BundleIO;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Turns a recorded run into a replayable bundle on disk.
 *
 * <p>A trace is a log of reads in order; a bundle is the same reads keyed so a replay can serve
 * them back. This reads a run's trace, derives the bundle, and writes it, so the checker and the
 * causal search can record a run once and replay it many times.
 */
final class Bundles {

  private Bundles() {
  }

  /**
   * Reads a run's trace and writes the bundle that replays it.
   *
   * @param trace     the trace file the run wrote, never {@code null}
   * @param bundleOut the bundle file to write, never {@code null}
   * @return the bundle that was written
   * @throws IOException if the trace cannot be read or the bundle cannot be written
   */
  static Bundle writeFromTrace(Path trace, Path bundleOut) throws IOException {
    final Bundle bundle = Bundle.fromEvents(TraceReader.readTrace(trace));
    BundleIO.write(bundleOut, bundle);
    return bundle;
  }
}
