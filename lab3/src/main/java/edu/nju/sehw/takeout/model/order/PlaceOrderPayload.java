package edu.nju.sehw.takeout.model.order;

import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Describes an order using server-priced menu items.
 *
 * @param shopId shop fulfilling the order
 * @param items dish identifiers and positive quantities
 */
public record PlaceOrderPayload(UUID shopId, Map<UUID, Integer> items) implements MessagePayload {
  /** Validates and snapshots the supplied fields. */
  public PlaceOrderPayload {
    Objects.requireNonNull(shopId, "shopId");
    Objects.requireNonNull(items, "items");
    items = Map.copyOf(items);
    if (items.isEmpty()
        || items.values().stream().anyMatch(quantity -> quantity <= 0 || quantity > 100)) {
      throw new IllegalArgumentException("Order requires valid dish quantities");
    }
  }

  @Override
  public PlaceOrderPayload deepCopy() {
    return new PlaceOrderPayload(shopId, items);
  }
}
