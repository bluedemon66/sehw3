package edu.nju.sehw.takeout.server.comment;

import edu.nju.sehw.takeout.model.comment.CommentSubject;
import edu.nju.sehw.takeout.model.comment.PurchaseMark;
import edu.nju.sehw.takeout.model.common.UserId;
import edu.nju.sehw.takeout.server.persistence.TransactionContext;

/**
 * Defines lookup of an author's purchase evidence for a review target in the current transaction.
 */
public interface PurchaseEvidenceResolver {
  /**
   * Finds the authenticated author's purchase evidence for the review subject.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param actor authenticated user whose ownership, purchase evidence, or idempotency record is
   *     being inspected
   * @param subject kind and identifiers of the shop, dish, or delivery service being reviewed
   * @return the purchase label calculated from server-side evidence
   */
  PurchaseMark resolve(TransactionContext transaction, UserId actor, CommentSubject subject);
}
