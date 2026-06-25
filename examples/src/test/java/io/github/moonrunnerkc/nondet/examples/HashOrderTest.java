package io.github.moonrunnerkc.nondet.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class HashOrderTest {

  @Test
  void returnsAKeyFromThePopulatedMap() {
    final UUID first = HashOrder.firstKey();
    assertNotNull(first, "a three entry map always yields a first key");
  }

  @Test
  void theYieldedKeyIsAWellFormedUuid() {
    final UUID first = HashOrder.firstKey();
    assertEquals(first, UUID.fromString(first.toString()));
  }
}
