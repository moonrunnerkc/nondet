package io.github.moonrunnerkc.nondet.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CallSiteIdTest {

  @Test
  void sameCoordinatesAlwaysProduceTheSameId() {
    final String first = CallSiteId.of("pkg/Demo", "run", 42, "System.nanoTime");
    final String second = CallSiteId.of("pkg/Demo", "run", 42, "System.nanoTime");
    assertEquals(first, second);
  }

  @Test
  void aDifferentLineProducesADifferentId() {
    final String line42 = CallSiteId.of("pkg/Demo", "run", 42, "System.nanoTime");
    final String line43 = CallSiteId.of("pkg/Demo", "run", 43, "System.nanoTime");
    assertNotEquals(line42, line43);
  }

  @Test
  void aDifferentApiAtTheSameLocationProducesADifferentId() {
    final String nano = CallSiteId.of("pkg/Demo", "run", 42, "System.nanoTime");
    final String millis = CallSiteId.of("pkg/Demo", "run", 42, "System.currentTimeMillis");
    assertNotEquals(nano, millis);
  }

  @Test
  void idIsLowerCaseHexOfFixedLength() {
    final String id = CallSiteId.of("pkg/Demo", "run", 42, "System.nanoTime");
    assertEquals(16, id.length());
    assertTrue(id.matches("[0-9a-f]{16}"), "expected 16 lower case hex characters but got: " + id);
  }
}
