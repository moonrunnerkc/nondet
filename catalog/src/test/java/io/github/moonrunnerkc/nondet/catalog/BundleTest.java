package io.github.moonrunnerkc.nondet.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class BundleTest {

  @Test
  void perSiteSequenceCountsOccurrencesAtEachSiteFromZero() {
    final Bundle bundle = Bundle.fromEvents(List.of(
        new Event(0, Category.TIME, "siteA", "10"),
        new Event(1, Category.TIME, "siteB", "20"),
        new Event(2, Category.TIME, "siteA", "11"),
        new Event(3, Category.TIME, "siteA", "12")));

    final List<BundleEntry> entries = bundle.entries();
    assertEquals(0, entries.get(0).perSiteSeq(), "first siteA read is occurrence 0");
    assertEquals(0, entries.get(1).perSiteSeq(), "first siteB read is occurrence 0");
    assertEquals(1, entries.get(2).perSiteSeq(), "second siteA read is occurrence 1");
    assertEquals(2, entries.get(3).perSiteSeq(), "third siteA read is occurrence 2");
  }

  @Test
  void keyIndexResolvesAReadByItsSiteAndOccurrence() {
    final Bundle bundle = Bundle.fromEvents(List.of(
        new Event(0, Category.RANDOM, "siteA", "first"),
        new Event(1, Category.RANDOM, "siteA", "second")));

    assertEquals("second", bundle.byKey().get(new Bundle.Key("siteA", 1)).value());
  }

  @Test
  void aNegativePerSiteSequenceIsRejected() {
    assertThrows(IllegalArgumentException.class,
        () -> new BundleEntry(0, "siteA", -1, Category.TIME, "x"));
  }
}
