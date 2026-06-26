package io.github.moonrunnerkc.nondet.examples.samples;

/**
 * Routes a fixed set of requests across shards by drawing a random number per request, the
 * way a naive load balancer might. The per-shard counts come out different every run because
 * the draws do, which is the kind of "works on my machine" nondeterminism that hides in
 * tests that assert on a specific shard.
 *
 * <p>The entropy is {@link Math#random()}, a RANDOM source. {@code nondet check} pins the
 * divergence at that call, the root of the differing routing.
 */
public final class RandomShardRouter {

  private static final int SHARDS = 4;
  private static final int REQUESTS = 12;

  private RandomShardRouter() {
  }

  /**
   * Routes the fixed request set once and prints the per-shard counts.
   *
   * @param args ignored
   */
  public static void main(String[] args) {
    final int[] perShard = new int[SHARDS];
    for (int request = 0; request < REQUESTS; request++) {
      perShard[shardFor(Math.random(), SHARDS)]++;
    }
    final StringBuilder out = new StringBuilder("routing:");
    for (int shard = 0; shard < SHARDS; shard++) {
      out.append(' ').append(shard).append('=').append(perShard[shard]);
    }
    System.out.println(out);
  }

  /**
   * Maps a draw in {@code [0, 1)} to a shard index.
   *
   * @param draw       a value in {@code [0, 1)}, as returned by {@link Math#random()}
   * @param shardCount the number of shards, greater than zero
   * @return a shard index in {@code [0, shardCount)}; a draw of exactly one clamps to the
   *     last shard rather than overflowing
   */
  static int shardFor(double draw, int shardCount) {
    final int shard = (int) (draw * shardCount);
    return shard >= shardCount ? shardCount - 1 : shard;
  }
}
