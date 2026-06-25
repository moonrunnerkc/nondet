package io.github.moonrunnerkc.nondet.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class CatalogTest {

  private final Catalog catalog = Catalog.ofDefault();

  @Test
  void freezesTheSixV010Sources() {
    assertEquals(6, Catalog.DEFAULT.size());
  }

  @Test
  void matchesNanoTimeToTheTimeCategory() {
    final Optional<EntropySource> source = catalog.match("java/lang/System", "nanoTime", "()J");
    assertTrue(source.isPresent());
    assertEquals(Category.TIME, source.get().category());
    assertEquals("System.nanoTime", source.get().api());
  }

  @Test
  void matchesBothGetenvOverloadsThroughOneEntry() {
    assertTrue(catalog.match("java/lang/System", "getenv", "()Ljava/util/Map;").isPresent());
    assertTrue(catalog.match("java/lang/System", "getenv", "(Ljava/lang/String;)Ljava/lang/String;").isPresent());
  }

  @Test
  void matchesBothGetPropertyOverloadsThroughOneEntry() {
    assertTrue(catalog.match("java/lang/System", "getProperty", "(Ljava/lang/String;)Ljava/lang/String;").isPresent());
    assertTrue(catalog.match(
        "java/lang/System", "getProperty",
        "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;").isPresent());
  }

  @Test
  void doesNotMatchAnOrdinaryCall() {
    assertTrue(catalog.match("java/lang/System", "lineSeparator", "()Ljava/lang/String;").isEmpty());
    assertTrue(catalog.match("java/lang/String", "length", "()I").isEmpty());
  }
}
