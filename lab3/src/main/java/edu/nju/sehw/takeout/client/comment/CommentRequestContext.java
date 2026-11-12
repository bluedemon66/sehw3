package edu.nju.sehw.takeout.client.comment;

import edu.nju.sehw.takeout.model.comment.CommentOperation;
import edu.nju.sehw.takeout.model.comment.CommentSubject;
import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Keeps the originating UI, comment command, and attachment identifiers for one client comment
 * operation.
 *
 * @param requestId identifier of this individual request; retries reuse the same identifier
 * @param correlationId identifier shared by the child requests of one root UI operation
 * @param sourceUiId registered participant identifier of the UI that should receive the final page
 * @param operation comment operation selected from the comment protocol catalogue
 * @param payload typed operation arguments or result data from which an isolated snapshot is taken
 * @param subject kind and identifiers of the shop, dish, or delivery service being reviewed
 * @param text comment or reply text supplied for validation, persistence, or display
 * @param attachmentIds ordered identifiers of resources already uploaded for this comment
 */
public record CommentRequestContext(
    UUID requestId,
    UUID correlationId,
    int sourceUiId,
    CommentOperation operation,
    MessagePayload payload,
    CommentSubject subject,
    String text,
    List<String> attachmentIds) {
  /**
   * Creates the CommentRequestContext value from the components documented below.
   *
   * @param requestId identifier of this individual request; retries reuse the same identifier
   * @param correlationId identifier shared by the child requests of one root UI operation
   * @param sourceUiId registered participant identifier of the UI that should receive the final
   *     page
   * @param operation comment operation selected from the comment protocol catalogue
   * @param payload typed operation arguments or result data from which an isolated snapshot is
   *     taken
   * @param subject kind and identifiers of the shop, dish, or delivery service being reviewed
   * @param text comment or reply text supplied for validation, persistence, or display
   * @param attachmentIds ordered identifiers of resources already uploaded for this comment
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public CommentRequestContext {
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(correlationId, "correlationId");
    Objects.requireNonNull(operation, "operation");
    Objects.requireNonNull(payload, "payload");
    payload = Objects.requireNonNull(payload.deepCopy(), "payload snapshot");
    Objects.requireNonNull(subject, "subject");
    Objects.requireNonNull(text, "text");
    Objects.requireNonNull(attachmentIds, "attachmentIds");
    attachmentIds = List.copyOf(attachmentIds);
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
