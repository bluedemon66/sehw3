package edu.nju.sehw.takeout.model.comment;

import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Objects;
import java.util.UUID;

/**
 * Carries the identifier of a comment selected for author withdrawal.
 *
 * @param commentId stable identifier of the comment being read or changed
 */
public record WithdrawCommentPayload(UUID commentId) implements MessagePayload {
  /**
   * Creates the WithdrawCommentPayload value from the components documented below.
   *
   * @param commentId stable identifier of the comment being read or changed
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public WithdrawCommentPayload {
    Objects.requireNonNull(commentId, "commentId");
  }

  /**
   * Creates a payload or message snapshot with independent mutable state.
   *
   * @return a separate snapshot preserving the same argument values and request identities
   */
  @Override
  public WithdrawCommentPayload deepCopy() {
    return new WithdrawCommentPayload(commentId);
  }
}
