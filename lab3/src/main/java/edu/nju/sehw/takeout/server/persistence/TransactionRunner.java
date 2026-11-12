package edu.nju.sehw.takeout.server.persistence;

/**
 * Owns transaction creation, commit, rollback, and connection lifetime for a supplied callback.
 *
 * <p>The same TransactionContext is passed through business writes, audit-related persistence, and
 * idempotency completion.
 */
public interface TransactionRunner {
  /**
   * Runs the supplied work with one transaction context and commits or rolls back its unit of work.
   *
   * @param <T> result type produced by the transactional callback
   * @param work callback that performs the unit of work using the provided transaction context
   * @return the value returned by the callback after successful transaction completion
   */
  <T> T inTransaction(TransactionWork<T> work);
}
