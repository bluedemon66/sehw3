package edu.nju.sehw.takeout.model.message;

import java.util.Objects;
import java.util.UUID;

/**
 * Carries the request identifier, operation code, and copied arguments of one planned data
 * operation.
 *
 * @param requestId identifier of this individual request; retries reuse the same identifier
 * @param operationCode numeric identifier selecting the operation to execute
 * @param payload typed operation arguments or result data from which an isolated snapshot is taken
 */
public record OperationRequest(UUID requestId, int operationCode, MessagePayload payload) {
  /**
   * Creates the OperationRequest value from the components documented below.
   *
   * @param requestId identifier of this individual request; retries reuse the same identifier
   * @param operationCode numeric identifier selecting the operation to execute
   * @param payload typed operation arguments or result data from which an isolated snapshot is
   *     taken
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public OperationRequest {
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(payload, "payload");
    payload = Objects.requireNonNull(payload.deepCopy(), "payload snapshot");
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
}
