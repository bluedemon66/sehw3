package edu.nju.sehw.takeout.model.comment;

import edu.nju.sehw.takeout.model.common.ResultStatus;
import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Objects;
import java.util.UUID;

/**
 * Associates a comment operation's result status and payload with its remote request.
 *
 * @param requestId identifier of this individual request; retries reuse the same identifier
 * @param operation comment operation selected from the comment protocol catalogue
 * @param status business or protocol outcome of the server operation
 * @param payload typed operation arguments or result data from which an isolated snapshot is taken
 */
public record CommentResult(
    UUID requestId, CommentOperation operation, ResultStatus status, MessagePayload payload) {
  /**
   * Creates the CommentResult value from the components documented below.
   *
   * @param requestId identifier of this individual request; retries reuse the same identifier
   * @param operation comment operation selected from the comment protocol catalogue
   * @param status business or protocol outcome of the server operation
   * @param payload typed operation arguments or result data from which an isolated snapshot is
   *     taken
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public CommentResult {
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(operation, "operation");
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
