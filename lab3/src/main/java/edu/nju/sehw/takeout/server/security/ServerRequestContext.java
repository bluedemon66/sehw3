package edu.nju.sehw.takeout.server.security;

import edu.nju.sehw.takeout.model.common.PermissionSet;
import edu.nju.sehw.takeout.model.common.UserId;
import java.util.Objects;
import java.util.UUID;

/**
 * Carries the principal, permissions, and request identity established by server verification.
 *
 * @param principalId identifier of the principal established by server verification
 * @param verifiedPermissions permissions established by server-side session verification
 * @param requestId identifier of this individual request; retries reuse the same identifier
 */
public record ServerRequestContext(
    UserId principalId, PermissionSet verifiedPermissions, UUID requestId) {
  /**
   * Creates the ServerRequestContext value from the components documented below.
   *
   * @param principalId identifier of the principal established by server verification
   * @param verifiedPermissions permissions established by server-side session verification
   * @param requestId identifier of this individual request; retries reuse the same identifier
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public ServerRequestContext {
    Objects.requireNonNull(principalId, "principalId");
    Objects.requireNonNull(verifiedPermissions, "verifiedPermissions");
    Objects.requireNonNull(requestId, "requestId");
  }
}
