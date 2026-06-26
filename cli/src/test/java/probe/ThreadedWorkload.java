package probe;

/**
 * A workload that reads the clock from several worker threads, used to prove the report
 * notes when more than one thread produced events.
 *
 * <p>Each worker reads {@link System#nanoTime()} twice and the main thread joins them all,
 * so every read is recorded before the JVM exits. The cross-thread ordering of those reads
 * is what the checker calls out as approximate.
 */
public final class ThreadedWorkload {

  private static final int WORKERS = 3;

  private ThreadedWorkload() {
  }

  /**
   * Starts the workers, waits for them, and prints a fixed line.
   *
   * @param args ignored
   * @throws InterruptedException if joining a worker is interrupted
   */
  public static void main(String[] args) throws InterruptedException {
    final Thread[] threads = new Thread[WORKERS];
    for (int i = 0; i < WORKERS; i++) {
      threads[i] = new Thread(() -> {
        blackhole(System.nanoTime());
        blackhole(System.nanoTime());
      }, "worker-" + i);
      threads[i].start();
    }
    for (final Thread thread : threads) {
      thread.join();
    }
    System.out.println("workers-done");
  }

  private static void blackhole(long value) {
    if (value == Long.MIN_VALUE) {
      System.out.println("unreachable");
    }
  }
}
