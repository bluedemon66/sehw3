package edu.nju.sehw.takeout.server.persistence;

import edu.nju.sehw.takeout.model.common.ResultStatus;
import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.message.OperationCode;
import edu.nju.sehw.takeout.model.message.TargetPayload;
import edu.nju.sehw.takeout.model.ui.DecimalValue;
import edu.nju.sehw.takeout.model.ui.TextValue;
import edu.nju.sehw.takeout.model.ui.UiValue;
import edu.nju.sehw.takeout.model.ui.UiValuesPayload;
import edu.nju.sehw.takeout.server.operation.OperationFailure;
import edu.nju.sehw.takeout.server.operation.PreparedOperation;
import edu.nju.sehw.takeout.server.security.ServerRequestContext;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
   */
  @Override
  public <T> T inTransaction(TransactionWork<T> work) {
    try (var connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      List<Runnable> cleanups = new ArrayList<>();
      TransactionContext transaction =
          new TransactionContext() {
            @Override
            public Connection connection() {
              return connection;
            }

            @Override
            public void onRollback(Runnable cleanup) {
              cleanups.add(Objects.requireNonNull(cleanup));
            }
          };
      try {
        T result = work.execute(transaction);
        connection.commit();
        return result;
      } catch (RuntimeException | Error | SQLException ex) {
        try {
          connection.rollback();
          for (int i = cleanups.size() - 1; i >= 0; i--) {
            try {
              cleanups.get(i).run();
            } catch (RuntimeException cleanup) {
              ex.addSuppressed(cleanup);
            }
          }
        } catch (SQLException rollback) {
          ex.addSuppressed(rollback);
        }
        throw ex;
      }
    } catch (SQLException ex) {
      throw new IllegalStateException("Database transaction failed", ex);
    }
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
   */
  @Override
  public QueryResult executeWithinTransaction(
      TransactionContext transaction,
      PreparedOperation operation,
      ServerRequestContext context,
      Message message) {
    var actor = context.principalId().value();
    var values =
        switch (OperationCode.fromCode(operation.operationCode())) {
          case MERCHANT_TURNOVER -> {
            var shop = ((TargetPayload) message.payload()).targetId();
            if (Jdbc.first(
                    transaction,
                    "SELECT 1 FROM shops WHERE shop_id=? AND merchant_id=?",
                    row -> row.getInt(1),
                    shop,
                    actor)
                .isEmpty()) {
              throw new OperationFailure(ResultStatus.FORBIDDEN, "Shop access denied");
            }
            var total =
                Jdbc.first(
                        transaction,
                        """
                        SELECT COALESCE(SUM(paid_amount), 0) FROM orders
                        WHERE shop_id=? AND status='COMPLETED'
                        """,
                        row -> row.getBigDecimal(1),
                        shop)
                    .orElseThrow();
            yield Map.<Integer, UiValue>of(20, new DecimalValue(total));
          }
          case RIDER_TASKS -> {
            var details =
                Jdbc.query(
                    transaction,
                    """
                    SELECT order_id, status FROM orders WHERE rider_id=?
                    AND status IN ('READY_FOR_PICKUP', 'DELIVERING') ORDER BY updated_at, order_id
                    """,
                    row -> row.getObject(1) + " " + row.getString(2),
                    actor);
            yield Map.<Integer, UiValue>of(20, new TextValue(String.join("\n", details)));
          }
          default ->
              throw new OperationFailure(
                  ResultStatus.INVALID_REQUEST, "No registered SQL operation");
        };
    return new QueryResult(ResultStatus.SUCCESS, new UiValuesPayload(values));
  }
}
