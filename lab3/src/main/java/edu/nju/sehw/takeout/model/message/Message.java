package edu.nju.sehw.takeout.model.message;

import java.util.Objects;
import java.util.UUID;

/**
 * Carries a request snapshot with local routing identifiers, an operation code, and a copied
 * payload.
 *
 * <p>requestId identifies one request and its retries. correlationId associates related child
 * requests with the same root UI operation.
 *
 * @param requestId identifier of this individual request; retries reuse the same identifier
 * @param correlationId identifier shared by the child requests of one root UI operation
 * @param senderId registered identifier of the participant sending the message
 * @param targetId registered identifier of the participant that should receive the message
 * @param operationCode numeric identifier selecting the operation to execute
 * @param payload typed operation arguments or result data from which an isolated snapshot is taken
 */
public record Message(
    UUID requestId,
    UUID correlationId,
    int senderId,
    int targetId,
    int operationCode,
    MessagePayload payload) {
  /**
   * Creates an isolated message snapshot with separate request and root-correlation identifiers.
   *
   * @param requestId identifier of this individual request; retries reuse the same identifier
   * @param correlationId identifier shared by the child requests of one root UI operation
   * @param senderId registered identifier of the participant sending the message
   * @param targetId registered identifier of the participant that should receive the message
   * @param operationCode numeric identifier selecting the operation to execute
   * @param payload typed operation arguments or result data from which an isolated snapshot is
   *     taken
   * @throws NullPointerException if a required component, dependency, or collection element is null
   * @throws IllegalArgumentException if a sender, target, or operation identifier is nonpositive
   */
  public Message {
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(correlationId, "correlationId");
    Objects.requireNonNull(payload, "payload");
    payload = Objects.requireNonNull(payload.deepCopy(), "payload snapshot");
    if (senderId <= 0 || targetId <= 0 || operationCode <= 0) {
      throw new IllegalArgumentException("消息编号须为正整数");
    }
  }

  /**
   * Creates a single-step message whose correlation ID equals its request ID.
   *
   * @param requestId identifier of this individual request; retries reuse the same identifier
   * @param senderId registered identifier of the participant sending the message
   * @param targetId registered identifier of the participant that should receive the message
   * @param operationCode numeric identifier selecting the operation to execute
   * @param payload typed operation arguments or result data from which an isolated snapshot is
   *     taken
   * @throws IllegalArgumentException if a sender, target, or operation identifier is nonpositive
   */
  public Message(
      UUID requestId, int senderId, int targetId, int operationCode, MessagePayload payload) {
    this(requestId, requestId, senderId, targetId, operationCode, payload);
  }

  /**
   * Returns an isolated snapshot of the stored operation data.
   *
   * @return a copy of the stored typed payload
   */
  @Override
  public MessagePayload payload() {
    return payload.deepCopy();
  }

  /**
   * Creates a payload or message snapshot with independent mutable state.
   *
   * @return a separate snapshot preserving the same argument values and request identities
   */
  public Message deepCopy() {
    return new Message(requestId, correlationId, senderId, targetId, operationCode, payload);
  }
}
