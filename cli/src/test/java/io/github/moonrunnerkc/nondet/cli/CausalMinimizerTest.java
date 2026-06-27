package io.github.moonrunnerkc.nondet.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class CausalMinimizerTest {

  @Test
  void reducesToTheSingleControllingElement() {
    final Predicate<Set<String>> reproduces = forced -> forced.contains("b");
    assertEquals(List.of("b"), CausalMinimizer.minimize(List.of("a", "b", "c", "d"), reproduces));
  }

  @Test
  void keepsBothElementsWhenTheyJointlyControlTheOutcome() {
    final Predicate<Set<String>> reproduces = forced -> forced.contains("a") && forced.contains("c");
    final List<String> minimal = CausalMinimizer.minimize(List.of("a", "b", "c", "d"), reproduces);
    assertEquals(2, minimal.size(), minimal.toString());
    assertTrue(minimal.containsAll(List.of("a", "c")), minimal.toString());
    assertTrue(reproduces.test(Set.copyOf(minimal)), "the minimal set must still reproduce");
  }

  @Test
  void theResultIsOneMinimalSoRemovingAnyElementLosesTheFailure() {
    final Predicate<Set<String>> reproduces = forced ->
        forced.contains("x") && forced.contains("y") && forced.contains("z");
    final List<String> minimal = CausalMinimizer.minimize(
        List.of("w", "x", "y", "z"), reproduces);
    for (final String element : minimal) {
      final Set<String> withoutOne = new java.util.HashSet<>(minimal);
      withoutOne.remove(element);
      assertTrue(!reproduces.test(withoutOne),
          "removing " + element + " should stop reproducing");
    }
  }

  @Test
  void theSearchIsDeterministic() {
    final Predicate<Set<String>> reproduces = forced -> forced.contains("c") && forced.contains("e");
    final List<String> candidates = List.of("a", "b", "c", "d", "e", "f");
    assertEquals(CausalMinimizer.minimize(candidates, reproduces),
        CausalMinimizer.minimize(candidates, reproduces),
        "the same candidates and test always yield the same minimal set");
  }
}
