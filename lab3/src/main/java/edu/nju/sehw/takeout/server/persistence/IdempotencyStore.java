package edu.nju.sehw.takeout.server.persistence;

import edu.nju.sehw.takeout.model.common.UserId;
import edu.nju.sehw.takeout.model.network.SecureResponse;
import java.util.UUID;

/**
 * Defines transactional claim and completion records keyed by authenticated actor and request
 * identifier.
 */
public interface IdempotencyStore {
  /**
   * Claims the actor and request identifier using the body digest in the current transaction.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param actor authenticated user whose ownership, purchase evidence, or idempotency record is
   *     being inspected
   * @param requestId identifier of this individual request; retries reuse the same identifier
   * @param digest digest identifying the request body for idempotent replay and mismatch detection
   * @return the claim disposition and, for a replay, the previously saved response
   */
  ClaimResult claim(TransactionContext transaction, UserId actor, UUID requestId, byte[] digest);

  /**
   * Saves the final response for replay in the transaction that commits the business changes.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param actor authenticated user whose ownership, purchase evidence, or idempotency record is
   *     being inspected
   * @param requestId identifier of this individual request; retries reuse the same identifier
   * @param result remote request identifier, result status, and typed result payload
   */
  void complete(
      TransactionContext transaction, UserId actor, UUID requestId, SecureResponse result);
}
