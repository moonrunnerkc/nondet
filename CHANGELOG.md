# Changelog

All notable changes to nondet are recorded here. The format follows Keep a Changelog, and the
project uses semantic versioning.

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
