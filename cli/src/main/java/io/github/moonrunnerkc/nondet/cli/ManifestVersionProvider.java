package io.github.moonrunnerkc.nondet.cli;

import picocli.CommandLine.IVersionProvider;

/**
 * Supplies {@code --version} from the jar manifest rather than a hardcoded string.
 *
 * <p>It reads {@code Implementation-Version} off the package that loaded the CLI, which the
 * jar plugin fills in from the project version at build time. When the classes are run
 * straight from a build directory there is no manifest, so the version reads as unknown and
 * the message says to run from the built jar.
 */
public final class ManifestVersionProvider implements IVersionProvider {

  /**
   * Returns the version banner lines for {@code --version}.
   *
   * @return a single line, {@code nondet <version>} when the manifest carries a version,
   *     otherwise a line explaining the version is only stamped into the built jar
   */
  @Override
  public String[] getVersion() {
    final Package pkg = ManifestVersionProvider.class.getPackage();
    final String version = pkg == null ? null : pkg.getImplementationVersion();
    if (version == null || version.isBlank()) {
      return new String[] {"nondet (version unknown; run from the built nondet-cli.jar to see it)"};
    }
    return new String[] {"nondet " + version};
  }
}
