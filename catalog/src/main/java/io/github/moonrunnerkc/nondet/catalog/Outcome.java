package io.github.moonrunnerkc.nondet.catalog;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * A deterministic fingerprint of what a run produced, independent of the entropy it read.
 *
 * <p>An outcome answers a different question than a trace. A trace says which entropy values a
 * run read; an outcome says what the run produced. Two runs agree on outcome when their
 * fingerprints are equal, even if every clock and random value behind them differed. That is the
 * pivot of causal attribution: a read only matters when forcing it changes the outcome, not
 * merely because its raw value varies between runs.
 *
 * <p>The fingerprint is the SHA-256 of the result surface in a fixed, length framed byte order:
 * the exit code, then standard output, then standard error, then an optional declared result. The
 * framing makes the encoding unambiguous, so {@code (stdout="a", stderr="b")} never collides with
 * {@code (stdout="ab", stderr="")}. The {@link #components()} set records which parts were present.
 *
 * @param fingerprint the lower case hex SHA-256 over the result surface, never {@code null}
 * @param components  the parts of the surface that fed the fingerprint, never {@code null}
 */
public record Outcome(String fingerprint, Set<OutcomeComponent> components) {

  /**
   * Validates the fields and freezes the component set in declaration order.
   *
   * @throws NullPointerException if {@code fingerprint} or {@code components} is {@code null}
   * @throws IllegalArgumentException if {@code components} is empty
   */
  public Outcome {
    Objects.requireNonNull(fingerprint, "fingerprint is required for an outcome");
    Objects.requireNonNull(components, "components is required for an outcome");
    if (components.isEmpty()) {
      throw new IllegalArgumentException(
          "an outcome must fingerprint at least one component; exit, stdout, and stderr are always present");
    }
    components = Collections.unmodifiableSet(EnumSet.copyOf(components));
  }

  /**
   * Fingerprints a run's result surface.
   *
   * <p>Exit code, standard output, and standard error are always part of the fingerprint. A
   * declared result is included only when {@code declaredResult} is non-null, which is how a
   * workload that publishes nothing and one that publishes an empty string stay distinguishable.
   *
   * @param exitCode       the process exit code
   * @param stdout         the bytes written to standard output, never {@code null}
   * @param stderr         the bytes written to standard error, never {@code null}
   * @param declaredResult the bytes of an explicit published result, or {@code null} when the
   *     workload published none
   * @return the outcome fingerprint over those parts
   * @throws NullPointerException if {@code stdout} or {@code stderr} is {@code null}
   * @throws IllegalStateException if SHA-256 is unavailable in the running JVM
   */
  public static Outcome of(int exitCode, byte[] stdout, byte[] stderr, byte[] declaredResult) {
    Objects.requireNonNull(stdout, "stdout bytes are required; pass an empty array for no output");
    Objects.requireNonNull(stderr, "stderr bytes are required; pass an empty array for no output");
    final MessageDigest digest = sha256();
    final EnumSet<OutcomeComponent> present = EnumSet.of(
        OutcomeComponent.EXIT, OutcomeComponent.STDOUT, OutcomeComponent.STDERR);
    frame(digest, 'E', intBytes(exitCode));
    frame(digest, 'O', stdout);
    frame(digest, 'R', stderr);
    if (declaredResult != null) {
      frame(digest, 'D', declaredResult);
      present.add(OutcomeComponent.DECLARED_RESULT);
    }
    return new Outcome(hex(digest.digest()), present);
  }

  /**
   * Reports whether two runs produced the same outcome.
   *
   * @param other the outcome to compare against, never {@code null}
   * @return {@code true} when both fingerprints are equal
   */
  public boolean agreesWith(Outcome other) {
    return fingerprint.equals(other.fingerprint);
  }

  private static void frame(MessageDigest digest, char tag, byte[] payload) {
    digest.update((byte) tag);
    digest.update(longBytes(payload.length));
    digest.update(payload);
  }

  private static byte[] intBytes(int value) {
    return new byte[] {
      (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value
    };
  }

  private static byte[] longBytes(long value) {
    final byte[] out = new byte[8];
    for (int i = 7; i >= 0; i--) {
      out[i] = (byte) value;
      value >>= 8;
    }
    return out;
  }

  private static String hex(byte[] bytes) {
    final StringBuilder out = new StringBuilder(bytes.length * 2);
    for (final byte b : bytes) {
      out.append(Character.forDigit((b >> 4) & 0xF, 16));
      out.append(Character.forDigit(b & 0xF, 16));
    }
    return out.toString();
  }

  private static MessageDigest sha256() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (final NoSuchAlgorithmException cause) {
      throw new IllegalStateException(
          "SHA-256 is required to fingerprint an outcome but is missing from this JVM", cause);
    }
  }
}
