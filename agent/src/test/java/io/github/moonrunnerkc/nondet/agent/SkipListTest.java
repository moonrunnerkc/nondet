package io.github.moonrunnerkc.nondet.agent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SkipListTest {

  @Test
  void skipsTheJdkAndJvmInternals() {
    assertTrue(SkipList.shouldSkip("java/lang/String"));
    assertTrue(SkipList.shouldSkip("jdk/internal/misc/Unsafe"));
    assertTrue(SkipList.shouldSkip("sun/nio/ch/IOUtil"));
    assertTrue(SkipList.shouldSkip("com/sun/proxy/Proxy0"));
  }

  @Test
  void skipsAsmAndTheShadedAgentRuntime() {
    assertTrue(SkipList.shouldSkip("org/objectweb/asm/ClassReader"));
    assertTrue(SkipList.shouldSkip("io/github/moonrunnerkc/nondet/agent/Hook"));
    assertTrue(SkipList.shouldSkip("io/github/moonrunnerkc/nondet/catalog/Catalog"));
  }

  @Test
  void instrumentsWorkloadsIncludingTheBundledExamples() {
    assertFalse(SkipList.shouldSkip("com/example/App"));
    assertFalse(SkipList.shouldSkip("probe/NanoSite"));
    assertFalse(SkipList.shouldSkip("io/github/moonrunnerkc/nondet/examples/FlakyRetry"));
    assertFalse(SkipList.shouldSkip("io/github/moonrunnerkc/nondet/scan/EntropyScanner"));
  }

  @Test
  void skipsAClassWithNoName() {
    assertTrue(SkipList.shouldSkip(null));
  }
}
