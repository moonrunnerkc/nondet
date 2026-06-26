# How it works

Summary: the moving parts, from the catalog of sources to the final report, in plain terms.

## The catalog

Everything starts from one fixed list of six entropy sources (the clock twice, two random
generators, the environment, system properties). Both modes use the same list, so a call site
is attributed to the same source whether it is found by reading bytecode or by watching the
program run. The list is frozen in v0.1.0 on purpose; growing it is the main way the tool
could lose focus.

## scan: static matching

scan reads compiled `.class` bytes with ASM and walks every method call. When a call's owner,
name, and descriptor match a catalog source, it records the class, method, line, and which
source it was. Then it prints the findings grouped by category. It never loads or runs your
code, so it is safe to point at anything, but it cannot tell whether a read actually affects
the output. It reports possible sources, not proven bugs.

## check: dynamic differencing

check is where the agent comes in. The flow is:

1. **Rewrite.** A Java agent attaches to each child JVM and rewrites every catalog call site.
   In place of, say, `System.nanoTime()`, it calls a small hook that does the real call,
   records the value with a stable id for that call site, and returns the value unchanged. So
   the program behaves the same, but every entropy read is now logged.
2. **Run N times.** check launches your program in several fresh child JVMs. Separate JVMs
   already produce different clocks, UUIDs, and random values on their own, so the runs differ
   without nondet injecting anything.
3. **Diff.** Each run writes a trace of its reads in order. check compares every later run
   against the first, walking the reads in lockstep, and reports the first index where they
   disagree, resolved back to the source line.

The result names a single first point of departure, plus any other call site that differs in
some run pairing.

## The trace files

Each run writes two small UTF-8 text files, one record per line:

- a **trace**: `seq|category|callSiteId|value`, one line per read, in order.
- a **registry**: `callSiteId|class|method|line|api`, mapping each id back to a source line.

A trace can end with markers that are not reads: `#truncated` if the run hit the
`--max-events` cap, and `#threads N` for how many threads produced reads. Pipes, backslashes,
and newlines inside a value are escaped, so a record never spans lines and the round trip is
exact. `--keep-traces` keeps these files so you can read them yourself; every id in a trace
resolves to a registry line.

## The modules

The build is a small Maven reactor:

- `catalog`: the six sources, the stable call site ids, and the shared file format.
- `scan`: the read-only bytecode scanner.
- `agent`: the instrumentation agent, the runtime recorder, and the trace writer.
- `cli`: the `nondet` command (`scan` and `check`).
- `examples`: small programs with known entropy sources, used to exercise both modes.

The agent and recorder stay pure JDK, with no third-party types, so they load cleanly inside
any program under any class loader.
