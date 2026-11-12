package edu.nju.sehw.takeout.server.persistence;

import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.server.operation.PreparedOperation;
import edu.nju.sehw.takeout.server.security.ServerRequestContext;

/** Defines execution of a prepared data operation using the caller's active transaction. */
public interface DatabaseAdapter {
  /**
   * Executes the prepared parameterized data operation in the supplied transaction.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param operation indexed operation metadata including permission, schema, and transaction
   *     policy
   * @param context authenticated principal, server-established permissions, and remote request
   *     identity
   * @param message message envelope containing request identity, routing fields, operation code,
   *     and arguments
   * @return the data-operation status and copied result payload
   */
  QueryResult executeWithinTransaction(
      TransactionContext transaction,
      PreparedOperation operation,
      ServerRequestContext context,
      Message message);
}
