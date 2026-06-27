# nondet evidence report

Every row below is a real `nondet check --minimize` run over 2 executions of the target. Nothing here is hand written: the verdict comes from the
run's exit code and the headers check printed, and the repro commands are copied from
its output. Clock and random values differ between machines and runs, so the exact
fingerprints in the repro commands are specific to the run that produced this report.

| Target | Provenance | Verdict | Causal reads |
| --- | --- | --- | --- |
| NanoMicro | bundled micro fixture | cause attributed | io.github.moonrunnerkc.nondet.evidence.micro.NanoMicro.main line 20 (System.nanoTime) |
| MillisMicro | bundled micro fixture | cause attributed | io.github.moonrunnerkc.nondet.evidence.micro.MillisMicro.main line 20 (System.currentTimeMillis) |
| RandomMicro | bundled micro fixture | cause attributed | io.github.moonrunnerkc.nondet.evidence.micro.RandomMicro.main line 20 (Math.random) |
| UuidMicro | bundled micro fixture | cause attributed | io.github.moonrunnerkc.nondet.evidence.micro.UuidMicro.main line 22 (UUID.randomUUID) |
| FlakyRetry | bundled example | outcome stable | none: the runs agreed on outcome |
| RandomShardRouter | bundled example | cause attributed | io.github.moonrunnerkc.nondet.examples.samples.RandomShardRouter.main line 28 (Math.random); io.github.moonrunnerkc.nondet.examples.samples.RandomShardRouter.main line 28 (Math.random)  [read #2 at this site]; io.github.moonrunnerkc.nondet.examples.samples.RandomShardRouter.main line 28 (Math.random)  [read #3 at this site]; io.github.moonrunnerkc.nondet.examples.samples.RandomShardRouter.main line 28 (Math.random)  [read #4 at this site]; io.github.moonrunnerkc.nondet.examples.samples.RandomShardRouter.main line 28 (Math.random)  [read #5 at this site]; io.github.moonrunnerkc.nondet.examples.samples.RandomShardRouter.main line 28 (Math.random)  [read #8 at this site]; io.github.moonrunnerkc.nondet.examples.samples.RandomShardRouter.main line 28 (Math.random)  [read #9 at this site]; io.github.moonrunnerkc.nondet.examples.samples.RandomShardRouter.main line 28 (Math.random)  [read #12 at this site] |
| ConfigGreeting | bundled example | outcome stable | none: the runs agreed on outcome |
| SnowFlake | beyondfengyu/SnowFlake @ f62ef149c7ff6853f1ad66ed37cc2c0f96b27b3e | cause attributed | SnowFlake.getNewstmp line 92 (System.currentTimeMillis) |

## Repro commands

Each command replays the recorded reads and exits zero only if it reproduces the
failing outcome, so it verifies itself.

### NanoMicro

```
nondet replay --bundle evidence/target/campaign/runs/NanoMicro-repro.bundle --class-path evidence/target/classes --expect 40736b39039cf55863a751e94f912f23c483113f8414832ee206e47df0b30913 io.github.moonrunnerkc.nondet.evidence.micro.NanoMicro
```

### MillisMicro

```
nondet replay --bundle evidence/target/campaign/runs/MillisMicro-repro.bundle --class-path evidence/target/classes --expect 59071db91eeea48d88a830a9aeea6f317674410710be1c5e14503ed0a1c1a498 io.github.moonrunnerkc.nondet.evidence.micro.MillisMicro
```

### RandomMicro

```
nondet replay --bundle evidence/target/campaign/runs/RandomMicro-repro.bundle --class-path evidence/target/classes --expect e28d1ca26f65a2b525ee1f6f2a44d0b019ff802ddaeddd28da63e832a35779ab io.github.moonrunnerkc.nondet.evidence.micro.RandomMicro
```

### UuidMicro

```
nondet replay --bundle evidence/target/campaign/runs/UuidMicro-repro.bundle --class-path evidence/target/classes --expect 2fe704018b9741e1070f9e3fb7833b621d04008177b885dfa1226e91887821e2 io.github.moonrunnerkc.nondet.evidence.micro.UuidMicro
```

### RandomShardRouter

```
nondet replay --bundle evidence/target/campaign/runs/RandomShardRouter-repro.bundle --class-path examples/target/classes --expect 2f45e8d13b448388a843e88c83cd67bb721dd2ff253235913f3f34fb5285ec1f io.github.moonrunnerkc.nondet.examples.samples.RandomShardRouter
```

### SnowFlake

```
nondet replay --bundle evidence/target/campaign/runs/SnowFlake-repro.bundle --class-path evidence/target/campaign/snowflake-classes --expect 771a34d4ce3ac7a09f19aef49adc7a174163d79be5bef10e5ae6d7fae789db99 SnowflakeDriver
```

## What this shows, and what it does not

- A "cause attributed" row means check ran the target several times, the outcomes differed,
  and delta-debugging narrowed the differing reads to the minimal set that controls the
  outcome, then wrote a repro bundle that reproduces it.
- An "outcome stable" row is not a failure of the tool: it is the no-false-positive case. The
  reads varied between runs but never changed what the program produced, so there is nothing
  to attribute.
- A "diverged, unattributed" row is honest about a limit: the outcomes differed but no read in
  the six-source catalog controlled them, so the cause is elsewhere, for example a JDK clock
  reached below the catalog or thread scheduling.
- The tool produces the repro bundle and the verified repro command. Opening a fix and getting
  it merged upstream is a real-world outcome a repro enables, not something this harness does
  on its own.
