package edu.nju.sehw.takeout.server.operation;

import edu.nju.sehw.takeout.model.network.SecureResponse;
import edu.nju.sehw.takeout.scaffold.NotImplementedYetException;
import edu.nju.sehw.takeout.server.comment.CommentOperationHandler;
import edu.nju.sehw.takeout.server.order.OrderOperationHandler;
import edu.nju.sehw.takeout.server.persistence.DatabaseAdapter;
import edu.nju.sehw.takeout.server.persistence.IdempotencyStore;
import edu.nju.sehw.takeout.server.persistence.TransactionRunner;
import edu.nju.sehw.takeout.server.resource.ServerUploadHandler;
import edu.nju.sehw.takeout.server.security.ServerPermissionChecker;
import edu.nju.sehw.takeout.server.task.VerifiedTask;
import java.util.Objects;

/**
 * Coordinates authorization, idempotency, transactions, and the selected server operation handler.
 */
public final class ServerOperationExecutor implements OperationExecutor {
  /** Permission preflight or server authorization service used by this component. */
  private final ServerPermissionChecker checker;

  /** Lookup of prepared operations by stable numeric operation code. */
  private final ServerOperationCatalog catalog;

  /** Database execution port participating in the current transaction. */
  private final DatabaseAdapter database;

  /** Owner that creates, commits, and rolls back the complete database unit of work. */
  private final TransactionRunner transactions;

  /** Store that claims requests and saves replayable outcomes in the active transaction. */
  private final IdempotencyStore idempotency;

  /** Server handler for the comment operation group. */
  private final CommentOperationHandler comments;

  /** Server handler for order commands. */
  private final OrderOperationHandler orders;

  /** Server handler that registers verified resource uploads. */
  private final ServerUploadHandler uploads;

  /**
   * Creates this component with the dependencies documented below.
   *
   * @param checker permission preflight or server authorization service used by this component
   * @param catalog lookup of prepared operations by stable numeric operation code
   * @param database database execution port participating in the current transaction
   * @param transactions owner that creates, commits, and rolls back the complete database unit of
   *     work
   * @param idempotency store that claims requests and saves replayable outcomes in the active
   *     transaction
   * @param comments server handler for the comment operation group
   * @param orders server handler for order commands
   * @param uploads server handler that registers verified resource uploads
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public ServerOperationExecutor(
      ServerPermissionChecker checker,
      ServerOperationCatalog catalog,
      DatabaseAdapter database,
      TransactionRunner transactions,
      IdempotencyStore idempotency,
      CommentOperationHandler comments,
      OrderOperationHandler orders,
      ServerUploadHandler uploads) {
    this.checker = Objects.requireNonNull(checker, "checker");
    this.catalog = Objects.requireNonNull(catalog, "catalog");
    this.database = Objects.requireNonNull(database, "database");
    this.transactions = Objects.requireNonNull(transactions, "transactions");
    this.idempotency = Objects.requireNonNull(idempotency, "idempotency");
    this.comments = Objects.requireNonNull(comments, "comments");
    this.orders = Objects.requireNonNull(orders, "orders");
    this.uploads = Objects.requireNonNull(uploads, "uploads");
  }

  /**
   * Executes one verified operation using its authorization, idempotency, and transaction
   * contracts.
   *
   * @param task authenticated request, reply channel, and optional staged upload to execute
   * @return the response associated with the task's remote request identifier
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  @Override
  public SecureResponse execute(VerifiedTask task) {
    throw new NotImplementedYetException("ServerOperationExecutor.execute");
  }
}
