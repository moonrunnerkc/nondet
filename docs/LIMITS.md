# Limits and roadmap

Summary: what nondet does and does not catch in v0.2.0, each claim backed by how the tool actually
works or by a real run, and what is planned next.

## What it does not catch

**Outcome-stable is not a proof of determinism.** When the runs produce the same outcome, check says
there is no causal nondeterminism: the reads that happened did not change what the program produced.
That is a stronger, more honest statement than v0.1.0's "no divergence", but it is still about the
runs that happened. A read that only sometimes changes the outcome needs a run where it does before
check can attribute it.

**Attribution needs a failing run to exist.** `check --minimize` narrows the reads that differ
between a passing run and a failing run. If a workload never actually fails across the runs it is
given, there are no two outcomes to compare and nothing to minimize. nondet observes; it does not yet
vary the clock or seed to force a latent failure into the open. That is the first roadmap item.

**The catalog is six sources.** A cause outside `System.nanoTime`, `System.currentTimeMillis`,
`Math.random`, `UUID.randomUUID`, `System.getenv`, and `System.getProperty` is not recorded. When the
outcomes differ but no catalog read controls them, check says so plainly: the cause is outside the
six sources. This is common and real. `Instant.now()` reads a JDK-internal clock below
`System.currentTimeMillis`, and `java.util.Random` and `SecureRandom` are not `Math.random`, so a
workload built on those diverges without an attributed read.

**MethodHandle dispatch is not covered.** Reflection through `Method.invoke` is covered: the read is
recorded and attributed to the reflective caller. A `MethodHandle` is not. Its call sites are
signature-polymorphic and its target is not a recoverable member, so it cannot be rewritten the way a
plain call or `Method.invoke` can.

**Thread pinning pins reads, not everything between them.** Under `--pin`, replay serves recorded
values in their recorded global order, so a threaded run's reads happen in the recorded interleaving.
It does not pin what threads do between reads. A workload whose outcome depends only on the values
read, or on their order, reproduces; one whose outcome depends on un-pinned work between reads keeps
the approximate-order note.

**Static scan over-reports.** `scan` flags every catalog call site whether or not the value affects
what the program outputs, so a `System.nanoTime()` whose result is discarded is still listed. A
finding marks a possible source, not a proven cause; that is what `check` is for.

**Traces are bounded.** Each run records at most `--max-events` reads (default 1000000) and then
stops. A capped run is marked truncated, and the comparison covers only the recorded prefix.

## Why ENV and SYSPROP often show no divergence

The clock and random sources differ on their own in every fresh JVM, so check surfaces them. An
environment variable or system property is different: its value is read from outside the program and
is the same in every run launched the same way. So on one machine, check usually reports the run as
outcome-stable for these, even though the branch they feed is a real reproducibility risk across
machines. The bundled `ConfigGreeting` sample shows this honest stable case, and the evidence report
records it as outcome-stable (see [Evidence](EVIDENCE.md)). When the value genuinely does change
between invocations, as a deploy or job launcher would change it, check attributes it like any other
source.

## Roadmap

Deferred to a later phase, in rough priority order:

1. **Active entropy injection.** Vary the clock and seed the random generators between runs on
   purpose, so check can challenge an outcome-stable result and force a latent failing outcome into
   the open instead of waiting for the world to produce one.
2. **MethodHandle coverage**, so a catalog source reached through a method handle is recorded too.
3. **Catalog growth**, gated, since growth is the main scope risk: the `java.time` clocks behind
   `Instant.now`, `java.util.Random` and `SecureRandom`, identity hash codes, and hash-order.
4. **Dependency scanning**, to find entropy reads inside libraries the program pulls in without
   running them.
