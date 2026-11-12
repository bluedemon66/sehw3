package edu.nju.sehw.takeout.model.comment;

import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Objects;

/**
 * Carries one comment display projection as a message payload.
 *
 * @param comment comment display projection returned by the server
 */
public record CommentViewPayload(CommentView comment) implements MessagePayload {
  /**
   * Creates the CommentViewPayload value from the components documented below.
   *
   * @param comment comment display projection returned by the server
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public CommentViewPayload {
    Objects.requireNonNull(comment, "comment");
  }

  /**
   * Creates a payload or message snapshot with independent mutable state.
   *
   * @return a separate snapshot preserving the same argument values and request identities
   */
  @Override
  public CommentViewPayload deepCopy() {
    return new CommentViewPayload(comment);
  }
}
