# Usage

Summary: how to run `scan` and `check`, every flag with its default, and what each exit code
means. Run `nondet --help`, `nondet scan --help`, or `nondet check --help` for the same
reference at the terminal.

## nondet scan

```
nondet scan PATH
```

`PATH` is a classes directory, a jar, or a single `.class` file. scan lists every catalog
call site, grouped by category and sorted, so two scans of the same input produce identical
text. It always exits 0; it is a report, not a pass or fail gate.

## nondet check

```
nondet check [options] MAINCLASS [args...]
```

check runs `MAINCLASS` several times in fresh child JVMs with the agent attached, records
every entropy read, and reports the first read where two runs disagree. Anything after
`MAINCLASS` is passed through to your program.

| Flag | Default | What it does |
| --- | --- | --- |
| `-r`, `--runs N` | 2 | How many times to run; minimum 2. More runs catch a value that only sometimes changes. |
| `-t`, `--timeout SECONDS` | 60 | Per-run time limit. A run that exceeds it is killed and reported, so a hanging program never hangs nondet. |
| `--max-events N` | 1000000 | Cap on recorded reads per run. Keeps a tight loop from growing an unbounded trace. |
| `--keep-traces DIR` | delete | Keep each run's trace and registry files in `DIR` instead of deleting them. |
| `--show-workload` | off | Print each run's captured output before the report. |
| `-v`, `--debug` | off | Print diagnostics to stderr (agent jar, child command lines, per-run event, thread, and instrumented-class counts) and turn on agent debug. The report on stdout stays clean. |
| `-a`, `--agent-jar PATH` | newest in `agent/target` | Path to `nondet-agent.jar`. |
| `-cp`, `--class-path CP` | none | Class path for your program, for example `examples/target/classes`. |
| `-h`, `--help` | | Show help with an example. |
| `-V`, `--version` | | Show the version, read from the jar manifest. |

Your program's own stdout and stderr are captured to a file, not mixed into the report. A run
that fails, times out, or writes no trace is reported with that captured output so you can see
what went wrong.

## Exit codes

| Code | Meaning |
| --- | --- |
| 0 | No divergence, or no entropy reads were observed |
| 1 | A divergence was found |
| 2 | Usage error: a bad option value or a missing argument |
| 3 | Execution or IO error: a run failed, timed out, the agent jar was missing, or a trace could not be read |

Code 0 covers two different outcomes that the report words differently. "No divergence" means
reads happened and every run agreed. "No entropy reads were observed" means nothing in the
catalog was read, so there was nothing to compare. Neither is a proof of determinism; see
[Limits](LIMITS.md).

## Examples

Three runs instead of two:

```
nondet check --runs 3 --class-path build/classes com.example.Main
```

Keep the traces to inspect them by hand:

```
nondet check --keep-traces ./traces --class-path build/classes com.example.Main
```

See the child command lines and per-run counts while debugging a setup:

```
nondet check --debug --class-path build/classes com.example.Main
```
