package io.github.moonrunnerkc.nondet.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.moonrunnerkc.nondet.catalog.Category;
import io.github.moonrunnerkc.nondet.catalog.Event;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecorderTest {

  @BeforeEach
  void clearRecorder() {
    Recorder.reset();
  }

  @Test
  void snapshotReturnsEventsInTheOrderTheyWereRecorded() {
    Recorder.record(Category.TIME, "id-a", "1");
    Recorder.record(Category.RANDOM, "id-b", "2");
    Recorder.record(Category.SYSPROP, "id-c", "3");

    final List<Event> events = Recorder.snapshot();
    assertEquals(List.of("id-a", "id-b", "id-c"), events.stream().map(Event::callSiteId).toList());
  }

  @Test
  void sequenceNumbersAreContiguousFromZero() {
    Recorder.record(Category.TIME, "id-a", "1");
    Recorder.record(Category.TIME, "id-b", "2");

    final List<Event> events = Recorder.snapshot();
    assertEquals(0, events.get(0).seq());
    assertEquals(1, events.get(1).seq());
  }

  @Test
  void resetClearsEveryRecordedEvent() {
    Recorder.record(Category.TIME, "id-a", "1");
    Recorder.reset();
    assertEquals(List.of(), Recorder.snapshot());
  }

  @Test
  void threadCountReflectsHowManyThreadsRecorded() throws InterruptedException {
    Recorder.record(Category.TIME, "from-main", "0");
    final Thread worker = new Thread(() -> Recorder.record(Category.RANDOM, "from-worker", "1"));
    worker.start();
    worker.join();

    assertEquals(2, Recorder.threadCount(),
        "both the main thread and the worker produced events");
  }

  @Test
  void threadCountIsZeroWhenNothingWasRecorded() {
    assertEquals(0, Recorder.threadCount());
  }

  @Test
  void snapshotFromOneThreadSeesEventsRecordedOnAnother() throws InterruptedException {
    final Thread worker = new Thread(() -> {
      Recorder.record(Category.RANDOM, "from-worker-1", "x");
      Recorder.record(Category.RANDOM, "from-worker-2", "y");
    }, "recorder-worker");
    worker.start();
    worker.join();

    final List<String> ids = Recorder.snapshot().stream().map(Event::callSiteId).toList();
    assertEquals(List.of("from-worker-1", "from-worker-2"), ids,
        "the flush thread must see events recorded by a different thread");
  }
}
