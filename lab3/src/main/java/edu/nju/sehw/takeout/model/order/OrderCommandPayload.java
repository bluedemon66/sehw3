package edu.nju.sehw.takeout.model.order;

import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Objects;
import java.util.UUID;

/**
 * Carries the target order identifier and expected version for an order command.
 *
 * @param orderId stable identifier of the order associated with this command, snapshot, or review
 *     target
 * @param expectedVersion order version expected by the caller when submitting a state change
 */
public record OrderCommandPayload(UUID orderId, long expectedVersion) implements MessagePayload {
  /**
   * Creates the OrderCommandPayload value from the components documented below.
   *
   * @param orderId stable identifier of the order associated with this command, snapshot, or review
   *     target
   * @param expectedVersion order version expected by the caller when submitting a state change
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public OrderCommandPayload {
    Objects.requireNonNull(orderId, "orderId");
  }

  /**
   * Creates a payload or message snapshot with independent mutable state.
   *
   * @return a separate snapshot preserving the same argument values and request identities
   */
  @Override
  public OrderCommandPayload deepCopy() {
    return new OrderCommandPayload(orderId, expectedVersion);
  }
}
