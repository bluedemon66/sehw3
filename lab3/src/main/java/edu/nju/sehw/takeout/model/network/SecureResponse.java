package edu.nju.sehw.takeout.model.network;

import edu.nju.sehw.takeout.model.common.ResultStatus;
import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Objects;
import java.util.UUID;

/**
 * Carries a remote request identifier, result status, and copied response payload.
 *
 * @param requestId identifier of this individual request; retries reuse the same identifier
 * @param status business or protocol outcome of the server operation
 * @param payload typed operation arguments or result data from which an isolated snapshot is taken
 */
public record SecureResponse(UUID requestId, ResultStatus status, MessagePayload payload) {
  /**
   * Creates the SecureResponse value from the components documented below.
   *
   * @param requestId identifier of this individual request; retries reuse the same identifier
   * @param status business or protocol outcome of the server operation
   * @param payload typed operation arguments or result data from which an isolated snapshot is
   *     taken
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public SecureResponse {
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(status, "status");
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
