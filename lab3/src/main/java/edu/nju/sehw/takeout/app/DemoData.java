package edu.nju.sehw.takeout.app;

import edu.nju.sehw.takeout.client.query.ClientSession;
import edu.nju.sehw.takeout.model.common.Permission;
import edu.nju.sehw.takeout.model.common.PermissionSet;
import edu.nju.sehw.takeout.model.common.UserId;
import edu.nju.sehw.takeout.model.network.AccessToken;
import edu.nju.sehw.takeout.server.persistence.Jdbc;
import edu.nju.sehw.takeout.server.persistence.JdbcDatabaseAdapter;
import edu.nju.sehw.takeout.server.security.ServerSessionStore;
import edu.nju.sehw.takeout.server.security.SessionPrincipal;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

/** Provides a small menu and separate role sessions for the localhost lab demonstration. */
public final class DemoData {
  /** Shop containing the two sample menu items. */
  public static final UUID SHOP = UUID.fromString("00000000-0000-0000-0000-000000000101");

  /** Sample dumpling dish. */
  public static final UUID DUMPLINGS = UUID.fromString("00000000-0000-0000-0000-000000000111");

  /** Sample noodle dish. */
  public static final UUID NOODLES = UUID.fromString("00000000-0000-0000-0000-000000000112");

  private DemoData() {}

  /** Roles available in the lab demo. */
  public enum Role {
    /** Customer placing orders and writing reviews. */
    CUSTOMER(
        1,
        Permission.PLACE_ORDER,
        Permission.CANCEL_OWN_ORDER,
        Permission.PUBLISH_COMMENT,
        Permission.WITHDRAW_OWN_COMMENT,
        Permission.UPLOAD_RESOURCE),
    /** Merchant fulfilling orders for the sample shop. */
    MERCHANT(2, Permission.MANAGE_SHOP, Permission.REPLY_SHOP_COMMENT),
    /** Rider assigned to sample orders. */
    RIDER(3, Permission.DELIVER_ORDER),
    /** Moderator handling reviews and communication cases. */
    MODERATOR(4, Permission.HIDE_COMMENT, Permission.RESOLVE_CASE);

    private final UserId user;
    private final PermissionSet permissions;

    Role(int number, Permission... permissions) {
      this.user = new UserId(new UUID(0, number));
      var all =
          EnumSet.of(Permission.READ_ORDER, Permission.READ_COMMENT, Permission.READ_RESOURCE);
      all.addAll(java.util.List.of(permissions));
      this.permissions = new PermissionSet(all);
    }

    /** Returns the actor associated with this lab role. */
    public UserId user() {
      return user;
    }

    private String environment() {
      return "SEHW3_" + name() + "_TOKEN";
    }
  }

  /** Installs the sample shop and menu without disturbing existing orders and comments. */
  public static void seed(DataSource dataSource) {
    new JdbcDatabaseAdapter(dataSource)
        .inTransaction(
            tx -> {
              Jdbc.update(
                  tx,
                  "MERGE INTO shops (shop_id, merchant_id) KEY (shop_id) VALUES (?, ?)",
                  SHOP,
                  Role.MERCHANT.user().value());
              Jdbc.update(
                  tx,
                  """
                  MERGE INTO shop_riders (shop_id, rider_id) KEY (shop_id, rider_id) VALUES (?, ?)
                  """,
                  SHOP,
                  Role.RIDER.user().value());
              Jdbc.update(
                  tx,
                  "MERGE INTO dishes (dish_id, shop_id, price) KEY (dish_id) VALUES (?, ?, ?)",
                  DUMPLINGS,
                  SHOP,
                  new BigDecimal("16.50"));
              Jdbc.update(
                  tx,
                  "MERGE INTO dishes (dish_id, shop_id, price) KEY (dish_id) VALUES (?, ?, ?)",
                  NOODLES,
                  SHOP,
                  new BigDecimal("18.00"));
              return null;
            });
  }

  /** Reads demo session tokens from the process environment. */
  public static ServerSessionStore serverSessions(Clock clock) {
    Map<String, SessionPrincipal> tokens = new HashMap<>();
    var expires = clock.instant().plus(8, ChronoUnit.HOURS);
    for (Role role : Role.values()) {
      String token = System.getenv(role.environment());
      if (token != null
          && !token.isBlank()
          && tokens.putIfAbsent(token, new SessionPrincipal(role.user, role.permissions, expires))
              != null) {
        throw new IllegalArgumentException("Demo session tokens must be distinct");
      }
    }
    if (tokens.isEmpty()) {
      throw new IllegalArgumentException("Set at least one SEHW3 role token");
    }
    return token -> Optional.ofNullable(tokens.get(token.value()));
  }

  /** Builds the demo session for the specified role. */
  public static ClientSession clientSession(Role role, Clock clock) {
    return new ClientSession(
        role.user,
        role.permissions,
        new AccessToken(requiredEnvironment(role.environment())),
        clock.instant().plus(8, ChronoUnit.HOURS));
  }

  static String requiredEnvironment(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Set the " + name + " environment variable");
    }
    return value;
  }
}
