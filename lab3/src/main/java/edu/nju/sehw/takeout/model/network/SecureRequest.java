package edu.nju.sehw.takeout.model.network;

import edu.nju.sehw.takeout.model.message.Message;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Carries a request's credential, timestamp, message digest, and message snapshot across the
 * transport boundary.
 *
 * @param requestId identifier of this individual request; retries reuse the same identifier
 * @param accessToken credential presented to the server for session authentication
 * @param sentAt timestamp at which the client prepares the outgoing request
 * @param bodyDigest 32-byte SHA-256 digest of the canonically encoded message
 * @param message message envelope containing request identity, routing fields, operation code, and
 *     arguments
 */
public record SecureRequest(
    UUID requestId, AccessToken accessToken, Instant sentAt, byte[] bodyDigest, Message message) {
  /**
   * Creates the SecureRequest value from the components documented below.
   *
   * @param requestId identifier of this individual request; retries reuse the same identifier
   * @param accessToken credential presented to the server for session authentication
   * @param sentAt timestamp at which the client prepares the outgoing request
   * @param bodyDigest 32-byte SHA-256 digest of the canonically encoded message
   * @param message message envelope containing request identity, routing fields, operation code,
   *     and arguments
   * @throws NullPointerException if a required component, dependency, or collection element is null
   * @throws IllegalArgumentException if the digest length is not 32 bytes or the envelope and
   *     message request IDs disagree
   */
  public SecureRequest {
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(accessToken, "accessToken");
    Objects.requireNonNull(sentAt, "sentAt");
    Objects.requireNonNull(bodyDigest, "bodyDigest");
    bodyDigest = bodyDigest.clone();
    Objects.requireNonNull(message, "message");
    if (bodyDigest.length != 32 || !requestId.equals(message.requestId())) {
      throw new IllegalArgumentException("报文摘要或请求标识不匹配");
    }
  }

  /**
   * Returns a defensive copy of the stored 32-byte message digest.
   *
   * @return a new array containing the stored 32-byte message digest
   */
  @Override
  public byte[] bodyDigest() {
    return bodyDigest.clone();
  }
}
