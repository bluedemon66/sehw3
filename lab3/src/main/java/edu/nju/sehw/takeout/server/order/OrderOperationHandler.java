package edu.nju.sehw.takeout.server.order;

import edu.nju.sehw.takeout.scaffold.NotImplementedYetException;
import edu.nju.sehw.takeout.server.persistence.QueryResult;
import edu.nju.sehw.takeout.server.persistence.TransactionContext;
import edu.nju.sehw.takeout.server.task.VerifiedTask;
import java.util.Objects;

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
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  public QueryResult handle(TransactionContext transaction, VerifiedTask task) {
    throw new NotImplementedYetException("OrderOperationHandler.handle");
  }
}
