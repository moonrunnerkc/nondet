package io.github.moonrunnerkc.nondet.catalog;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Derives a stable identifier for an entropy call site from its source coordinates.
 *
 * <p>The id is a function of the declaring class, method, line, and api label only.
 * It does not depend on class load order, a running counter, or wall clock time, so
 * the static scanner and the dynamic agent compute the same id for the same site and
 * two runs over the same input produce identical ids.
 */
public final class CallSiteId {

  private static final int ID_HEX_LENGTH = 16;

  private CallSiteId() {
  }

  /**
   * Computes the stable id for a call site.
   *
   * <p>The id is the first {@value #ID_HEX_LENGTH} hex characters of the SHA-256 of a
   * canonical coordinate string. Truncation keeps trace lines compact; collisions are
   * negligible across a single program at v0.1.0 scope.
   *
   * @param declaringClass the declaring class in internal form, for example
   *     {@code io/github/moonrunnerkc/nondet/examples/FlakyRetry}, never {@code null}
   * @param method         the enclosing method name, never {@code null}
   * @param line           the source line of the call, or a negative value when unknown
   * @param api            the catalog api label, for example {@code System.nanoTime}, never {@code null}
   * @return a lower case hex id of length {@value #ID_HEX_LENGTH}
   * @throws IllegalStateException if SHA-256 is unavailable in the running JVM
   */
  public static String of(String declaringClass, String method, int line, String api) {
    final String canonical = declaringClass + '#' + method + ':' + line + '@' + api;
    final byte[] digest = sha256(canonical.getBytes(StandardCharsets.UTF_8));
    final StringBuilder hex = new StringBuilder(ID_HEX_LENGTH);
    for (int i = 0; hex.length() < ID_HEX_LENGTH; i++) {
      hex.append(Character.forDigit((digest[i] >> 4) & 0xF, 16));
      hex.append(Character.forDigit(digest[i] & 0xF, 16));
    }
    return hex.toString();
  }

  private static byte[] sha256(byte[] input) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(input);
    } catch (final NoSuchAlgorithmException cause) {
      throw new IllegalStateException(
          "SHA-256 is required to derive call site ids but is missing from this JVM", cause);
    }
  }
}
