package edu.nju.sehw.takeout.server.security;

import edu.nju.sehw.takeout.model.network.SecureRequest;

/** Defines validation of an incoming request and creation of its authenticated server context. */
public interface ServerRequestVerifier {
  /**
   * Validates the request and resolves its authoritative session identity and permissions.
   *
   * @param request outgoing or received request envelope containing credential, timestamp, digest,
   *     and message
   * @return the server-authenticated principal, permissions, and request identifier
   */
  ServerRequestContext verify(SecureRequest request);
}
