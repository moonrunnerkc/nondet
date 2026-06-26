package probe;

import java.util.concurrent.CountDownLatch;

/**
 * A workload that never terminates, used to prove a hanging run is killed at the timeout
 * instead of hanging the checker.
 */
public final class HangingWorkload {

  private HangingWorkload() {
  }

  /**
   * Blocks forever on a latch that is never counted down.
   *
   * @param args ignored
   * @throws InterruptedException if the blocking wait is interrupted
   */
  public static void main(String[] args) throws InterruptedException {
    new CountDownLatch(1).await();
  }
}
