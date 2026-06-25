package io.github.moonrunnerkc.nondet.scan;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Locates the compiled fixture classes on the test classpath. */
final class ScanFixtures {

  private static final String FIXTURE_PACKAGE = "/io/github/moonrunnerkc/nondet/scan/fixtures/";

  private ScanFixtures() {
  }

  static Path classFile(String simpleName) {
    final String resource = FIXTURE_PACKAGE + simpleName + ".class";
    try {
      return Paths.get(ScanFixtures.class.getResource(resource).toURI());
    } catch (final URISyntaxException cause) {
      throw new IllegalStateException("fixture resource path is malformed: " + resource, cause);
    } catch (final NullPointerException cause) {
      throw new IllegalStateException(
          "fixture " + simpleName + " is not on the test classpath; expected " + resource, cause);
    }
  }

  static Path directory() {
    return classFile("NanoTimeProbe").getParent();
  }
}
