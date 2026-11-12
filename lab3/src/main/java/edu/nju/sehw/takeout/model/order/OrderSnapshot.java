package edu.nju.sehw.takeout.model.order;

import edu.nju.sehw.takeout.model.common.UserId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Carries the persisted order identity, ownership, status, paid amount, and concurrency version.
 *
 * @param orderId stable identifier of the order associated with this command, snapshot, or review
 *     target
 * @param customerId identifier of the customer who owns the order
 * @param shopId identifier of the shop owning the order or review target
 * @param status current state in the paid-order lifecycle
 * @param paidAmount exact decimal amount actually paid for the order
 * @param updatedAt instant at which the order snapshot was last changed
 * @param version persisted order version used for optimistic concurrency checks
 */
public record OrderSnapshot(
    UUID orderId,
    UserId customerId,
    UUID shopId,
    OrderStatus status,
    BigDecimal paidAmount,
    Instant updatedAt,
    long version) {
  /**
   * Creates the OrderSnapshot value from the components documented below.
   *
   * @param orderId stable identifier of the order associated with this command, snapshot, or review
   *     target
   * @param customerId identifier of the customer who owns the order
   * @param shopId identifier of the shop owning the order or review target
   * @param status current state in the paid-order lifecycle
   * @param paidAmount exact decimal amount actually paid for the order
   * @param updatedAt instant at which the order snapshot was last changed
   * @param version persisted order version used for optimistic concurrency checks
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public OrderSnapshot {
    Objects.requireNonNull(orderId, "orderId");
    Objects.requireNonNull(customerId, "customerId");
    Objects.requireNonNull(shopId, "shopId");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(paidAmount, "paidAmount");
    Objects.requireNonNull(updatedAt, "updatedAt");
  }
}
