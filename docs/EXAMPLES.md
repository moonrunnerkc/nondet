# Worked examples

Real, captured runs of `nondet` against the bundled `examples` module. Nothing here is
invented; each block is copied from an actual run on a single Linux box with Temurin JDK 21.
The clock and random values differ every run, so yours will not match digit for digit; the
shape of the report and the exit code are what to compare against.

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

## check RetryWithTimeout: a clock-driven retry loop (TIME)

The loop retries until a wall-clock deadline, so the number of attempts and the clock values
behind them shift between runs. Check pins the divergence at the `System.currentTimeMillis`
call, not at the attempt count it feeds. Exit code 1.

```
$ java -jar cli/target/nondet-cli.jar check \
    --class-path examples/target/classes \
    io.github.moonrunnerkc.nondet.examples.samples.RetryWithTimeout
nondet check: first divergence at read #0

  run A: io.github.moonrunnerkc.nondet.examples.samples.RetryWithTimeout.attempts line 43 (System.currentTimeMillis)  read 1782427844487
  run B: io.github.moonrunnerkc.nondet.examples.samples.RetryWithTimeout.attempts line 43 (System.currentTimeMillis)  read 1782427844935

high confidence: this call site read different values across the two runs
```

## check RandomShardRouter: random routing across shards (RANDOM)

Each request is routed by a fresh `Math.random()` draw, so the per-shard counts differ every
run. Check pins the divergence at the draw, the root of the differing routing. Exit code 1.

```
$ java -jar cli/target/nondet-cli.jar check \
    --class-path examples/target/classes \
    io.github.moonrunnerkc.nondet.examples.samples.RandomShardRouter
nondet check: first divergence at read #0

  run A: io.github.moonrunnerkc.nondet.examples.samples.RandomShardRouter.main line 28 (Math.random)  read 0.36575568913603973
  run B: io.github.moonrunnerkc.nondet.examples.samples.RandomShardRouter.main line 28 (Math.random)  read 0.8027773750323397

high confidence: this call site read different values across the two runs
```

## check ConfigGreeting: a config branch (SYSPROP), and an honest limit

ConfigGreeting reads a system property and branches on it. On a single machine the property
does not change between runs, so both runs read the same value and check reports no
divergence. Exit code 0.

```
$ java -jar cli/target/nondet-cli.jar check \
    --class-path examples/target/classes \
    io.github.moonrunnerkc.nondet.examples.samples.ConfigGreeting
nondet check: no divergence; both runs read the same entropy in the same order
```

This is not a bug in the sample or the tool: it is the honest limit of passive observation.
The branch is a real reproducibility risk, since the same code prints a different greeting on
a box configured differently, but running the same machine twice cannot surface it because
the property is stable. Exposing this kind of latent branch is exactly what the planned
active-injection mode is for: it would vary the property on purpose between runs and watch the
branch take a different path. Until then, a NONE result over a config read means "not observed
to differ here," not "proven deterministic."
