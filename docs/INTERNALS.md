# How it works

Summary: the moving parts, from the catalog of sources to the minimal cause and the repro, in plain
terms.

## The catalog

Everything starts from one fixed list of six entropy sources (the clock twice, two random
generators, the environment, system properties). Both modes use the same list, so a call site is
attributed to the same source whether it is found by reading bytecode or by watching the program run.
The list is frozen on purpose; v0.2.0 buys its value from causal logic, not from more sources.

## scan: static matching

scan reads compiled `.class` bytes with ASM and walks every method call. When a call's owner, name,
and descriptor match a catalog source, it records the class, method, line, and which source it was,
then prints the findings grouped by category. It never loads or runs your code, so it is safe to
point at anything, but it cannot tell whether a read actually affects the output. It reports possible
sources, not proven causes.

## check: outcomes, not raw reads

check is where the agent comes in. A Java agent attaches to each child JVM and rewrites every catalog
call site: in place of, say, `System.nanoTime()`, it calls a small hook that does the real call,
records the value with a stable id for that call site, and returns the value unchanged. So the
program behaves the same, but every entropy read is now logged.

The pivot of v0.2.0 is what counts as nondeterminism. check runs your program several times in fresh
child JVMs and fingerprints what each run *produced*, not just what it read. The fingerprint is a hash
over the run's result surface: its exit code, standard output, standard error, and an optional result
the workload publishes. check groups the runs by that fingerprint. If every run produced the same
outcome, there is no causal nondeterminism, even when the clocks behind the runs differed, and check
says so. Only when the outcomes differ is there something a read could have caused.

## Outcomes and the result channel

The outcome is a SHA-256 over the result surface in a fixed, length-framed byte order, so two streams
never run together at their boundary. A workload that wants to declare an explicit result, separate
from its console output, writes it to the file named by `nondet.result.out` or sets the
`nondet.result` property; the agent folds that into the fingerprint. Exit code, stdout, and stderr are
always part of it; the declared result only when present.

## Bundles and replay

A trace is a log of reads in order. A bundle is the same reads turned into a lookup table, keyed by
call site id and per-site sequence, so a replay can answer "what did the n-th read at this site
return?" In replay mode the agent loads a bundle and each rewritten call site serves its recorded
value instead of the live JDK one. A bundle replays to a byte-identical outcome every time, which is
the primitive everything else stands on. `nondet replay` runs a bundle on its own, and with `--expect`
it exits non-zero unless it reproduces a named outcome, so a repro command verifies itself.

## Minimizing the cause

When the outcomes differ, `check --minimize` finds the smallest set of reads that controls them. It
takes a passing run and a failing run, builds a bundle from each, and runs delta-debugging: it forces
a chosen subset of the reads that differ to their failing values, frees the rest to their passing
values, replays, and asks whether the failing outcome came back. Every trial is a deterministic
replay, so the search is reproducible and its result is one-minimal: removing any one read from it no
longer reproduces. The output names those reads with class, method, line, and api, and writes a repro
bundle plus the exact `nondet replay` command.

## Reflection and threads

The static rewrite reads a target off the bytecode operands, which a reflective call does not carry.
So check also rewrites `Method.invoke` call sites: the hook resolves the real target at run time and,
when it is a catalog source, records it and attributes it to the reflective caller with the api marked
reflective. Under replay, `--pin` serves recorded values in their recorded global order, so a threaded
run reproduces its interleaving; without it, replay serves each site's values in order but lets the
threads reach the reads in any order, which is the right default for a single-threaded run or a
minimization trial.

## The files

Each run writes small UTF-8 text files, one record per line:

- a **trace**: `seq|category|callSiteId|value`, one line per read, in order.
- a **registry**: `callSiteId|class|method|line|api`, mapping each id back to a source line.
- an **outcome**: `fingerprint|components`, the run's result fingerprint.
- a **bundle** (`#nondet-bundle 1` header, then `globalSeq|callSiteId|perSiteSeq|category|value`), the
  replay recipe.

A trace can end with markers that are not reads: `#truncated` if the run hit the `--max-events` cap,
and `#threads N` for how many threads produced reads. Pipes, backslashes, and newlines inside a value
are escaped, so a record never spans lines and the round trip is exact.

## The modules

The build is a small Maven reactor:

- `catalog`: the six sources, the stable call site ids, outcomes, bundles, and the shared file format.
- `scan`: the read-only bytecode scanner.
- `agent`: the instrumentation agent, the runtime recorder, replay, and the trace writer.
- `cli`: the `nondet` command (`scan`, `check`, `replay`), the causal search, and the reports.
- `examples`: small programs with known entropy sources, used to exercise both modes.
- `evidence`: the corpus runner and micro fixtures behind the evidence report.

The agent, recorder, replay, and pin stay pure JDK, with no third-party types, so they load cleanly
inside any program under any class loader.
