package edu.nju.sehw.takeout.server.security;

import edu.nju.sehw.takeout.model.network.AccessToken;
import java.util.Optional;

/**
 * Defines lookup of the authoritative principal and expiry associated with an access credential.
 */
public interface ServerSessionStore {
  /**
   * Looks up the authoritative principal associated with the supplied credential.
   *
   * @param token credential used to look up the authoritative server session
   * @return the matching principal and expiry, or Optional.empty() for an unknown session
   */
  Optional<SessionPrincipal> find(AccessToken token);
}
