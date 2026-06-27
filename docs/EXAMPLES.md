# Worked examples

Real, captured runs of `nondet` against the bundled `examples` module. Nothing here is invented; each
block is copied from an actual run on a single Linux box with Temurin JDK 21. The clock, random, and
UUID values differ every run, and so do the outcome fingerprints and the minimal set for a given
failure, so yours will not match digit for digit. The shape of the report, the attributed source, and
the exit code are what to compare against.

Build first so the agent jar and the compiled samples exist:

```
mvn -DskipTests package
```

Then point the CLI at the compiled samples. The commands below use the `java -jar` form; the
`./nondet` wrapper takes the same arguments.

## scan: list every entropy call site

```
$ java -jar cli/target/nondet-cli.jar scan examples/target/classes
nondet scan: 7 entropy call sites

TIME (4)
  io.github.moonrunnerkc.nondet.examples.FlakyRetry.attempts  line 39  System.nanoTime
  io.github.moonrunnerkc.nondet.examples.FlakyRetry.attempts  line 41  System.nanoTime
  io.github.moonrunnerkc.nondet.examples.samples.RetryWithTimeout.attempts  line 43  System.currentTimeMillis
  io.github.moonrunnerkc.nondet.examples.samples.RetryWithTimeout.attempts  line 45  System.currentTimeMillis

RANDOM (2)
  io.github.moonrunnerkc.nondet.examples.HashOrder.firstKey  line 39  UUID.randomUUID
  io.github.moonrunnerkc.nondet.examples.samples.RandomShardRouter.main  line 28  Math.random

SYSPROP (1)
  io.github.moonrunnerkc.nondet.examples.samples.ConfigGreeting.main  line 30  System.getProperty
```

## check FlakyRetry: a clock read that does not change the result

FlakyRetry checks the clock on every attempt, so the recorded values differ run to run, but it always
completes the same number of attempts and prints the same line. The clock varies; the outcome does
not. check says so, and exits 0. This is the case v0.1.0 used to flag as a divergence.

```
$ java -jar cli/target/nondet-cli.jar check \
    --class-path examples/target/classes \
    io.github.moonrunnerkc.nondet.examples.FlakyRetry
nondet check: no causal nondeterminism across 2 runs

the runs read different entropy values along the way, but every run produced the same
outcome (fingerprint 1f98c33deefd), so none of those reads controlled it.
```

## check --minimize HashOrder: pin the cause and get a repro (RANDOM)

HashOrder's first key depends on a `UUID.randomUUID` draw, so the runs differ. With `--minimize`,
check narrows the differing reads to the one that controls the outcome and writes a repro bundle plus
a verified replay command. Exit code 1.

```
$ java -jar cli/target/nondet-cli.jar check --minimize \
    --class-path examples/target/classes \
    io.github.moonrunnerkc.nondet.examples.HashOrder
nondet check: the runs produced 2 different outcomes

  outcome A  fingerprint 6c8416bfb065  exit 0  run 1
  outcome B  fingerprint 2638869a0512  exit 0  run 2

...

nondet check: minimal causal set: 1 read controls the outcome

  io.github.moonrunnerkc.nondet.examples.HashOrder.firstKey line 39 (UUID.randomUUID)  [read #3 at this site]

forced to their failing values these reproduce outcome 2638869a0512.

repro bundle: nondet-repro.bundle
reproduce with:
  nondet replay --bundle nondet-repro.bundle --class-path examples/target/classes \
    --expect 2638869a05126124656352e279f61f88cdc31270e43b4729092803cc8a21be97 \
    io.github.moonrunnerkc.nondet.examples.HashOrder
```

Run that `replay` command and it serves the recorded UUID back, reproduces outcome B, and exits 0.
Change the value the bundle records and it stops reproducing, which is what makes it a regression
check.

## check ConfigGreeting: a config branch (SYSPROP), and an honest limit

ConfigGreeting reads a system property and branches on it. On a single machine the property does not
change between runs, so both runs read the same value, produce the same greeting, and check reports
the run as outcome-stable. Exit code 0.

```
$ java -jar cli/target/nondet-cli.jar check \
    --class-path examples/target/classes \
    io.github.moonrunnerkc.nondet.examples.samples.ConfigGreeting
nondet check: no causal nondeterminism across 2 runs

the runs read the same entropy in the same order and produced the same outcome (fingerprint aea723b3cbf4).
```

This is not a bug in the sample or the tool: it is the honest limit of passive observation. The branch
is a real reproducibility risk across machines, but running the same machine twice cannot surface it
because the property is stable. See [Limits](LIMITS.md).

## On real external code

The same `check --minimize` pins a real cause inside third-party code. The
[evidence report](EVIDENCE-REPORT.md) runs it against `beyondfengyu/SnowFlake` at a pinned commit and
attributes the divergence to the `System.currentTimeMillis` read inside the upstream generator, with a
repro that reproduces. See [Evidence](EVIDENCE.md) to run that campaign yourself.
