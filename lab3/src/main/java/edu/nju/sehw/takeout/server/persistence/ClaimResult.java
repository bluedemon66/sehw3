package edu.nju.sehw.takeout.server.persistence;

import edu.nju.sehw.takeout.model.network.SecureResponse;
import java.util.Objects;
import java.util.Optional;

/**
 * Reports ownership or replay of an idempotent request and its optional saved response.
 *
 * @param status ownership, replay, progress, or digest-mismatch outcome of the idempotency claim
 * @param result remote request identifier, result status, and typed result payload
 */
public record ClaimResult(ClaimStatus status, Optional<SecureResponse> result) {
  /**
   * Creates the ClaimResult value from the components documented below.
   *
   * @param status ownership, replay, progress, or digest-mismatch outcome of the idempotency claim
   * @param result remote request identifier, result status, and typed result payload
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public ClaimResult {
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(result, "result");
  }
}
