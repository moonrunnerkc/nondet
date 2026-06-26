package io.github.moonrunnerkc.nondet.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WireFormatTest {

  @Test
  void traceEventRoundTrips() {
    final Event event = new Event(7, Category.RANDOM, "a1b2c3d4e5f60718", "550e8400-e29b");
    assertEquals(event, WireFormat.parseTrace(WireFormat.formatTrace(event)));
  }

  @Test
  void registryCallSiteRoundTrips() {
    final CallSite callSite =
        new CallSite("a1b2c3d4e5f60718", "pkg/Demo", "run", 42, "System.nanoTime");
    assertEquals(callSite, WireFormat.parseRegistry(WireFormat.formatRegistry(callSite)));
  }

  @Test
  void valuesWithSeparatorsAndNewlinesSurviveTheRoundTrip() {
    final Event event = new Event(
        1, Category.ENV, "id", "PATH=/a|/b\\c\nLANG=en");
    final String line = WireFormat.formatTrace(event);
    assertEquals(-1, line.indexOf('\n'), "an encoded line must never contain a raw newline");
    assertEquals(event, WireFormat.parseTrace(line));
  }

  @Test
  void traceLineUsesThePipeSeparatedContract() {
    final Event event = new Event(3, Category.TIME, "deadbeefdeadbeef", "12345");
    assertEquals("3|TIME|deadbeefdeadbeef|12345", WireFormat.formatTrace(event));
  }

  @Test
  void recognizesTheTruncationMarkerAndNotARecord() {
    assertTrue(WireFormat.isTruncationMarker(WireFormat.TRUNCATION_MARKER));
    assertFalse(WireFormat.isTruncationMarker("3|TIME|deadbeefdeadbeef|12345"));
  }

  @Test
  void threadCountMarkerRoundTrips() {
    final String marker = WireFormat.formatThreadCount(4);
    assertTrue(WireFormat.isThreadCountMarker(marker));
    assertEquals(4, WireFormat.parseThreadCount(marker));
  }

  @Test
  void aRecordIsNotMistakenForAThreadCountMarker() {
    assertFalse(WireFormat.isThreadCountMarker("3|TIME|deadbeefdeadbeef|12345"));
  }
}
