package edu.nju.sehw.takeout.server.security;

import edu.nju.sehw.takeout.model.message.MessageCodec;
import edu.nju.sehw.takeout.model.network.MessageDigests;
import edu.nju.sehw.takeout.model.network.SecureRequest;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Objects;

/** Validates a request envelope against the authoritative session store and message codec. */
public final class DefaultServerRequestVerifier implements ServerRequestVerifier {
  /** Authoritative server-side lookup from credentials to verified principals. */
  private final ServerSessionStore sessions;

  /** Shared encoder and decoder that defines deterministic message bytes. */
  private final MessageCodec codec;

  /** Clock used to timestamp requests or evaluate session expiry. */
  private final Clock clock;

  /**
   * Creates this component with the dependencies documented below.
   *
   * @param sessions authoritative server-side lookup from credentials to verified principals
   * @param codec shared encoder and decoder that defines deterministic message bytes
   * @param clock clock used to timestamp requests or evaluate session expiry
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public DefaultServerRequestVerifier(
      ServerSessionStore sessions, MessageCodec codec, Clock clock) {
    this.sessions = Objects.requireNonNull(sessions, "sessions");
    this.codec = Objects.requireNonNull(codec, "codec");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /**
   * Validates the request and resolves its authoritative session identity and permissions.
   *
   * @param request outgoing or received request envelope containing credential, timestamp, digest,
   *     and message
   * @return the server-authenticated principal, permissions, and request identifier
   */
  @Override
  public ServerRequestContext verify(SecureRequest request) {
    validateEnvelope(request);
    var principal =
        sessions
            .find(request.accessToken())
            .filter(session -> session.expiresAt().isAfter(clock.instant()))
            .orElseThrow(() -> new SecurityException("Unknown or expired session"));
    return new ServerRequestContext(
        principal.userId(), principal.permissions(), request.requestId());
  }

  /**
   * Checks the request envelope, timestamp, and encoded-message digest before task creation.
   *
   * @param request outgoing or received request envelope containing credential, timestamp, digest,
   *     and message
   */
  private void validateEnvelope(SecureRequest request) {
    var now = clock.instant();
    if (!request.requestId().equals(request.message().requestId())
        || request.sentAt().isBefore(now.minusSeconds(300))
        || request.sentAt().isAfter(now.plusSeconds(30))
        || !MessageDigest.isEqual(
            request.bodyDigest(), MessageDigests.sha256(codec.encode(request.message())))) {
      throw new SecurityException("Invalid request envelope");
    }
  }
}
