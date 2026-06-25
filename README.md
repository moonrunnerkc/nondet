# nondet

A JVM nondeterminism detector: it finds the call site where a program's execution
stops being reproducible.

## Two modes

Both modes share one frozen entropy catalog.

- `nondet scan` reads compiled bytecode and lists every call to a known entropy
  source, grouped by category. It loads nothing and runs nothing.
- `nondet check` runs a workload twice under an instrumentation agent and reports
  the first entropy read where the two runs disagree.

Personal open source under github.com/moonrunnerkc. It is not an Aftermath
Technologies product.

## Entropy catalog

The v0.1.0 catalog is frozen at six sources:

| Source | Category |
| --- | --- |
| `System.nanoTime` | TIME |
| `System.currentTimeMillis` | TIME |
| `Math.random` | RANDOM |
| `UUID.randomUUID` | RANDOM |
| `System.getenv` | ENV |
| `System.getProperty` | SYSPROP |

The `Category` enum also declares `HASH_ORDER` and `IDENTITY_HASH`. Those are
reserved for a later phase and have no catalog entry yet.

## Build

Java 21 and Maven.

```
mvn -DskipTests package
mvn test
```

Two runnable jars come out of the build:

- `cli/target/nondet.jar`, the `nondet` command.
- `agent/target/nondet-agent.jar`, the agent. Its manifest carries `Premain-Class`
  and `Can-Retransform-Classes: true`.

## Run scan

```
java -jar cli/target/nondet.jar scan <path>
```

`<path>` is a classes directory, a jar, or a single `.class` file. Output is grouped
by category and sorted, so two scans of the same input are byte identical.

## Run check

```
java -jar cli/target/nondet.jar check \
  --class-path <workload classpath> \
  [--runs N] [--show-workload] \
  <fully.qualified.MainClass> [args...]
```

Add `--agent-jar <path>` to point at the agent jar, or omit it to use the newest
jar under `agent/target`. The command runs the main class N times, each in a fresh
JVM with the agent attached, then diffs every later run against the first. It
reports the earliest first-divergence, lists any other call site that differs in
some run pairing, and exits zero when the runs agree, one when they diverge, and
two when a run could not be completed.

`--runs N` sets the run count, default two, minimum two. More runs widen recall:
two runs can agree by chance on a value that only sometimes changes, and extra runs
make that miss less likely. Separate JVMs vary the clock, UUIDs, and process salts
on their own, so the runs differ without the tool controlling entropy. A
deterministic workload reports no divergence at any run count.

Child output is captured, not inherited, so the workload's own stdout never lands
in the report. A failing or trace-less run is reported with its captured output.
Pass `--show-workload` to print each run's captured output before the report.

Set `-Dnondet.max.events=N` on the `check` command to cap the reads each run
records, default 1000000; the cap is forwarded to every child JVM. A run that hits
the cap stops recording and is reported as truncated, with the divergence search
limited to the recorded prefix. The cap keeps an unbounded loop over an
instrumented source from growing an unbounded trace.

## Limits in v0.1.0

- Dynamic check compares the values read at each catalog call site, in order. If a
  nondeterministic source happens to return the same value in both runs, or the
  difference only surfaces in something the program computes downstream, check
  reports agreement. It pins divergence at the source, not every downstream effect.
- Static scan over-reports. It flags every catalog call site whether or not the
  value affects observable behavior, so a `System.nanoTime()` whose result is
  thrown away is still listed. A finding marks a possible source, not a proven bug.
- Only direct calls are instrumented. A catalog source reached through reflection
  (`Method.invoke`) or another layer of dynamic dispatch is not rewritten, so its
  read is invisible to check. This is a known blind spot: the rewrite matches a call
  site by its bytecode operands, and a reflective call carries none of them.
- Traces are bounded. Each run records at most `-Dnondet.max.events` reads (default
  1000000) and then stops, so an unbounded loop cannot exhaust memory. When a run is
  truncated the report says so, and the divergence search covers only the recorded
  prefix.
- Multi-threaded attribution is out of scope in v0.1.x. Events carry a global
  sequence, but the interleaving across threads is itself nondeterministic, so two
  runs of a threaded workload can differ in order with no real defect. Trust check
  only for single-threaded runs.

## Example output

Captured from a real run against the bundled `examples` module. The clock values in
the check report come from one actual run, so they change each time you run it.

`scan` over the compiled examples:

```
nondet scan: 3 entropy call sites

TIME (2)
  io.github.moonrunnerkc.nondet.examples.FlakyRetry.attempts  line 39  System.nanoTime
  io.github.moonrunnerkc.nondet.examples.FlakyRetry.attempts  line 41  System.nanoTime

RANDOM (1)
  io.github.moonrunnerkc.nondet.examples.HashOrder.firstKey  line 39  UUID.randomUUID
```

`check` on `FlakyRetry`. The workload's own stdout, the attempt count from each run, is
captured rather than printed, so the report stands alone. Both runs reach the same
count, but the clock they read to get there differs, which is the divergence check pins:

```
nondet check: first divergence at read #0

  run A: io.github.moonrunnerkc.nondet.examples.FlakyRetry.attempts line 39 (System.nanoTime)  read 7198345880532
  run B: io.github.moonrunnerkc.nondet.examples.FlakyRetry.attempts line 39 (System.nanoTime)  read 7198579015625

high confidence: this call site read different values across the two runs
```

The command exits one because the runs diverged. Pass `--show-workload` to print the
captured run output above the report.

## Wire formats

The agent and the cli exchange two plain UTF-8 files, one record per line, LF
terminated:

- trace line: `seq|category|callSiteId|value`
- registry line: `callSiteId|class|method|line|api`

Pipes, backslashes, and line breaks inside a field are escaped, so a record never
spans lines. The writer and reader share one codec, so the round trip is exact.

## Modules

- `catalog`: the entropy catalog, stable call site ids, and the shared wire types.
- `scan`: the read only bytecode scanner.
- `agent`: the instrumentation agent, runtime recorder, and trace writer.
- `cli`: the `nondet` command.
- `examples`: small programs with known entropy sources. `FlakyRetry` retries on
  `System.nanoTime` up to a fixed cap; `HashOrder` keys a map on `UUID.randomUUID`. The
  agent only skips its own runtime packages, so both examples are valid scan and check
  targets.

## Status

v0.1.0 is functional end to end. The call site rewrite
(`agent` `EntropyMethodVisitor.visitMethodInsn`) and the first divergence walk
(`cli` `Diff.first`) are both implemented, so `check` records entropy reads and reports
the first read where two runs disagree, with its source location.

## License

MIT. See `LICENSE`.
