package edu.nju.sehw.takeout.model.comment;

import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Objects;
import java.util.UUID;

/**
 * Carries the parent comment identifier and merchant reply text.
 *
 * @param commentId stable identifier of the comment being read or changed
 * @param text comment or reply text supplied for validation, persistence, or display
 */
public record ReplyCommentPayload(UUID commentId, String text) implements MessagePayload {
  /**
   * Creates the ReplyCommentPayload value from the components documented below.
   *
   * @param commentId stable identifier of the comment being read or changed
   * @param text comment or reply text supplied for validation, persistence, or display
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public ReplyCommentPayload {
    Objects.requireNonNull(commentId, "commentId");
    Objects.requireNonNull(text, "text");
  }

  /**
   * Creates a payload or message snapshot with independent mutable state.
   *
   * @return a separate snapshot preserving the same argument values and request identities
   */
  @Override
  public ReplyCommentPayload deepCopy() {
    return new ReplyCommentPayload(commentId, text);
  }
}
