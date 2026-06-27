package io.github.moonrunnerkc.nondet.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.moonrunnerkc.nondet.catalog.EntropySource;
import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReflectiveCatalogTest {

  @Test
  void matchesAReflectivelyResolvedNanoTime() throws NoSuchMethodException {
    final Method nanoTime = System.class.getMethod("nanoTime");
    final Optional<EntropySource> match = ReflectiveCatalog.match(nanoTime);
    assertTrue(match.isPresent(), "System.nanoTime is a catalog source");
    assertEquals("System.nanoTime", match.get().api());
  }

  @Test
  void matchesAGetPropertyOverloadByOwnerAndName() throws NoSuchMethodException {
    final Method getProperty = System.class.getMethod("getProperty", String.class);
    assertEquals("System.getProperty", ReflectiveCatalog.match(getProperty).orElseThrow().api());
  }

  @Test
  void doesNotMatchANonCatalogMethod() throws NoSuchMethodException {
    final Method length = String.class.getMethod("length");
    assertTrue(ReflectiveCatalog.match(length).isEmpty(), "String.length is not entropy");
  }
}
