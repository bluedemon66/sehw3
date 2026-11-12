package edu.nju.sehw.takeout.client.query;

import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.message.MessageCodec;
import edu.nju.sehw.takeout.model.network.SecureRequest;
import edu.nju.sehw.takeout.scaffold.NotImplementedYetException;
import java.time.Clock;
import java.util.Objects;

/**
 * Builds authenticated request envelopes from a client session, message codec, and clock.
 *
 * <p>The session supplies the credential, the codec supplies canonical message bytes, and the clock
 * supplies the request timestamp.
 */
public final class DefaultRequestSecurity implements RequestSecurity {
  /** Current client identity, cached permissions, credential, and expiry. */
  private final ClientSession session;

  /** Shared encoder and decoder that defines deterministic message bytes. */
  private final MessageCodec codec;

  /** Clock used to timestamp requests or evaluate session expiry. */
  private final Clock clock;

  /**
   * Creates this component with the dependencies documented below.
   *
   * @param session current client identity, cached permissions, credential, and expiry
   * @param codec shared encoder and decoder that defines deterministic message bytes
   * @param clock clock used to timestamp requests or evaluate session expiry
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public DefaultRequestSecurity(ClientSession session, MessageCodec codec, Clock clock) {
    this.session = Objects.requireNonNull(session, "session");
    this.codec = Objects.requireNonNull(codec, "codec");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /**
   * Encodes the message and attaches the session credential, request time, and body digest.
   *
   * @param message message envelope containing request identity, routing fields, operation code,
   *     and arguments
   * @return a request envelope whose request ID matches the supplied message
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  @Override
  public SecureRequest prepare(Message message) {
    throw new NotImplementedYetException("DefaultRequestSecurity.prepare");
  }

  /**
   * Computes the SHA-256 digest of the canonical encoded message bytes.
   *
   * @param body canonical encoded message bytes over which the SHA-256 digest is computed
   * @return the 32-byte digest of the supplied body
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  private byte[] sha256(byte[] body) {
    throw new NotImplementedYetException("DefaultRequestSecurity.sha256");
  }
}
