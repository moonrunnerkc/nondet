package io.github.moonrunnerkc.nondet.agent;

import io.github.moonrunnerkc.nondet.catalog.Category;
import io.github.moonrunnerkc.nondet.catalog.Event;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects entropy reads at run time into a per thread buffer with a global sequence.
 *
 * <p>Each thread appends to its own list, so the hot path has no lock contention. Every
 * thread's buffer is registered in a shared queue the moment that thread first records,
 * so {@link #snapshot()} sees events from all threads, not just the caller's. A single
 * atomic counter stamps every event with a global sequence number that defines the total
 * order across threads, and {@link #snapshot()} merges the buffers and sorts by it. That
 * is what lets the shutdown hook, which runs on its own thread, flush a trace recorded by
 * the workload threads.
 *
 * <p>Recording is capped so an unbounded loop over an instrumented source cannot exhaust
 * memory. The cap comes from {@value #MAX_EVENTS_PROPERTY}, defaulting to
 * {@value #DEFAULT_MAX_EVENTS}. Once the cap is reached further reads are dropped and
 * {@link #truncated()} reports {@code true}, so a caller can say the trace is a prefix of
 * what the run actually read.
 *
 * <p>This class is pure JDK by design. The instrumented program calls into it from the
 * rewritten call sites, so it must resolve without ASM or any third party type on the
 * class path. State is static because one recorder serves the whole instrumented JVM.
 *
 * <p>{@link #snapshot()} reads the buffers without locking and assumes the recording
 * threads are quiescent, which holds when it runs from the shutdown hook after the
 * workload finishes.
 */
public final class Recorder {

  /** System property naming the per run cap on recorded events. */
  public static final String MAX_EVENTS_PROPERTY = "nondet.max.events";

  /** The default cap when {@value #MAX_EVENTS_PROPERTY} is unset or unparseable. */
  public static final long DEFAULT_MAX_EVENTS = 1_000_000L;

  private static final AtomicLong SEQUENCE = new AtomicLong();
  private static final AtomicBoolean TRUNCATED = new AtomicBoolean(false);
  private static volatile long maxEvents = readMaxEvents();
  private static final Queue<List<Event>> BUFFERS = new ConcurrentLinkedQueue<>();
  private static final ThreadLocal<List<Event>> THREAD_BUFFER = ThreadLocal.withInitial(() -> {
    final List<Event> buffer = new ArrayList<>();
    BUFFERS.add(buffer);
    return buffer;
  });

  private Recorder() {
  }

  /**
   * Records one entropy read on the current thread, unless the event cap is reached.
   *
   * <p>The first {@link #maxEvents} reads are buffered. Once the global sequence reaches
   * the cap the read is dropped and the run is marked truncated; the counter keeps
   * advancing but no further memory is used, so an unbounded loop stays bounded.
   *
   * @param category   the kind of entropy that produced the value, never {@code null}
   * @param callSiteId the stable id of the originating call site, never {@code null}
   * @param value      the read value rendered as text, never {@code null}
   */
  public static void record(Category category, String callSiteId, String value) {
    final long seq = SEQUENCE.getAndIncrement();
    if (seq >= maxEvents) {
      TRUNCATED.set(true);
      return;
    }
    THREAD_BUFFER.get().add(new Event(seq, category, callSiteId, value));
  }

  /**
   * Reports whether this run hit the event cap and dropped reads.
   *
   * @return {@code true} when at least one read was dropped because the cap was reached
   */
  public static boolean truncated() {
    return TRUNCATED.get();
  }

  /**
   * Returns the number of threads that recorded at least one event.
   *
   * <p>Each recording thread owns one buffer, so a buffer with any events stands for a
   * thread that produced output. A count above one means the trace interleaves reads from
   * several threads, whose ordering across threads is itself nondeterministic.
   *
   * @return the count of threads that produced at least one event, zero when nothing was read
   */
  public static int threadCount() {
    int active = 0;
    for (final List<Event> buffer : BUFFERS) {
      if (!buffer.isEmpty()) {
        active++;
      }
    }
    return active;
  }

  /**
   * Returns every recorded event in global sequence order.
   *
   * @return a new list of all events seen so far, ordered by sequence number
   */
  public static List<Event> snapshot() {
    final List<Event> merged = new ArrayList<>();
    for (final List<Event> buffer : BUFFERS) {
      merged.addAll(buffer);
    }
    merged.sort((left, right) -> Long.compare(left.seq(), right.seq()));
    return merged;
  }

  /**
   * Clears all recorded state and re-reads the event cap. Visible for testing.
   */
  static void reset() {
    BUFFERS.clear();
    THREAD_BUFFER.remove();
    SEQUENCE.set(0);
    TRUNCATED.set(false);
    maxEvents = readMaxEvents();
  }

  private static long readMaxEvents() {
    final String raw = System.getProperty(MAX_EVENTS_PROPERTY);
    if (raw == null || raw.isBlank()) {
      return DEFAULT_MAX_EVENTS;
    }
    try {
      final long parsed = Long.parseLong(raw.trim());
      return parsed > 0 ? parsed : DEFAULT_MAX_EVENTS;
    } catch (final NumberFormatException ignored) {
      return DEFAULT_MAX_EVENTS;
    }
  }
}
