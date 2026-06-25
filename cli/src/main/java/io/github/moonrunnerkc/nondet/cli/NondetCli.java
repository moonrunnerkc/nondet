package io.github.moonrunnerkc.nondet.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Top level command for the nondet tool.
 *
 * <p>With no subcommand it prints usage. The real work lives in {@code scan} and
 * {@code check}.
 */
@Command(
    name = "nondet",
    mixinStandardHelpOptions = true,
    version = "nondet 0.1.0",
    subcommands = {ScanCommand.class, CheckCommand.class},
    description = "Find the call site where a JVM program stops being reproducible.")
public final class NondetCli implements Runnable {

  /**
   * Prints usage when invoked without a subcommand.
   */
  @Override
  public void run() {
    new CommandLine(this).usage(System.out);
  }

  /**
   * Parses arguments, runs the selected subcommand, and exits with its status code.
   *
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    System.exit(new CommandLine(new NondetCli()).execute(args));
  }
}
