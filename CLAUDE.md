# CLAUDE.md

Rules for working in this repository. Follow all of them for every file.

## Project

nondet is a JVM nondeterminism detector. It finds the exact call site where a
program's execution stops being reproducible. Two modes share one entropy
catalog: a static bytecode scanner (`nondet scan`) and a dynamic differential
checker (`nondet check`) that runs a workload several times and, when the runs
produce different outcomes, names the minimal set of entropy reads that causes
the difference and writes a deterministic repro that reproduces it on demand.

Personal open source under github.com/moonrunnerkc. This is not an Aftermath
Technologies product and must never be described as one.

Java 21. Maven multi-module reactor: catalog, scan, agent, cli, examples.

## Build and test

- `mvn -q -DskipTests package` to build.
- `mvn -q test` to run tests.
- `mvn -q -pl cli exec:java ...` or the built jar to run the CLI.
- The agent jar manifest must carry `Premain-Class` and
  `Can-Retransform-Classes: true`.

## Hard rules, non-negotiable

- No em dashes anywhere, including code comments, Javadoc, commit messages, and
  the README. Use commas, colons, semicolons, parentheses, or separate sentences.
- Code reads as written by a careful human. No AI tells, no generic filler names,
  no boilerplate padding, no comments that restate the code.
- Prose (README, docs, Javadoc descriptions) uses contractions and a plain,
  direct voice. No marketing language, no "empowering," no feature walls.
- No fabricated metrics, benchmarks, or results. If a number is not measured, it
  does not appear. Accuracy over hype, always.
- Do not implement files marked `// TODO(brad):`. Leave them as contracted stubs.

## Dependencies

Minimal by policy, since the tool is about hidden dependencies. The only allowed
dependencies are ASM (asm, asm-tree, asm-commons), picocli, and JUnit 5 for tests.
Do not add any other dependency without explicit approval. Hook and Recorder stay
pure JDK so they resolve under any classloader.

## Java code standards

- No wildcard imports. Import every type explicitly.
- No raw types and no unchecked casts. If a cast is unavoidable, isolate and
  document why.
- Prefer records for data carriers. Prefer immutability. Mark fields and locals
  `final` where it aids clarity.
- Package-private by default. Make a type or method public only when something
  outside the package needs it, and then it gets full Javadoc.
- Full Javadoc on every public type and public method, written as a contract:
  what it does, what it returns, what it throws, and any precondition.
- 300-line file limit. Past that, decompose. One responsibility per class.
- Names are clear and specific. No abbreviations that are not standard, no
  Hungarian notation, no single-letter names outside tight loops.
- Error messages state what failed and what to do about it, not just that
  something went wrong.

## Determinism (this tool especially)

The detector itself must be deterministic. No wall-clock time or unseeded
randomness in core logic. Output uses stable sort order so two runs over the same
input produce byte-identical reports. Identifiers derive from stable coordinates,
not from load order or counters.

## Testing

- JUnit 5. Test names describe the behavior under test, not the method name.
- Tests validate real behavior, not wiring. A test should be understandable
  without reading the implementation it covers.
- Do not mock what can be tested directly. Use real bytecode fixtures (small
  compiled classes with known entropy sources) rather than mocking ASM.
- Every public type has at least one behavior test. Prefer an integration test
  when the boundary between components is the thing that matters.
- Stubbed, unimplemented units get their intended tests written and annotated
  `@Disabled("awaiting implementation")` so the suite stays green.

## Comments and commits

- Comment the why, never the what. Delete any comment that paraphrases the code.
- No decorative banners or section-divider comments.
- Commit messages: imperative mood, terse, scoped to the module touched. No
  "this commit," no AI tells, no filler.

## Project guardrails

- The entropy catalog is frozen at six sources: System.nanoTime,
  System.currentTimeMillis, Math.random, UUID.randomUUID, System.getenv,
  System.getProperty. This holds through v0.2.0. Do not expand it without
  approval. Catalog growth is the main scope risk, and v0.2.0 buys its value
  from causal logic, not from more sources.
- v0.2.0 lifts the v0.1.0 freeze on thread-scheduling attribution and
  JDK-internal patching, but only within a narrow boundary:
  - Reflection coverage is limited to rewriting call sites of
    java.lang.reflect.Method.invoke and java.lang.invoke.MethodHandle invocation
    in instrumented (non-skipped) classes, so a catalog target dispatched through
    them routes through the same Recorder. The JDK reflection classes themselves
    are still never instrumented or patched. When the true source line of a
    reflective catalog call cannot be recovered, it is labelled reflective and
    said so, never fabricated.
  - Thread coverage is limited to pinning the order of entropy reads under
    replay, by making each rewritten read wait its turn against the recorded
    global sequence. It pins ordering only at entropy-read points; it does not
    patch the JVM scheduler, rewrite JDK concurrency classes, or claim exact
    interleaving between reads. Where order still cannot be pinned, the existing
    honest "approximate" note stays.
  - Dependency scanning is still out of scope.
- The two human-owned files are agent EntropyMethodVisitor (the call-site rewrite)
  and cli Diff (first-divergence). Never implement or modify them. All new causal
  logic lives in new types. A new component that needs first-divergence behavior
  calls the existing Diff contract rather than rewriting it. If one of these files
  must change, stop and leave a `// TODO(brad):` contract describing the change.
- The only allowed runtime dependencies stay ASM and picocli, with JUnit 5 for
  tests. Delta-debugging, bundle IO, and replay are written in house with the JDK.
