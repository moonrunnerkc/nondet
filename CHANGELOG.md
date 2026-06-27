# Changelog

All notable changes to nondet are recorded here. The format follows Keep a Changelog, and the
project uses semantic versioning.

## 0.2.0 - 2026-06-26

Causal attribution and deterministic repro. `check` no longer flags a read just because its raw
value varies; it reports the minimal set of reads that actually change what the program produced and
hands back a repro that reproduces the failure on demand. The catalog stays frozen at the same six
sources.

### Added

- Outcome capture: each run is fingerprinted over its exit code, stdout, stderr, and an optional
  declared result a workload publishes through `nondet.result.out` or the `nondet.result` property.
  `check` groups runs by that fingerprint, so a clock that varies but never reaches the result is
  reported as outcome-stable rather than as a divergence.
- Bundles and replay: a run is recorded as a bundle keyed by call site id and per-site sequence, and
  `nondet replay` re-runs it so every rewritten call site returns its recorded value instead of the
  live one. A bundle replays to a byte-identical outcome every time. `nondet replay --expect` makes a
  repro command self-verifying.
- `check --minimize`: when the outcomes differ, in-house delta-debugging narrows the differing reads
  to the minimal set that controls the outcome, names them with class, method, line, and api, and
  writes a repro bundle plus the exact `nondet replay` command that reproduces the failure.
- Reflection coverage: a catalog source reached through `Method.invoke` is recorded and attributed to
  the reflective caller, with the api marked reflective.
- Threaded replay: an optional `--pin` serves recorded values in their recorded global order, so a
  threaded run reproduces its interleaving; the default per-site serving stays for single-threaded and
  minimization replays.
- Evidence harness: a corpus runner that runs `check --minimize` over micro fixtures, the bundled
  examples, and a pinned real project, writing a measured report (`docs/EVIDENCE-REPORT.md`); a
  MicroBench regression gate in the suite that fails if a known cause stops being attributed.

### Changed

- A non-zero child exit is part of a run's outcome now, not a harness error. Only a timeout or a
  missing trace is reported as an execution error.
- `check` exit codes keep their meaning against outcomes: 0 when the runs agree on outcome or read
  nothing, 1 when their outcomes differ.

### Known limits

- A repro reproduces an outcome that the catalog can capture; a cause reached below the catalog, such
  as `Instant.now`, or through a `MethodHandle`, is reported as an unattributed divergence rather than
  pinned.
- Thread pinning pins order at entropy-read points, not arbitrary work between reads.

## 0.1.0 - 2026-06-25

First release. A JVM nondeterminism detector with two modes over one frozen entropy catalog.

### Added

- `nondet scan`: a static bytecode scanner that lists every call to a catalog entropy source,
  grouped by category and stably sorted.
- `nondet check`: a dynamic differential checker that runs a workload N times under an
  instrumentation agent and reports the first entropy read where the runs diverge, with its
  source location and any other call site that differs in some run pairing.
- Frozen six-source catalog: `System.nanoTime`, `System.currentTimeMillis`, `Math.random`,
  `UUID.randomUUID`, `System.getenv`, `System.getProperty`.
- Multi-run checking with `--runs` to widen recall beyond the two-run default.
- Per-run recorded-event cap via `--max-events` (default 1000000), with truncated runs
  reported so the divergence search is known to cover only a prefix.
- Robust failure handling: a per-run `--timeout` that kills and reports a hanging workload, a
  loud report on non-zero child exit or a missing trace, actionable one-line errors for a
  missing agent jar or bad inputs, and a distinct "no entropy reads observed" message that is
  not mistaken for a determinism proof.
- Multi-thread honesty: the agent records how many threads produced events, and the report
  notes that cross-thread ordering is approximate when more than one did.
- CLI ergonomics: `--help` with examples on every command, `--version` read from the jar
  manifest, `--debug` diagnostics to stderr, `--keep-traces` to retain trace files, and
  `--show-workload` to print captured child output.
- Documented exit codes: 0 no divergence or no reads, 1 divergence, 2 usage error, 3 execution
  or IO error.
- Executable shaded CLI jar (`nondet-cli.jar`) and a `./nondet` wrapper.
- Realistic samples under `examples`: `RetryWithTimeout`, `RandomShardRouter`, and
  `ConfigGreeting`, with captured runs in `docs/EXAMPLES.md`.

### Known limits

- A NONE result is not a determinism proof; raise `--runs` to increase confidence.
- Reflection and other dynamic dispatch are not instrumented.
- Static scan over-reports, flagging call sites whether or not the value is observed.
- Multi-thread attribution is approximate.
- Traces are capped at `--max-events`.
