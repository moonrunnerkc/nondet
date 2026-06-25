package io.github.moonrunnerkc.nondet.agent;

import io.github.moonrunnerkc.nondet.catalog.Catalog;
import java.lang.instrument.Instrumentation;

/**
 * Premain entry point for the nondet instrumentation agent.
 *
 * <p>The agent installs the entropy transformer and a shutdown hook that flushes the
 * trace and call site registry. Attach it with
 * {@code -javaagent:nondet-agent.jar} and point the output at a file with
 * {@code -Dnondet.trace.out=<path>}. The agent never throws back into the launching JVM;
 * a failed transform is dropped so the target program runs unchanged.
 */
public final class Agent {

  private Agent() {
  }

  /**
   * Installs the transformer and the shutdown flush.
   *
   * @param agentArgs the agent argument string, unused in v0.1.0
   * @param instrumentation the instrumentation handle supplied by the JVM, never {@code null}
   */
  public static void premain(String agentArgs, Instrumentation instrumentation) {
    Registry.installShutdownHook();
    instrumentation.addTransformer(new EntropyTransformer(Catalog.ofDefault()), true);
  }
}
