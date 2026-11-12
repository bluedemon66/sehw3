package edu.nju.sehw.takeout.server.comment;

import edu.nju.sehw.takeout.model.comment.CommentSubject;
import edu.nju.sehw.takeout.model.comment.PurchaseMark;
import edu.nju.sehw.takeout.model.common.UserId;
import edu.nju.sehw.takeout.scaffold.NotImplementedYetException;
import edu.nju.sehw.takeout.server.persistence.DatabaseAdapter;
import edu.nju.sehw.takeout.server.persistence.TransactionContext;
import java.util.Objects;

/** Adapts database purchase records to the purchase-evidence label shown with a review. */
public final class DefaultPurchaseEvidenceResolver implements PurchaseEvidenceResolver {
  /** Database execution port participating in the current transaction. */
  private final DatabaseAdapter database;

  /**
   * Creates this component with the dependencies documented below.
   *
   * @param database database execution port participating in the current transaction
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public DefaultPurchaseEvidenceResolver(DatabaseAdapter database) {
    this.database = Objects.requireNonNull(database, "database");
  }

  /**
   * Finds the authenticated author's purchase evidence for the review subject.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param actor authenticated user whose ownership, purchase evidence, or idempotency record is
   *     being inspected
   * @param subject kind and identifiers of the shop, dish, or delivery service being reviewed
   * @return the purchase label calculated from server-side evidence
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  @Override
  public PurchaseMark resolve(
      TransactionContext transaction, UserId actor, CommentSubject subject) {
    throw new NotImplementedYetException("DefaultPurchaseEvidenceResolver.resolve");
  }
}
