package io.github.moonrunnerkc.nondet.scan;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Reads the class files to scan from a directory tree, a jar, or a single class file.
 *
 * <p>Entries come back sorted by origin so a scan over the same input is reproducible.
 * Only {@code .class} files are returned; anything else in a directory or jar is
 * ignored.
 */
public final class ClassSource {

  private static final String CLASS_SUFFIX = ".class";

  private ClassSource() {
  }

  /**
   * Reads every class file reachable from a path.
   *
   * <p>A directory is walked recursively for {@code .class} files. A {@code .jar} or
   * {@code .zip} file is read entry by entry. A path that is itself a {@code .class}
   * file yields that one class.
   *
   * @param path the directory, archive, or class file to read, never {@code null}
   * @return the class files found, sorted by origin, possibly empty
   * @throws IOException if the path does not exist or cannot be read
   * @throws IllegalArgumentException if the path is a regular file that is neither a
   *     class file nor a jar or zip archive
   */
  public static List<ClassEntry> read(Path path) throws IOException {
    if (!Files.exists(path)) {
      throw new IOException("scan path does not exist: " + path
          + "; pass a classes directory, a jar, or a .class file");
    }
    if (Files.isDirectory(path)) {
      return readDirectory(path);
    }
    final String name = path.getFileName().toString();
    if (name.endsWith(CLASS_SUFFIX)) {
      return List.of(new ClassEntry(path.toString(), Files.readAllBytes(path)));
    }
    if (name.endsWith(".jar") || name.endsWith(".zip")) {
      return readArchive(path);
    }
    throw new IllegalArgumentException("cannot scan " + path
        + "; expected a directory, a .jar, a .zip, or a .class file");
  }

  private static List<ClassEntry> readDirectory(Path root) throws IOException {
    final List<ClassEntry> entries = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(root)) {
      walk.filter(Files::isRegularFile)
          .filter(file -> file.getFileName().toString().endsWith(CLASS_SUFFIX))
          .sorted()
          .forEach(file -> entries.add(readRegularFile(file)));
    }
    return entries;
  }

  private static ClassEntry readRegularFile(Path file) {
    try {
      return new ClassEntry(file.toString(), Files.readAllBytes(file));
    } catch (final IOException cause) {
      throw new UncheckedIOException("failed to read class file " + file, cause);
    }
  }

  private static List<ClassEntry> readArchive(Path archive) throws IOException {
    final List<ClassEntry> entries = new ArrayList<>();
    try (InputStream in = Files.newInputStream(archive);
         ZipInputStream zip = new ZipInputStream(in)) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        if (!entry.isDirectory() && entry.getName().endsWith(CLASS_SUFFIX)) {
          entries.add(new ClassEntry(archive + "!" + entry.getName(), zip.readAllBytes()));
        }
      }
    }
    entries.sort(Comparator.comparing(ClassEntry::origin));
    return entries;
  }
}
