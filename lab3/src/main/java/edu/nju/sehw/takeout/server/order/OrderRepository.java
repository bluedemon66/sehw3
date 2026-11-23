package edu.nju.sehw.takeout.server.order;

import edu.nju.sehw.takeout.model.common.UserId;
import edu.nju.sehw.takeout.model.order.OrderSnapshot;
import edu.nju.sehw.takeout.model.order.OrderStatus;
import edu.nju.sehw.takeout.model.order.PlaceOrderPayload;
import edu.nju.sehw.takeout.server.persistence.TransactionContext;
import java.util.Optional;
import java.util.UUID;

/** Defines order lookup and conditional state transitions inside the active transaction. */
public interface OrderRepository {
  /** Creates a pending order from the current menu prices. */
  default OrderSnapshot create(
      TransactionContext transaction, UserId customer, PlaceOrderPayload order) {
    throw new UnsupportedOperationException("Order creation is unavailable");
  }

  /** Checks whether the principal manages this shop. */
  default boolean manages(TransactionContext transaction, UserId actor, UUID shopId) {
    return false;
  }

  /** Checks whether the principal is the assigned rider. */
  default boolean assigned(TransactionContext transaction, UserId actor, UUID orderId) {
    return false;
  }

  /**
   * Reads the selected order snapshot in the supplied transaction.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param orderId order identifier
   * @return the order snapshot, or Optional.empty() when the order is absent
   */
  Optional<OrderSnapshot> find(TransactionContext transaction, UUID orderId);

  /**
   * Changes the order state when both the persisted version and source state match the
   * expectations.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param orderId stable identifier of the order associated with this command, snapshot, or review
   *     target
   * @param expectedVersion order version expected by the caller when submitting a state change
   * @param expected order state required for the conditional update to succeed
   * @param next order state to store after the expected version and state match
   * @return true when the conditional transition succeeds; false when the expected state or version
   *     does not match
   */
  boolean transition(
      TransactionContext transaction,
      UUID orderId,
      long expectedVersion,
      OrderStatus expected,
      OrderStatus next);
}
