<div align="center">

<img src="docs/assets/cover.svg" alt="nondet" width="840">

[![build](https://github.com/moonrunnerkc/nondet/actions/workflows/ci.yml/badge.svg)](https://github.com/moonrunnerkc/nondet/actions/workflows/ci.yml)
&nbsp;
[![license: MIT](https://img.shields.io/badge/license-MIT-3fb950.svg)](LICENSE)
&nbsp;
[![Java 21](https://img.shields.io/badge/Java-21-58a6ff.svg)](pom.xml)

**Find the reads that make a JVM program flaky, and get a deterministic repro.**

[Install](#install) · [Quick start](#quick-start) · [Sources](#sources) · [Usage](docs/USAGE.md) · [Examples](docs/EXAMPLES.md) · [How it works](docs/INTERNALS.md) · [Evidence](docs/EVIDENCE.md) · [Limits](docs/LIMITS.md) · [Changelog](CHANGELOG.md)

</div>

---

Some programs pass on your machine and fail on someone else's, or fail one run in ten. The
cause is almost always a hidden read of something that changes between runs: the clock, a
random number, an environment variable, a system property. nondet finds the read that actually
controls the failure and hands back a repro that reproduces it.

It works two ways:

- **scan** reads your compiled classes and lists every place the code touches one of these
  sources. It loads nothing and runs nothing.
- **check** runs your program a few times and compares what it produced, not just what it read.
  When the runs differ, `--minimize` narrows the differing reads to the minimal set that controls
  the outcome and writes a repro bundle plus a `nondet replay` command that reproduces the failure.
  A read whose value varies but never changes the result is not flagged.

Personal open source under github.com/moonrunnerkc. Not an Aftermath Technologies product.

## Install

You need Java 21 and Maven.

```bash
mvn -DskipTests package
```

That produces two jars: the command (`cli/target/nondet-cli.jar`) and the agent
(`agent/target/nondet-agent.jar`). Run the command with the wrapper or plain `java`:

```bash
./nondet --help
java -jar cli/target/nondet-cli.jar --help
```

## Quick start

Scan the bundled examples to see what they read:

```text
$ ./nondet scan examples/target/classes
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

Now pin the cause of one that is not reproducible, and get a repro:

```text
$ ./nondet check --minimize --class-path examples/target/classes \
    io.github.moonrunnerkc.nondet.examples.HashOrder
nondet check: the runs produced 2 different outcomes

  outcome A  fingerprint 6c8416bfb065  exit 0  run 1
  outcome B  fingerprint 2638869a0512  exit 0  run 2

nondet check: minimal causal set: 1 read controls the outcome

  io.github.moonrunnerkc.nondet.examples.HashOrder.firstKey line 39 (UUID.randomUUID)  [read #3 at this site]

repro bundle: nondet-repro.bundle
reproduce with:
  nondet replay --bundle nondet-repro.bundle --class-path examples/target/classes \
    --expect 2638869a0512... io.github.moonrunnerkc.nondet.examples.HashOrder
```

The `replay` command serves the recorded UUID back and exits 0 only if it reproduces outcome B,
so it verifies itself. Exit code `1` means the outcomes differed; `0` means they agreed, which is
the honest no-false-positive case, for example `FlakyRetry`, whose clock varies but whose result
does not. More runs, including a real cause pinned inside third-party code, are in
[Examples](docs/EXAMPLES.md) and the [evidence report](docs/EVIDENCE-REPORT.md).

## Sources

The six entropy sources nondet knows, frozen through v0.2.0:

<div align="center">

| Source | Kind |
| :--- | :---: |
| `System.nanoTime` | TIME |
| `System.currentTimeMillis` | TIME |
| `Math.random` | RANDOM |
| `UUID.randomUUID` | RANDOM |
| `System.getenv` | ENV |
| `System.getProperty` | SYSPROP |

</div>

A read reached through `Method.invoke` is covered and attributed to its caller; one reached through
a `MethodHandle`, or a cause outside these six sources, is reported as an unattributed divergence
rather than pinned. An outcome-stable result is not a proof of determinism. See
[Limits](docs/LIMITS.md) for the full picture, with evidence.

## Documentation

| Guide | What's in it |
| :--- | :--- |
| [Usage](docs/USAGE.md) | Every flag, its default, and what each exit code means. |
| [Examples](docs/EXAMPLES.md) | Real captured runs, including the case where nothing diverges and why. |
| [How it works](docs/INTERNALS.md) | The catalog, outcomes, bundles, replay, and the causal search, in plain terms. |
| [Evidence](docs/EVIDENCE.md) | How to run the corpus campaign and turn a repro bundle into a fix. |
| [Limits and roadmap](docs/LIMITS.md) | What nondet cannot catch yet, with evidence, and what is planned next. |
| [Changelog](CHANGELOG.md) | Release notes. |

## License

MIT. See [LICENSE](LICENSE).
