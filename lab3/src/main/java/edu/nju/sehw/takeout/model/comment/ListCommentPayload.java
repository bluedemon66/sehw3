package edu.nju.sehw.takeout.model.comment;

import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Objects;
import java.util.Optional;

/**
 * Carries the target, ordering, cursor, and requested page size for comment browsing.
 *
 * @param subject kind and identifiers of the shop, dish, or delivery service being reviewed
 * @param sort comment ordering strategy selected for the query
 * @param cursor optional cursor from a previous result; an empty optional starts the first page
 * @param limit requested maximum number of comment entries in the result page
 */
public record ListCommentPayload(
    CommentSubject subject, CommentSort sort, Optional<String> cursor, int limit)
    implements MessagePayload {
  /**
   * Creates the ListCommentPayload value from the components documented below.
   *
   * @param subject kind and identifiers of the shop, dish, or delivery service being reviewed
   * @param sort comment ordering strategy selected for the query
   * @param cursor optional cursor from a previous result; an empty optional starts the first page
   * @param limit requested maximum number of comment entries in the result page
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public ListCommentPayload {
    Objects.requireNonNull(subject, "subject");
    Objects.requireNonNull(sort, "sort");
    Objects.requireNonNull(cursor, "cursor");
  }

  /**
   * Creates a payload or message snapshot with independent mutable state.
   *
   * @return a separate snapshot preserving the same argument values and request identities
   */
  @Override
  public ListCommentPayload deepCopy() {
    return new ListCommentPayload(subject, sort, cursor, limit);
  }
}
