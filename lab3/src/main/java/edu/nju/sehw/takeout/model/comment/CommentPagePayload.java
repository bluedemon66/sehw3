package edu.nju.sehw.takeout.model.comment;

import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Objects;

/**
 * Carries a comment-list page inside a Core or network message.
 *
 * @param page assembled page associated with the UI request being completed
 */
public record CommentPagePayload(CommentPage page) implements MessagePayload {
  /**
   * Creates the CommentPagePayload value from the components documented below.
   *
   * @param page assembled page associated with the UI request being completed
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public CommentPagePayload {
    Objects.requireNonNull(page, "page");
  }

  /**
   * Creates a payload or message snapshot with independent mutable state.
   *
   * @return a separate snapshot preserving the same argument values and request identities
   */
  @Override
  public CommentPagePayload deepCopy() {
    return new CommentPagePayload(page);
  }
}
