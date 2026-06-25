package io.github.moonrunnerkc.nondet.agent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The Hook signature set is the contract the rewrite guard consults. It must hold exactly
 * the supported overloads: the four no-arg sources and the String-returning env and
 * property reads, and it must not hold the no-arg {@code System.getenv()} that returns the
 * whole environment, since that overload has no Hook and must pass through untouched.
 */
class HookMethodsTest {

  private final Set<String> signatures = HookMethods.signatures();

  @Test
  void coversTheFourNoArgSources() {
    assertTrue(signatures.contains("nanoTime(Ljava/lang/String;)J"), signatures.toString());
    assertTrue(signatures.contains("currentTimeMillis(Ljava/lang/String;)J"), signatures.toString());
    assertTrue(signatures.contains("random(Ljava/lang/String;)D"), signatures.toString());
    assertTrue(signatures.contains("randomUUID(Ljava/lang/String;)Ljava/util/UUID;"), signatures.toString());
  }

  @Test
  void coversTheSupportedEnvAndPropertyOverloads() {
    assertTrue(signatures.contains("getenv(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"),
        signatures.toString());
    assertTrue(signatures.contains("getProperty(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"),
        signatures.toString());
    assertTrue(signatures.contains(
        "getProperty(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"),
        signatures.toString());
  }

  @Test
  void omitsTheUnsupportedNoArgGetenvThatReturnsTheWholeEnvironment() {
    assertFalse(signatures.contains("getenv(Ljava/lang/String;)Ljava/util/Map;"), signatures.toString());
  }

  @Test
  void holdsExactlySevenSupportedSignatures() {
    assertTrue(signatures.size() == 7, "expected seven supported Hook signatures but found " + signatures);
  }
}
