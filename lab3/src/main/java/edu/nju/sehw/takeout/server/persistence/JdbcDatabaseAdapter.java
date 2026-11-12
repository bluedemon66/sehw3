package edu.nju.sehw.takeout.server.persistence;

import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.scaffold.NotImplementedYetException;
import edu.nju.sehw.takeout.server.operation.PreparedOperation;
import edu.nju.sehw.takeout.server.security.ServerRequestContext;
import java.util.Objects;
import javax.sql.DataSource;

/** Adapts the database and transaction-owner contracts to a JDBC data source. */
public final class JdbcDatabaseAdapter implements DatabaseAdapter, TransactionRunner {
  /** Configured JDBC connection factory used by the database adapter. */
  private final DataSource dataSource;

  /**
   * Creates this component with the dependencies documented below.
   *
   * @param dataSource configured JDBC connection factory used by the database adapter
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public JdbcDatabaseAdapter(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  /**
   * Runs the supplied work with one transaction context and commits or rolls back its unit of work.
   *
   * @param <T> result type produced by the transactional callback
   * @param work callback that performs the unit of work using the provided transaction context
   * @return the value returned by the callback after successful transaction completion
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  @Override
  public <T> T inTransaction(TransactionWork<T> work) {
    throw new NotImplementedYetException("JdbcDatabaseAdapter.inTransaction");
  }

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
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  @Override
  public QueryResult executeWithinTransaction(
      TransactionContext transaction,
      PreparedOperation operation,
      ServerRequestContext context,
      Message message) {
    throw new NotImplementedYetException("JdbcDatabaseAdapter.executeWithinTransaction");
  }
}
