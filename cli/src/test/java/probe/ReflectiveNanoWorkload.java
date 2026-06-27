package probe;

import java.lang.reflect.Method;

/**
 * A workload that reads {@link System#nanoTime()} through reflection rather than directly.
 *
 * <p>The static rewrite matches direct call operands, so a clock read dispatched through
 * {@code Method.invoke} would be invisible to it. This fixture reads the clock exactly that way and
 * lets the value reach the output, so the checker must record the reflective read, attribute it to
 * this caller, and reproduce it under replay.
 */
public final class ReflectiveNanoWorkload {

  private ReflectiveNanoWorkload() {
  }

  /**
   * Reads the clock through reflection and prints a line derived from it.
   *
   * @param args ignored
   * @throws ReflectiveOperationException if the reflective call fails
   */
  public static void main(String[] args) throws ReflectiveOperationException {
    final Method nanoTime = System.class.getMethod("nanoTime");
    final long sampled = (long) nanoTime.invoke(null);
    if (sampled % 2L == 0L) {
      System.out.println("R-EVEN-" + sampled);
    } else {
      System.out.println("R-ODD-" + sampled);
    }
  }
}
