package edu.nju.sehw.takeout.server.order;

import edu.nju.sehw.takeout.model.common.ResultStatus;
import edu.nju.sehw.takeout.model.common.UserId;
import edu.nju.sehw.takeout.model.order.OrderSnapshot;
import edu.nju.sehw.takeout.model.order.OrderStatus;
import edu.nju.sehw.takeout.model.order.PlaceOrderPayload;
import edu.nju.sehw.takeout.server.operation.OperationFailure;
import edu.nju.sehw.takeout.server.persistence.Jdbc;
import edu.nju.sehw.takeout.server.persistence.TransactionContext;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Persists orders and performs versioned order status updates. */
public final class JdbcOrderRepository implements OrderRepository {
  private final Clock clock;

  /** Uses the given clock for order timestamps. */
  public JdbcOrderRepository(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
  }

  @Override
  public Optional<OrderSnapshot> find(TransactionContext transaction, UUID orderId) {
    return Jdbc.first(
        transaction,
        "SELECT * FROM orders WHERE order_id=?",
        row ->
            new OrderSnapshot(
                row.getObject("order_id", UUID.class),
                new UserId(row.getObject("customer_id", UUID.class)),
                row.getObject("shop_id", UUID.class),
                OrderStatus.valueOf(row.getString("status")),
                row.getBigDecimal("paid_amount"),
                row.getObject("updated_at", OffsetDateTime.class).toInstant(),
                row.getLong("version")),
        orderId);
  }

  @Override
  public OrderSnapshot create(
      TransactionContext transaction, UserId customer, PlaceOrderPayload order) {
    BigDecimal amount = BigDecimal.ZERO;
    for (var item : order.items().entrySet()) {
      BigDecimal price =
          Jdbc.first(
                  transaction,
                  "SELECT price FROM dishes WHERE dish_id=? AND shop_id=?",
                  row -> row.getBigDecimal(1),
                  item.getKey(),
                  order.shopId())
              .orElseThrow(
                  () ->
                      new OperationFailure(ResultStatus.NOT_FOUND, "Dish not found at this shop"));
      amount = amount.add(price.multiply(BigDecimal.valueOf(item.getValue())));
    }
    UUID rider =
        Jdbc.first(
                transaction,
                """
                SELECT rider_id FROM shop_riders WHERE shop_id=? ORDER BY rider_id LIMIT 1
                """,
                row -> row.getObject(1, UUID.class),
                order.shopId())
            .orElseThrow(
                () ->
                    new OperationFailure(
                        ResultStatus.REJECTED, "No rider available for this shop"));
    UUID id = UUID.randomUUID();
    var now = clock.instant();
    Jdbc.update(
        transaction,
        """
        INSERT INTO orders (order_id, customer_id, shop_id, rider_id, status,
          paid_amount, updated_at, version)
        VALUES (?, ?, ?, ?, 'PENDING_ACCEPTANCE', ?, ?, 0)
        """,
        id,
        customer.value(),
        order.shopId(),
        rider,
        amount,
        now.atOffset(ZoneOffset.UTC));
    for (var item : order.items().entrySet()) {
      Jdbc.update(
          transaction,
          "INSERT INTO order_items (order_id, dish_id, quantity) VALUES (?, ?, ?)",
          id,
          item.getKey(),
          item.getValue());
    }
    return new OrderSnapshot(
        id, customer, order.shopId(), OrderStatus.PENDING_ACCEPTANCE, amount, now, 0);
  }

  @Override
  public boolean transition(
      TransactionContext transaction,
      UUID orderId,
      long expectedVersion,
      OrderStatus expected,
      OrderStatus next) {
    return Jdbc.update(
            transaction,
            """
            UPDATE orders SET status=?, version=version+1, updated_at=?
            WHERE order_id=? AND version=? AND status=?
            """,
            next.name(),
            clock.instant().atOffset(ZoneOffset.UTC),
            orderId,
            expectedVersion,
            expected.name())
        == 1;
  }

  @Override
  public boolean manages(TransactionContext transaction, UserId actor, UUID shopId) {
    return Jdbc.first(
            transaction,
            "SELECT 1 FROM shops WHERE shop_id=? AND merchant_id=?",
            row -> row.getInt(1),
            shopId,
            actor.value())
        .isPresent();
  }

  @Override
  public boolean assigned(TransactionContext transaction, UserId actor, UUID orderId) {
    return Jdbc.first(
            transaction,
            "SELECT 1 FROM orders WHERE order_id=? AND rider_id=?",
            row -> row.getInt(1),
            orderId,
            actor.value())
        .isPresent();
  }
}
