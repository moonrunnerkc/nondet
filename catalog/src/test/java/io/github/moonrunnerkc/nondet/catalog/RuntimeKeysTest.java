package io.github.moonrunnerkc.nondet.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RuntimeKeysTest {

  @Test
  void everyKeyIsNamespacedUnderNondet() {
    final List<String> keys = List.of(
        RuntimeKeys.TRACE_OUT, RuntimeKeys.REGISTRY_OUT, RuntimeKeys.RESULT_OUT,
        RuntimeKeys.RESULT_VALUE, RuntimeKeys.MAX_EVENTS, RuntimeKeys.DEBUG,
        RuntimeKeys.MODE, RuntimeKeys.REPLAY_IN, RuntimeKeys.REPLAY_PIN);
    for (final String key : keys) {
      assertTrue(key.startsWith("nondet."),
          "every agent protocol key must live under the nondet namespace, got " + key);
    }
  }

  @Test
  void keysAreDistinctSoNoTwoChannelsCollide() {
    final List<String> keys = List.of(
        RuntimeKeys.TRACE_OUT, RuntimeKeys.REGISTRY_OUT, RuntimeKeys.RESULT_OUT,
        RuntimeKeys.RESULT_VALUE, RuntimeKeys.MAX_EVENTS, RuntimeKeys.DEBUG,
        RuntimeKeys.MODE, RuntimeKeys.REPLAY_IN, RuntimeKeys.REPLAY_PIN);
    assertEquals(keys.size(), keys.stream().distinct().count(),
        "two protocol keys share a name, so one channel would overwrite another");
  }

  @Test
  void resultFileAndResultValueAreSeparateChannels() {
    assertEquals("nondet.result.out", RuntimeKeys.RESULT_OUT);
    assertEquals("nondet.result", RuntimeKeys.RESULT_VALUE);
    assertTrue(RuntimeKeys.RESULT_OUT.startsWith(RuntimeKeys.RESULT_VALUE),
        "the result file key is expected to extend the result value key namespace");
  }

  @Test
  void replayModeValueIsTheLiteralTheAgentCompares() {
    assertEquals("replay", RuntimeKeys.MODE_REPLAY);
  }
}
