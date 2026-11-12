package edu.nju.sehw.takeout.model.comment;

import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.List;
import java.util.Objects;

/**
 * Carries comment text, its review target, and the uploaded attachment identifiers.
 *
 * @param subject kind and identifiers of the shop, dish, or delivery service being reviewed
 * @param text comment or reply text supplied for validation, persistence, or display
 * @param attachmentIds ordered identifiers of resources already uploaded for this comment
 */
public record PublishCommentPayload(CommentSubject subject, String text, List<String> attachmentIds)
    implements MessagePayload {
  /**
   * Creates the PublishCommentPayload value from the components documented below.
   *
   * @param subject kind and identifiers of the shop, dish, or delivery service being reviewed
   * @param text comment or reply text supplied for validation, persistence, or display
   * @param attachmentIds ordered identifiers of resources already uploaded for this comment
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public PublishCommentPayload {
    Objects.requireNonNull(subject, "subject");
    Objects.requireNonNull(text, "text");
    Objects.requireNonNull(attachmentIds, "attachmentIds");
    attachmentIds = List.copyOf(attachmentIds);
  }

  /**
   * Creates a payload or message snapshot with independent mutable state.
   *
   * @return a separate snapshot preserving the same argument values and request identities
   */
  @Override
  public PublishCommentPayload deepCopy() {
    return new PublishCommentPayload(subject, text, attachmentIds);
  }
}
