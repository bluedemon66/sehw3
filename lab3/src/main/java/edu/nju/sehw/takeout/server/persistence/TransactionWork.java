package edu.nju.sehw.takeout.server.persistence;

/**
 * Defines the callback executed inside a transaction owned by TransactionRunner.
 *
 * @param <T> result type produced by the transactional callback
 */
public interface TransactionWork<T> {
  /**
   * Performs the supplied business and persistence work inside the active transaction.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @return the result produced by this unit of work
   */
  T execute(TransactionContext transaction);
}
