package io.github.moonrunnerkc.nondet.examples.samples;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The draw-to-shard mapping is deterministic and is tested directly; the randomness lives
 * only in the draw the main passes in.
 */
class RandomShardRouterTest {

  @Test
  void mapsTheUnitIntervalEvenlyAcrossShards() {
    assertEquals(0, RandomShardRouter.shardFor(0.0, 4));
    assertEquals(1, RandomShardRouter.shardFor(0.25, 4));
    assertEquals(2, RandomShardRouter.shardFor(0.5, 4));
    assertEquals(3, RandomShardRouter.shardFor(0.999, 4));
  }

  @Test
  void aDrawOfOneClampsToTheLastShardInsteadOfOverflowing() {
    assertEquals(3, RandomShardRouter.shardFor(1.0, 4));
  }
}
