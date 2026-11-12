package edu.nju.sehw.takeout.server.security;

import edu.nju.sehw.takeout.model.common.PermissionSet;
import edu.nju.sehw.takeout.model.common.UserId;
import java.time.Instant;
import java.util.Objects;

/**
 * Stores the server's user identity, granted permissions, and session expiry.
 *
 * @param userId stable identifier of the user represented by the server session
 * @param permissions immutable snapshot of permissions granted to the represented session
 * @param expiresAt instant at which the represented session expires
 */
public record SessionPrincipal(UserId userId, PermissionSet permissions, Instant expiresAt) {
  /**
   * Creates the SessionPrincipal value from the components documented below.
   *
   * @param userId stable identifier of the user represented by the server session
   * @param permissions immutable snapshot of permissions granted to the represented session
   * @param expiresAt instant at which the represented session expires
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public SessionPrincipal {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(permissions, "permissions");
    Objects.requireNonNull(expiresAt, "expiresAt");
  }
}
