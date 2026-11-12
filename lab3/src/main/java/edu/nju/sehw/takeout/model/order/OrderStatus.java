package edu.nju.sehw.takeout.model.order;

/**
 * Describes the paid-order lifecycle, including customer confirmation and specialist exception
 * handling.
 */
public enum OrderStatus {
  /** The paid order is waiting for merchant acceptance. */
  PENDING_ACCEPTANCE,
  /** The accepted order is queued before preparation. */
  QUEUED,
  /** The merchant has started preparing the order. */
  PREPARING,
  /** Preparation is complete and the order awaits rider pickup. */
  READY_FOR_PICKUP,
  /** The rider has picked up the order and is delivering it. */
  DELIVERING,
  /** The rider has marked delivery and the customer must confirm receipt. */
  AWAITING_CUSTOMER_CONFIRMATION,
  /** The customer has confirmed receipt and completion can trigger settlement. */
  COMPLETED,
  /** The order requires specialist communication after a not-received report. */
  EXCEPTION,
  /** The merchant has rejected the paid order. */
  REJECTED,
  /** The queued order has been cancelled. */
  CANCELLED
}
