package io.github.moonrunnerkc.nondet.examples;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A program whose printed key depends on hash bucket placement of random keys.
 *
 * <p>Three {@link UUID#randomUUID()} values go into a {@link HashMap}; the program prints
 * the first key the iterator yields. Random UUIDs hash to different buckets each run, so
 * the iteration order, and therefore the first key, changes from run to run. The root
 * cause the scanner flags is the {@code UUID.randomUUID} call that produces the keys.
 */
public final class HashOrder {

  private static final int KEY_COUNT = 3;

  private HashOrder() {
  }

  /**
   * Builds the map and prints the first key in iteration order.
   *
   * @param args ignored
   */
  public static void main(String[] args) {
    System.out.println(firstKey());
  }

  /**
   * Returns the first key a fresh map of random UUIDs yields from its iterator.
   *
   * @return one of the generated keys, never {@code null}
   */
  static UUID firstKey() {
    final Map<UUID, String> byId = new HashMap<>();
    for (int i = 0; i < KEY_COUNT; i++) {
      byId.put(UUID.randomUUID(), "value-" + i);
    }
    return byId.keySet().iterator().next();
  }
}
