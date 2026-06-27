# Usage

Summary: how to run `scan`, `check`, and `replay`, every flag with its default, and what each exit
code means. Run `nondet --help`, `nondet scan --help`, `nondet check --help`, or `nondet replay
--help` for the same reference at the terminal.

## nondet scan

```
nondet scan PATH
```

`PATH` is a classes directory, a jar, or a single `.class` file. scan lists every catalog call site,
grouped by category and sorted, so two scans of the same input produce identical text. It always
exits 0; it is a report, not a pass or fail gate.

## nondet check

```
nondet check [options] MAINCLASS [args...]
```

check runs `MAINCLASS` several times in fresh child JVMs with the agent attached, fingerprints what
each run produced, and reports whether the outcomes agree. When they differ, `--minimize` finds the
reads that cause it. Anything after `MAINCLASS` is passed through to your program.

| Flag | Default | What it does |
| --- | --- | --- |
| `-r`, `--runs N` | 2 | How many times to run; minimum 2. More runs raise the chance of seeing both a passing and a failing outcome. |
| `--minimize` | off | When the outcomes differ, find the minimal set of reads that cause it and write a repro bundle. |
| `--repro-out BUNDLE` | `nondet-repro.bundle` | Where to write the repro bundle under `--minimize`. |
| `-t`, `--timeout SECONDS` | 60 | Per-run time limit. A run that exceeds it is killed and reported, so a hanging program never hangs nondet. |
| `--max-events N` | 1000000 | Cap on recorded reads per run. Keeps a tight loop from growing an unbounded trace. |
| `--keep-traces DIR` | delete | Keep each run's trace, registry, and outcome files in `DIR` instead of deleting them. |
| `--show-workload` | off | Print each run's captured output before the report. |
| `-v`, `--debug` | off | Print diagnostics to stderr (agent jar, child command lines, per-run counts, outcomes) and turn on agent debug. The report on stdout stays clean. |
| `-a`, `--agent-jar PATH` | newest in `agent/target` | Path to `nondet-agent.jar`. |
| `-cp`, `--class-path CP` | none | Class path for your program, for example `examples/target/classes`. |
| `-h`, `--help` | | Show help with an example. |
| `-V`, `--version` | | Show the version, read from the jar manifest. |

Your program's own stdout and stderr are captured to files, not mixed into the report. A run that
times out or writes no trace is reported with that captured output so you can see what went wrong; a
non-zero exit is part of the outcome, not a failure of the harness.

## nondet replay

```
nondet replay [options] --bundle BUNDLE MAINCLASS [args...]
```

replay runs `MAINCLASS` once against a recorded bundle, so every rewritten call site returns its
recorded value instead of the live one, and prints the outcome it reproduced.

| Flag | Default | What it does |
| --- | --- | --- |
| `-b`, `--bundle BUNDLE` | required | The recorded bundle to replay. |
| `--expect FINGERPRINT` | none | Exit non-zero unless the replay reproduces this outcome, so the command verifies itself. |
| `--pin` | off | Pin threaded read order to the recorded global sequence while replaying. |
| `-t`, `--timeout SECONDS` | 60 | Wall-clock limit; a run that exceeds it is killed. |
| `--keep-traces DIR` | delete | Keep the replay's files in `DIR`. |
| `-a`, `--agent-jar PATH` | newest in `agent/target` | Path to `nondet-agent.jar`. |
| `-cp`, `--class-path CP` | none | Class path for your program. |
| `-v`, `--debug` | off | Print diagnostics to stderr. |

`check --minimize` prints a ready `nondet replay --expect ...` command for the failure it found.

## Exit codes

| Code | Meaning |
| --- | --- |
| 0 | The runs agree on outcome (or read nothing); for `replay`, the run completed and matched any `--expect` |
| 1 | The outcomes differ; for `replay`, an expected outcome was not reproduced |
| 2 | Usage error: a bad option value or a missing argument |
| 3 | Execution or IO error: a run timed out or wrote no trace, the agent jar was missing, or a trace could not be read |

Code 0 from `check` covers two outcomes the report words differently. "No causal nondeterminism"
means reads happened and every run produced the same outcome. "No entropy reads were observed" means
nothing in the catalog was read at all. Neither is a proof of determinism; see [Limits](LIMITS.md).

## Examples

Find the read that causes a divergence and write a repro:

```
nondet check --minimize --class-path build/classes com.example.Main
```

Reproduce a failure from the repro bundle, verifying it:

```
nondet replay --bundle nondet-repro.bundle --class-path build/classes \
  --expect <fingerprint> com.example.Main
```

Keep the traces and bundles to inspect them by hand:

```
nondet check --keep-traces ./traces --class-path build/classes com.example.Main
```
