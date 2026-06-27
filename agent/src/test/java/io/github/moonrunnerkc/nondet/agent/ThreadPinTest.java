package io.github.moonrunnerkc.nondet.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.moonrunnerkc.nondet.catalog.Bundle;
import io.github.moonrunnerkc.nondet.catalog.Category;
import io.github.moonrunnerkc.nondet.catalog.Event;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ThreadPinTest {

  @Test
  void servesValuesInGlobalOrderWhenCalledInThatOrder() {
    final ThreadPin pin = ThreadPin.fromBundle(Bundle.fromEvents(List.of(
        new Event(0, Category.TIME, "siteA", "a0"),
        new Event(1, Category.TIME, "siteB", "b0"),
        new Event(2, Category.TIME, "siteA", "a1"))));

    assertEquals("a0", pin.serve("siteA"));
    assertEquals("b0", pin.serve("siteB"));
    assertEquals("a1", pin.serve("siteA"));
  }

  @Test
  void aReadBlocksUntilItsSiteIsNextInGlobalOrder() throws InterruptedException {
    final ThreadPin pin = ThreadPin.fromBundle(Bundle.fromEvents(List.of(
        new Event(0, Category.TIME, "siteA", "first"),
        new Event(1, Category.TIME, "siteB", "second"))));
    final AtomicReference<String> served = new AtomicReference<>();
    final CountDownLatch started = new CountDownLatch(1);

    final Thread reader = new Thread(() -> {
      started.countDown();
      served.set(pin.serve("siteB"));
    });
    reader.start();
    started.await();
    waitUntilBlocked(reader);
    assertNull(served.get(), "the siteB read must wait while siteA is the next recorded read");

    assertEquals("first", pin.serve("siteA"), "serving siteA unblocks the waiting siteB read");
    reader.join(TimeUnit.SECONDS.toMillis(2));
    assertEquals("second", served.get(), "the siteB read proceeds once it is its turn");
  }

  @Test
  void exhaustedReadsReturnNullSoTheCallerFallsThroughToLive() {
    final ThreadPin pin = ThreadPin.fromBundle(Bundle.fromEvents(List.of(
        new Event(0, Category.TIME, "siteA", "only"))));

    assertEquals("only", pin.serve("siteA"));
    assertNull(pin.serve("siteA"), "a read past the recorded ones is not served");
  }

  private static void waitUntilBlocked(Thread thread) throws InterruptedException {
    for (int i = 0; i < 200; i++) {
      final Thread.State state = thread.getState();
      if (state == Thread.State.WAITING || state == Thread.State.BLOCKED) {
        return;
      }
      Thread.sleep(5);
    }
    assertTrue(false, "the reader thread never blocked waiting for its turn");
  }
}
