package edu.nju.sehw.takeout.model.comment;

import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Objects;

/**
 * Carries the resulting publication state of a comment operation.
 *
 * @param status publication, withdrawal, or platform visibility state of the comment
 */
public record CommentStatusPayload(CommentStatus status) implements MessagePayload {
  /**
   * Creates the CommentStatusPayload value from the components documented below.
   *
   * @param status publication, withdrawal, or platform visibility state of the comment
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public CommentStatusPayload {
    Objects.requireNonNull(status, "status");
  }

  /**
   * Creates a payload or message snapshot with independent mutable state.
   *
   * @return a separate snapshot preserving the same argument values and request identities
   */
  @Override
  public CommentStatusPayload deepCopy() {
    return new CommentStatusPayload(status);
  }
}
