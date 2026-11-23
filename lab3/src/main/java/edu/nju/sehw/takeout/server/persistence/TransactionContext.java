package edu.nju.sehw.takeout.server.persistence;

import java.sql.Connection;

/**
 * Exposes the JDBC connection belonging to the currently active unit of work.
 *
 * <p>P.S. Connection lifetime belongs to the enclosing TransactionRunner.
 */
public interface TransactionContext {
  /**
   * Returns the JDBC connection associated with the active transaction.
   *
   * @return the connection owned by the enclosing TransactionRunner
   */
  Connection connection();

  /**
   * Registers cleanup of an external write if the database transaction rolls back.
   *
   * @param cleanup cleanup action
   */
  default void onRollback(Runnable cleanup) {
    throw new UnsupportedOperationException("Transaction does not support rollback callbacks");
  }
}
