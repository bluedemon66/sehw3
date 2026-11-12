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
}
