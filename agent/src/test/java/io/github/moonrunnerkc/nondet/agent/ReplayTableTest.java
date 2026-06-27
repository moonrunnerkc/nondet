package io.github.moonrunnerkc.nondet.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.moonrunnerkc.nondet.catalog.Bundle;
import io.github.moonrunnerkc.nondet.catalog.Category;
import io.github.moonrunnerkc.nondet.catalog.Event;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReplayTableTest {

  @Test
  void servesEachSitesValuesInPerSiteOrder() {
    final ReplayTable table = ReplayTable.fromBundle(Bundle.fromEvents(List.of(
        new Event(0, Category.TIME, "site", "first"),
        new Event(1, Category.TIME, "site", "second"))));

    assertEquals("first", table.next("site"));
    assertEquals("second", table.next("site"));
  }

  @Test
  void tracksAseparateCursorPerSite() {
    final ReplayTable table = ReplayTable.fromBundle(Bundle.fromEvents(List.of(
        new Event(0, Category.TIME, "a", "a0"),
        new Event(1, Category.TIME, "b", "b0"),
        new Event(2, Category.TIME, "a", "a1"))));

    assertEquals("a0", table.next("a"));
    assertEquals("b0", table.next("b"));
    assertEquals("a1", table.next("a"));
  }

  @Test
  void returnsNullForAnAbsentSite() {
    final ReplayTable table = ReplayTable.fromBundle(Bundle.fromEvents(List.of(
        new Event(0, Category.TIME, "known", "v"))));

    assertNull(table.next("unknown"));
  }

  @Test
  void returnsNullOnceASiteIsExhausted() {
    final ReplayTable table = ReplayTable.fromBundle(Bundle.fromEvents(List.of(
        new Event(0, Category.TIME, "site", "only"))));

    assertEquals("only", table.next("site"));
    assertNull(table.next("site"));
  }
}
