# Evidence

Summary: how to run the evidence campaign yourself, what it measures, and how a repro bundle turns
into an upstream fix. The bar is the one NonDex set: run on real code, pin real causes, produce
repros that reproduce.

## The campaign

`evidence/corpus/run-campaign.sh` builds the tool, fetches one pinned real project, and runs
`nondet check --minimize` over a corpus of targets, writing [EVIDENCE-REPORT.md](EVIDENCE-REPORT.md).
Every row in that report is a real run the script drove; nothing in it is hand written.

```bash
evidence/corpus/run-campaign.sh
```

The corpus has three kinds of target:

- **Micro fixtures** (`evidence/src/main/java/.../micro`): one per catalog source that can drive an
  outcome, each letting a single read reach its output. These are the regression gate: `MicroBenchTest`
  runs the same pipeline in the test suite and fails if any known cause stops being attributed.
- **Bundled examples**: the `examples` module's programs, including the honest stable cases where a
  read varies but never changes the outcome.
- **A pinned real project**: [beyondfengyu/SnowFlake](https://github.com/beyondfengyu/SnowFlake),
  pinned at commit `f62ef149c7ff6853f1ad66ed37cc2c0f96b27b3e`, compiled from source alongside a thin
  driver. Its `nextId()` reads `System.currentTimeMillis` once, and the report attributes the
  divergence to that read inside the upstream code, at `SnowFlake.getNewstmp line 92`.

The report classifies each run from its exit code and the headers check printed:

- **cause attributed**: the outcomes differed and delta-debugging found the minimal reads that
  control them, with a repro bundle that reproduces the failure.
- **outcome stable**: the reads varied but the outcome did not, the no-false-positive case.
- **diverged, unattributed**: the outcomes differed but no catalog read controlled them, so the cause
  is outside the six sources.
- **could not run**: the target did not run under the agent, so nothing was measured.

## From a repro bundle to a fix

`nondet check --minimize` writes a repro bundle and prints a `nondet replay` command that reproduces
the failing outcome and exits non-zero if it ever stops reproducing. That is the artifact a fix is
built on:

1. Run `nondet check --minimize` on the flaky workload until the outcomes diverge, and keep the
   repro bundle it writes.
2. Read the minimal causal set. It names the exact reads, with class, method, line, and api, that
   control the failure. That is where the nondeterminism enters.
3. Make the fix at that read: seed the generator, inject the clock, pass the value in, or assert on
   something stable instead of the raw read.
4. Use the `nondet replay --expect` command as a regression check: before the fix it reproduces the
   failure, after the fix the outcome no longer matches and the command reports it.

The tool produces the repro and the verified command. Opening a pull request and getting it merged is
a real-world outcome a solid repro enables; it is not something this harness can do on its own, and
this doc does not claim any merged PRs that have not happened.

## Reproducing the report

The repro bundles a campaign writes live under `target/`, which is not committed, so the exact
commands in a committed report point at paths from the run that produced it. Re-run the campaign to
regenerate the report and its bundles against your own machine; the verdicts will match, and the
fingerprints will be your run's.
