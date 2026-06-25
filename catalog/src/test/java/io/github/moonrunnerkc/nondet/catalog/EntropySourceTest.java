package io.github.moonrunnerkc.nondet.catalog;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EntropySourceTest {

  @Test
  void exactSourceMatchesOnlyItsDescriptor() {
    final EntropySource nanoTime =
        new EntropySource(Category.TIME, "java/lang/System", "nanoTime", "()J", "System.nanoTime");
    assertTrue(nanoTime.matches("java/lang/System", "nanoTime", "()J"));
    assertFalse(nanoTime.matches("java/lang/System", "nanoTime", "(I)J"));
  }

  @Test
  void nullDescriptorMatchesEveryOverload() {
    final EntropySource getProperty =
        new EntropySource(Category.SYSPROP, "java/lang/System", "getProperty", null, "System.getProperty");
    assertTrue(getProperty.matches("java/lang/System", "getProperty", "(Ljava/lang/String;)Ljava/lang/String;"));
    assertTrue(getProperty.matches(
        "java/lang/System", "getProperty", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"));
  }

  @Test
  void rejectsADifferentOwnerOrName() {
    final EntropySource random =
        new EntropySource(Category.RANDOM, "java/lang/Math", "random", "()D", "Math.random");
    assertFalse(random.matches("java/lang/StrictMath", "random", "()D"));
    assertFalse(random.matches("java/lang/Math", "abs", "()D"));
  }

  @Test
  void rejectsMissingRequiredCoordinates() {
    assertThrows(NullPointerException.class,
        () -> new EntropySource(Category.TIME, null, "nanoTime", "()J", "System.nanoTime"));
  }
}
