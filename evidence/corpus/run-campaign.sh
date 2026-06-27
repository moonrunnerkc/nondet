#!/usr/bin/env bash
#
# Run the nondet evidence campaign: build the tool, fetch a pinned real project, and run
# nondet check --minimize over the whole corpus, writing docs/EVIDENCE-REPORT.md.
#
# Every number in the report comes from a real run this script drives. Nothing is hand written.
# The real project (beyondfengyu/SnowFlake) is pinned to a commit so the run is reproducible.
#
# Usage: evidence/corpus/run-campaign.sh [work-dir]

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
WORK="${1:-$ROOT/evidence/target/campaign}"
RUNS="${RUNS:-2}"

SNOW_REPO="https://github.com/beyondfengyu/SnowFlake.git"
SNOW_COMMIT="f62ef149c7ff6853f1ad66ed37cc2c0f96b27b3e"

CLI_JAR="$ROOT/cli/target/nondet-cli.jar"
AGENT_JAR="$ROOT/agent/target/nondet-agent.jar"
EVIDENCE_CLASSES="$ROOT/evidence/target/classes"
EXAMPLE_CLASSES="$ROOT/examples/target/classes"

echo "==> building the tool"
( cd "$ROOT" && mvn -q -DskipTests package )

echo "==> fetching the pinned real project ($SNOW_COMMIT)"
SNOW_SRC="$WORK/snowflake-src"
SNOW_OUT="$WORK/snowflake-classes"
rm -rf "$SNOW_SRC" "$SNOW_OUT"
mkdir -p "$SNOW_OUT"
git clone --quiet "$SNOW_REPO" "$SNOW_SRC"
git -C "$SNOW_SRC" checkout --quiet "$SNOW_COMMIT"
cp "$ROOT/evidence/corpus/SnowflakeDriver.java" "$SNOW_SRC/"
javac -d "$SNOW_OUT" "$SNOW_SRC/SnowFlake.java" "$SNOW_SRC/SnowflakeDriver.java"

echo "==> writing the corpus"
CORPUS="$WORK/corpus.tsv"
mkdir -p "$WORK"
MICRO="io.github.moonrunnerkc.nondet.evidence.micro"
EX="io.github.moonrunnerkc.nondet.examples"
{
  printf 'NanoMicro\tbundled micro fixture\t%s\t%s.NanoMicro\n' "$EVIDENCE_CLASSES" "$MICRO"
  printf 'MillisMicro\tbundled micro fixture\t%s\t%s.MillisMicro\n' "$EVIDENCE_CLASSES" "$MICRO"
  printf 'RandomMicro\tbundled micro fixture\t%s\t%s.RandomMicro\n' "$EVIDENCE_CLASSES" "$MICRO"
  printf 'UuidMicro\tbundled micro fixture\t%s\t%s.UuidMicro\n' "$EVIDENCE_CLASSES" "$MICRO"
  printf 'FlakyRetry\tbundled example\t%s\t%s.FlakyRetry\n' "$EXAMPLE_CLASSES" "$EX"
  printf 'RandomShardRouter\tbundled example\t%s\t%s.samples.RandomShardRouter\n' "$EXAMPLE_CLASSES" "$EX"
  printf 'ConfigGreeting\tbundled example\t%s\t%s.samples.ConfigGreeting\n' "$EXAMPLE_CLASSES" "$EX"
  printf 'SnowFlake\tbeyondfengyu/SnowFlake @ %s\t%s\tSnowflakeDriver\n' "$SNOW_COMMIT" "$SNOW_OUT"
} > "$CORPUS"

echo "==> running nondet check --minimize over the corpus"
java -cp "$EVIDENCE_CLASSES" io.github.moonrunnerkc.nondet.evidence.CorpusRunner \
  --cli "$CLI_JAR" \
  --agent "$AGENT_JAR" \
  --corpus "$CORPUS" \
  --out "$ROOT/docs/EVIDENCE-REPORT.md" \
  --work "$WORK/runs" \
  --runs "$RUNS"

echo "==> done: docs/EVIDENCE-REPORT.md"
