package edu.nju.sehw.takeout.model.comment;

import edu.nju.sehw.takeout.model.common.UserId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Stores a merchant reply, its parent comment, and its visibility state.
 *
 * @param replyId stable identifier of the merchant reply
 * @param commentId stable identifier of the comment being read or changed
 * @param merchantId identifier of the merchant who wrote the reply
 * @param text comment or reply text supplied for validation, persistence, or display
 * @param createdAt instant at which the represented persistent record was created
 * @param visible whether this reply is eligible for display with its parent comment
 */
public record CommentReply(
    UUID replyId,
    UUID commentId,
    UserId merchantId,
    String text,
    Instant createdAt,
    boolean visible) {
  /**
   * Creates the CommentReply value from the components documented below.
   *
   * @param replyId stable identifier of the merchant reply
   * @param commentId stable identifier of the comment being read or changed
   * @param merchantId identifier of the merchant who wrote the reply
   * @param text comment or reply text supplied for validation, persistence, or display
   * @param createdAt instant at which the represented persistent record was created
   * @param visible whether this reply is eligible for display with its parent comment
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public CommentReply {
    Objects.requireNonNull(replyId, "replyId");
    Objects.requireNonNull(commentId, "commentId");
    Objects.requireNonNull(merchantId, "merchantId");
    Objects.requireNonNull(text, "text");
    Objects.requireNonNull(createdAt, "createdAt");
  }
}
