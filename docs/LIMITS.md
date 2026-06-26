# Limits and roadmap

Summary: what nondet does not catch in v0.1.0, each claim backed by how the tool actually
works or by a real run, and what is planned next.

## What it does not catch

**A "no divergence" result is not a proof.** It says the runs agreed on the reads that
happened. Two runs can agree by chance on a value that only sometimes changes. Raising
`--runs` lowers that chance but cannot prove a negative.

**Reflection and other dynamic dispatch are invisible.** The agent rewrites a call site by
matching its bytecode operands. A catalog source reached through `Method.invoke` or a similar
layer carries none of those operands, so it is never rewritten and its read is never recorded.

**Static scan over-reports.** It flags every catalog call site whether or not the value
affects what the program outputs, so a `System.nanoTime()` whose result is discarded is still
listed. A finding marks a possible source, not a proven bug.

**Multi-thread attribution is approximate.** Reads carry a global sequence number, but the
order threads interleave is itself nondeterministic, so two runs of a threaded program can
differ in order with no real defect. When more than one thread produced reads, the report says
so rather than pretending the order is exact.

**Traces are bounded.** Each run records at most `--max-events` reads (default 1000000) and
then stops, so a runaway loop cannot exhaust memory. A capped run is marked truncated, and the
search covers only the recorded prefix.

## Why ENV and SYSPROP often show no divergence

The clock and random sources differ on their own in every fresh JVM, so check surfaces them
easily. An environment variable or system property is different: its value is read from
outside the program and is the same in every run launched the same way. So on one machine,
check usually reports no divergence for these, even though the branch they feed is a real
reproducibility risk across machines. The bundled `ConfigGreeting` sample shows this honest
no-divergence case (see [Examples](EXAMPLES.md)).

When the value genuinely does change between invocations, check pins it like any other source.
The following is a real run against a small workload that reads only `System.getenv`, launched
by a wrapper that sets the variable to a different value each time, which is what a deploy or
job launcher does in practice:

```
nondet check: first divergence at read #0

  run A: WildEnv.main line 4 (System.getenv)  read tok-17284-18550
  run B: WildEnv.main line 4 (System.getenv)  read tok-31147-26602

high confidence: this call site read different values across the two runs
```

So the ENV path works end to end. The gap is only that passive observation on one machine
cannot make the value vary by itself, which is the motivation for the first roadmap item.

## Roadmap

Deferred to a later phase, in rough priority order:

1. **Active entropy injection.** Vary the clock and seed the random generators between runs on
   purpose, and perturb environment and property reads, so check can challenge a no-divergence
   result and expose a latent branch instead of waiting for the world to change it. This is the
   next capability.
2. **Thread-scheduling attribution**, so cross-thread divergence can be pinned rather than only
   flagged as approximate.
3. **Reflection and dynamic-dispatch coverage.**
4. **Catalog growth** (`java.time`, more random sources, identity hash codes, hash-order). Each
   addition is gated, since growth is the main scope risk.
5. **Dependency scanning**, to find entropy reads inside libraries the program pulls in.
