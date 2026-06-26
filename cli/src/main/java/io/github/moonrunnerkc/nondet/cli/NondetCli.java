package io.github.moonrunnerkc.nondet.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Top level command for the nondet tool.
 *
 * <p>With no subcommand it prints usage. The real work lives in {@code scan} and
 * {@code check}. The version comes from the jar manifest through
 * {@link ManifestVersionProvider}, so it is never hardcoded.
 */
@Command(
    name = "nondet",
    mixinStandardHelpOptions = true,
    versionProvider = ManifestVersionProvider.class,
    subcommands = {ScanCommand.class, CheckCommand.class},
    description = "Find the call site where a JVM program stops being reproducible.",
    footerHeading = "%nExamples:%n",
    footer = {
      "  nondet scan examples/target/classes",
      "  nondet check --class-path examples/target/classes \\",
      "    io.github.moonrunnerkc.nondet.examples.FlakyRetry",
      "",
      "Exit codes: 0 no divergence (or no reads), 1 divergence, 2 usage error, 3 execution error."
    })
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
