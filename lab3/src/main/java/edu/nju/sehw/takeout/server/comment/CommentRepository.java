package edu.nju.sehw.takeout.server.comment;

import edu.nju.sehw.takeout.model.comment.CommentPage;
import edu.nju.sehw.takeout.model.comment.CommentStatus;
import edu.nju.sehw.takeout.model.comment.CommentView;
import edu.nju.sehw.takeout.model.comment.ReplyView;
import edu.nju.sehw.takeout.server.persistence.TransactionContext;

/**
 * Defines comment queries and state-changing operations within a caller-owned database transaction.
 */
public interface CommentRepository {
  /**
   * Reads the visible comment projections using the selected target, ordering, and cursor.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param query comment-list parameters together with the authenticated request context
   * @return the requested comment page and its optional next-page cursor
   */
  CommentPage listPage(TransactionContext transaction, CommentQuery query);

  /**
   * Persists the validated comment and its image associations in the supplied transaction.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param command comment write parameters together with the authenticated request context
   * @return the published comment display projection
   */
  CommentView publish(TransactionContext transaction, PublishCommentCommand command);

  /**
   * Updates the authored comment to its withdrawn state in the supplied transaction.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param command comment write parameters together with the authenticated request context
   * @return the resulting comment publication state
   */
  CommentStatus withdraw(TransactionContext transaction, WithdrawCommentCommand command);

  /**
   * Persists the authorized merchant reply in the supplied transaction.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param command comment write parameters together with the authenticated request context
   * @return the public display projection of the new reply
   */
  ReplyView reply(TransactionContext transaction, ReplyCommentCommand command);

  /**
   * Records platform moderation of the comment in the supplied transaction.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param command comment write parameters together with the authenticated request context
   * @return the resulting comment publication state
   */
  CommentStatus hide(TransactionContext transaction, HideCommentCommand command);
}
