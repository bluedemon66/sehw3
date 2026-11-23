package edu.nju.sehw.takeout.server.order;

import edu.nju.sehw.takeout.model.common.ResultStatus;
import edu.nju.sehw.takeout.model.message.OperationCode;
import edu.nju.sehw.takeout.model.message.TargetPayload;
import edu.nju.sehw.takeout.model.order.OrderCommandPayload;
import edu.nju.sehw.takeout.model.order.OrderSnapshot;
import edu.nju.sehw.takeout.model.order.OrderStatus;
import edu.nju.sehw.takeout.model.order.PlaceOrderPayload;
import edu.nju.sehw.takeout.model.order.ResolveCasePayload;
import edu.nju.sehw.takeout.model.ui.DecimalValue;
import edu.nju.sehw.takeout.model.ui.TextValue;
import edu.nju.sehw.takeout.model.ui.UiValuesPayload;
import edu.nju.sehw.takeout.server.operation.OperationFailure;
import edu.nju.sehw.takeout.server.persistence.QueryResult;
import edu.nju.sehw.takeout.server.persistence.TransactionContext;
import edu.nju.sehw.takeout.server.task.VerifiedTask;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Dispatches authenticated order commands to order and exception-case repositories. */
public final class OrderOperationHandler {
  /** Repository port used by this business handler. */
  private final OrderRepository repository;

  /** Repository for communication cases linked to exception orders. */
  private final ExceptionCaseRepository exceptionCases;

  /**
   * Creates this component with the dependencies documented below.
   *
   * @param repository repository port used by this business handler
   * @param exceptionCases repository for communication cases linked to exception orders
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public OrderOperationHandler(OrderRepository repository, ExceptionCaseRepository exceptionCases) {
    this.repository = Objects.requireNonNull(repository, "repository");
    this.exceptionCases = Objects.requireNonNull(exceptionCases, "exceptionCases");
  }

  /**
   * Processes an authenticated order command using the current transaction and required
   * repositories.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param task authenticated request, reply channel, and optional staged upload to execute
   * @return the order-operation status and typed result payload
   */
  public QueryResult handle(TransactionContext transaction, VerifiedTask task) {
    var operation = OperationCode.fromCode(task.message().operationCode());
    var actor = task.context().principalId();
    var payload = task.message().payload();
    if (operation == OperationCode.PLACE_ORDER) {
      return result(repository.create(transaction, actor, (PlaceOrderPayload) payload));
    }
    if (operation == OperationCode.RESOLVE_CASE) {
      var command = (ResolveCasePayload) payload;
      var resolved = exceptionCases.resolve(transaction, command.caseId(), command.resolution());
      return new QueryResult(
          ResultStatus.SUCCESS,
          new UiValuesPayload(
              Map.of(
                  20,
                  new TextValue(resolved.status().name()),
                  21,
                  new TextValue(resolved.caseId().toString()))));
    }
    UUID orderId =
        operation == OperationCode.ORDER_DETAIL
            ? ((TargetPayload) payload).targetId()
            : ((OrderCommandPayload) payload).orderId();
    OrderSnapshot order =
        repository
            .find(transaction, orderId)
            .orElseThrow(() -> new OperationFailure(ResultStatus.NOT_FOUND, "Order not found"));
    boolean customer = order.customerId().equals(actor);
    boolean allowed =
        switch (operation) {
          case CANCEL_ORDER, CONFIRM_RECEIPT, REPORT_NOT_RECEIVED -> customer;
          case ACCEPT_ORDER, REJECT_ORDER, START_PREPARATION, FINISH_PREPARATION ->
              repository.manages(transaction, actor, order.shopId());
          case RIDER_PICKUP, CONFIRM_DELIVERY -> repository.assigned(transaction, actor, orderId);
          case ORDER_DETAIL ->
              customer
                  || repository.manages(transaction, actor, order.shopId())
                  || repository.assigned(transaction, actor, orderId);
          default -> false;
        };
    if (!allowed) {
      throw new OperationFailure(ResultStatus.FORBIDDEN, "Order access denied");
    }
    if (operation == OperationCode.ORDER_DETAIL) {
      return result(order);
    }
    OrderStatus expected =
        switch (operation) {
          case ACCEPT_ORDER, REJECT_ORDER -> OrderStatus.PENDING_ACCEPTANCE;
          case CANCEL_ORDER ->
              order.status() == OrderStatus.PENDING_ACCEPTANCE
                  ? OrderStatus.PENDING_ACCEPTANCE
                  : OrderStatus.QUEUED;
          case START_PREPARATION -> OrderStatus.QUEUED;
          case FINISH_PREPARATION -> OrderStatus.PREPARING;
          case RIDER_PICKUP -> OrderStatus.READY_FOR_PICKUP;
          case CONFIRM_DELIVERY -> OrderStatus.DELIVERING;
          case CONFIRM_RECEIPT, REPORT_NOT_RECEIVED -> OrderStatus.AWAITING_CUSTOMER_CONFIRMATION;
          default -> throw new IllegalArgumentException("Unsupported order operation");
        };
    OrderStatus next =
        switch (operation) {
          case ACCEPT_ORDER -> OrderStatus.QUEUED;
          case REJECT_ORDER -> OrderStatus.REJECTED;
          case CANCEL_ORDER -> OrderStatus.CANCELLED;
          case START_PREPARATION -> OrderStatus.PREPARING;
          case FINISH_PREPARATION -> OrderStatus.READY_FOR_PICKUP;
          case RIDER_PICKUP -> OrderStatus.DELIVERING;
          case CONFIRM_DELIVERY -> OrderStatus.AWAITING_CUSTOMER_CONFIRMATION;
          case CONFIRM_RECEIPT -> OrderStatus.COMPLETED;
          case REPORT_NOT_RECEIVED -> OrderStatus.EXCEPTION;
          default -> throw new IllegalArgumentException("Unsupported order operation");
        };
    long version = ((OrderCommandPayload) payload).expectedVersion();
    if (!repository.transition(transaction, orderId, version, expected, next)) {
      throw new OperationFailure(ResultStatus.CONFLICT, "Order state or version changed");
    }
    if (next == OrderStatus.EXCEPTION) {
      var communication = exceptionCases.open(transaction, orderId);
      return new QueryResult(
          ResultStatus.SUCCESS,
          new UiValuesPayload(
              Map.of(
                  20,
                  new TextValue(next.name()),
                  21,
                  new TextValue(communication.caseId().toString()))));
    }
    return result(repository.find(transaction, orderId).orElseThrow());
  }

  private static QueryResult result(OrderSnapshot order) {
    return new QueryResult(
        ResultStatus.SUCCESS,
        new UiValuesPayload(
            Map.of(
                20,
                new TextValue(order.status().name()),
                21,
                new TextValue(order.orderId().toString()),
                22,
                new DecimalValue(order.paidAmount()),
                23,
                new DecimalValue(java.math.BigDecimal.valueOf(order.version())))));
  }
}
