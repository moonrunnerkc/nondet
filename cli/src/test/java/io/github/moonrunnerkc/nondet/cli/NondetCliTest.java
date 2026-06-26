package io.github.moonrunnerkc.nondet.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/**
 * The top command and each subcommand expose help with an example, and the version reads
 * from the manifest rather than a hardcoded string.
 */
class NondetCliTest {

  @Test
  void versionProviderProducesANondetBanner() {
    final String[] lines = new ManifestVersionProvider().getVersion();
    assertEquals(1, lines.length);
    assertTrue(lines[0].startsWith("nondet"), lines[0]);
  }

  @Test
  void topLevelHelpListsScanAndCheckWithDescriptions() {
    final Result help = run("--help");
    assertEquals(0, help.code());
    assertTrue(help.out().contains("scan"), help.out());
    assertTrue(help.out().contains("check"), help.out());
    assertTrue(help.out().contains("Statically list entropy call sites"), help.out());
  }

  @Test
  void checkHelpIncludesAUsageExample() {
    final Result help = run("check", "--help");
    assertEquals(0, help.code());
    assertTrue(help.out().contains("Example"), help.out());
    assertTrue(help.out().contains("FlakyRetry"), help.out());
    assertTrue(help.out().contains("--timeout"), help.out());
  }

  @Test
  void scanHelpIncludesAUsageExample() {
    final Result help = run("scan", "--help");
    assertEquals(0, help.code());
    assertTrue(help.out().contains("Example"), help.out());
    assertTrue(help.out().contains("nondet scan"), help.out());
  }

  private static Result run(String... args) {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final PrintStream original = System.out;
    final int code;
    try {
      System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
      code = new CommandLine(new NondetCli()).execute(args);
    } finally {
      System.setOut(original);
    }
    return new Result(code, out.toString(StandardCharsets.UTF_8));
  }

  private record Result(int code, String out) {
  }
}
