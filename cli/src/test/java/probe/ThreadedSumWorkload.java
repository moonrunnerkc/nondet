package probe;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A threaded workload whose outcome is a clock-derived sum across worker threads.
 *
 * <p>Each of several workers reads {@link System#nanoTime()} once and adds the low digits of that
 * read to a shared total. The total depends on the values read, not the order they were added in, so
 * replaying the recorded reads reproduces the same total whichever way the threads interleave. That
 * makes it a clean threaded-replay fixture: the outcome comes back identical under replay even
 * though the live run's reads came from several threads at once.
 */
public final class ThreadedSumWorkload {

  private static final int WORKERS = 4;

  private ThreadedSumWorkload() {
  }

  /**
   * Sums the low digits of one clock read per worker and prints the total.
   *
   * @param args ignored
   * @throws InterruptedException if joining a worker is interrupted
   */
  public static void main(String[] args) throws InterruptedException {
    final AtomicLong total = new AtomicLong();
    final Thread[] workers = new Thread[WORKERS];
    for (int i = 0; i < WORKERS; i++) {
      workers[i] = new Thread(() -> total.addAndGet(Math.floorMod(System.nanoTime(), 1000L)));
      workers[i].start();
    }
    for (final Thread worker : workers) {
      worker.join();
    }
    System.out.println("sum-mod:" + total.get());
  }
}
