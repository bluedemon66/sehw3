package edu.nju.sehw.takeout.server.operation;

import edu.nju.sehw.takeout.model.common.ResultStatus;
import edu.nju.sehw.takeout.model.common.TargetDescriptor;
import edu.nju.sehw.takeout.model.message.BinaryMessageCodec;
import edu.nju.sehw.takeout.model.message.ErrorPayload;
import edu.nju.sehw.takeout.model.message.OperationCode;
import edu.nju.sehw.takeout.model.network.MessageDigests;
import edu.nju.sehw.takeout.model.network.SecureResponse;
import edu.nju.sehw.takeout.model.resource.ResourcePayload;
import edu.nju.sehw.takeout.model.resource.ResourceRefPayload;
import edu.nju.sehw.takeout.server.comment.CommentOperationHandler;
import edu.nju.sehw.takeout.server.order.OrderOperationHandler;
import edu.nju.sehw.takeout.server.persistence.DatabaseAdapter;
import edu.nju.sehw.takeout.server.persistence.IdempotencyStore;
import edu.nju.sehw.takeout.server.persistence.TransactionContext;
import edu.nju.sehw.takeout.server.persistence.TransactionRunner;
import edu.nju.sehw.takeout.server.resource.ServerUploadHandler;
import edu.nju.sehw.takeout.server.security.ServerPermissionChecker;
import edu.nju.sehw.takeout.server.task.VerifiedTask;
import java.util.Objects;
import java.util.UUID;

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

  private final BinaryMessageCodec codec = new BinaryMessageCodec();

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
   */
  @Override
  public SecureResponse execute(VerifiedTask task) {
    UUID id = task.context().requestId();
    try {
      var message = task.message();
      var operation =
          catalog
              .find(message.operationCode())
              .orElseThrow(
                  () -> new OperationFailure(ResultStatus.INVALID_REQUEST, "Unknown operation"));
      var target = StandardOperations.validate(operation, message.payload());
      var access = checker.check(task.context(), operation, target);
      if (!access.allowed()) {
        return failure(id, ResultStatus.FORBIDDEN, access.description());
      }
      return transactions.inTransaction(
          transaction -> {
            if (operation.transactionPolicy() == TransactionPolicy.READ_ONLY) {
              return executeWithin(transaction, operation, target, task);
            }
            var actor = task.context().principalId();
            var claim =
                idempotency.claim(
                    transaction, actor, id, MessageDigests.sha256(codec.encode(message)));
            return switch (claim.status()) {
              case CLAIMED -> {
                var response = executeWithin(transaction, operation, target, task);
                idempotency.complete(transaction, actor, id, response);
                yield response;
              }
              case REPLAY -> claim.result().orElseThrow();
              case IN_PROGRESS ->
                  failure(id, ResultStatus.IN_PROGRESS, "Request is still processing");
              case DIGEST_MISMATCH ->
                  failure(id, ResultStatus.CONFLICT, "Request ID already used for different data");
            };
          });
    } catch (OperationFailure ex) {
      return failure(id, ex.status(), ex.getMessage());
    } catch (IllegalArgumentException ex) {
      return failure(
          id,
          ResultStatus.INVALID_REQUEST,
          ex.getMessage() == null ? "Invalid input" : ex.getMessage());
    }
  }

  private SecureResponse executeWithin(
      TransactionContext transaction,
      PreparedOperation definition,
      TargetDescriptor target,
      VerifiedTask task) {
    UUID id = task.context().requestId();
    return switch (OperationCode.fromCode(definition.operationCode())) {
      case COMMENT_LIST, COMMENT_PUBLISH, COMMENT_WITHDRAW, COMMENT_REPLY, COMMENT_HIDE -> {
        var result = comments.handle(transaction, task);
        yield new SecureResponse(id, result.status(), result.payload());
      }
      case PLACE_ORDER,
          CANCEL_ORDER,
          CONFIRM_RECEIPT,
          REPORT_NOT_RECEIVED,
          START_PREPARATION,
          RIDER_PICKUP,
          CONFIRM_DELIVERY,
          ACCEPT_ORDER,
          REJECT_ORDER,
          FINISH_PREPARATION,
          ORDER_DETAIL,
          RESOLVE_CASE -> {
        var result = orders.handle(transaction, task);
        yield new SecureResponse(id, result.status(), result.payload());
      }
      case UPLOAD_ATTACHMENT ->
          new SecureResponse(
              id, ResultStatus.SUCCESS, new ResourceRefPayload(uploads.handle(transaction, task)));
      case GET_RESOURCE ->
          new SecureResponse(
              id,
              ResultStatus.SUCCESS,
              new ResourcePayload(
                  uploads.read(transaction, target.targetId().toString(), task.context())));
      default -> {
        var result =
            database.executeWithinTransaction(
                transaction, definition, task.context(), task.message());
        yield new SecureResponse(id, result.status(), result.payload());
      }
    };
  }

  private static SecureResponse failure(UUID id, ResultStatus status, String description) {
    return new SecureResponse(id, status, new ErrorPayload(status.name(), description));
  }
}
