package edu.nju.sehw.takeout.model.comment;

import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Objects;
import java.util.UUID;

/**
 * Carries the comment identifier and moderation reason for a platform hide operation.
 *
 * @param commentId stable identifier of the comment being read or changed
 * @param reason moderation explanation recorded for the platform hide operation
 */
public record HideCommentPayload(UUID commentId, String reason) implements MessagePayload {
  /**
   * Creates the HideCommentPayload value from the components documented below.
   *
   * @param commentId stable identifier of the comment being read or changed
   * @param reason moderation explanation recorded for the platform hide operation
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public HideCommentPayload {
    Objects.requireNonNull(commentId, "commentId");
    Objects.requireNonNull(reason, "reason");
  }

  /**
   * Creates a payload or message snapshot with independent mutable state.
   *
   * @return a separate snapshot preserving the same argument values and request identities
   */
  @Override
  public HideCommentPayload deepCopy() {
    return new HideCommentPayload(commentId, reason);
  }
}
