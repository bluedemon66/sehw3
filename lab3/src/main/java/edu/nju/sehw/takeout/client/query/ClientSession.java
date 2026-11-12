package edu.nju.sehw.takeout.client.query;

import edu.nju.sehw.takeout.model.common.PermissionSet;
import edu.nju.sehw.takeout.model.common.UserId;
import edu.nju.sehw.takeout.model.network.AccessToken;
import java.time.Instant;
import java.util.Objects;

/**
 * Stores the client's current identity, cached permissions, credential, and expiry time.
 *
 * @param actorId identifier of the user represented by the cached client session
 * @param permissions immutable snapshot of permissions granted to the represented session
 * @param accessToken credential presented to the server for session authentication
 * @param expiresAt instant at which the represented session expires
 */
public record ClientSession(
    UserId actorId, PermissionSet permissions, AccessToken accessToken, Instant expiresAt) {
  /**
   * Creates the ClientSession value from the components documented below.
   *
   * @param actorId identifier of the user represented by the cached client session
   * @param permissions immutable snapshot of permissions granted to the represented session
   * @param accessToken credential presented to the server for session authentication
   * @param expiresAt instant at which the represented session expires
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public ClientSession {
    Objects.requireNonNull(actorId, "actorId");
    Objects.requireNonNull(permissions, "permissions");
    Objects.requireNonNull(accessToken, "accessToken");
    Objects.requireNonNull(expiresAt, "expiresAt");
  }
}
