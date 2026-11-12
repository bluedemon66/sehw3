package edu.nju.sehw.takeout.client.query;

import java.util.Objects;
import java.util.UUID;

/**
 * Relates a remote request to its root operation, originating participant, and UI receiver.
 *
 * @param requestId identifier of this individual request; retries reuse the same identifier
 * @param correlationId identifier shared by the child requests of one root UI operation
 * @param sourceParticipantId registered participant to which the remote result should be routed
 * @param sourceUiId registered participant identifier of the UI that should receive the final page
 */
public record ReplyRoute(
    UUID requestId, UUID correlationId, int sourceParticipantId, int sourceUiId) {
  /**
   * Creates the ReplyRoute value from the components documented below.
   *
   * @param requestId identifier of this individual request; retries reuse the same identifier
   * @param correlationId identifier shared by the child requests of one root UI operation
   * @param sourceParticipantId registered participant to which the remote result should be routed
   * @param sourceUiId registered participant identifier of the UI that should receive the final
   *     page
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public ReplyRoute {
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(correlationId, "correlationId");
  }
}
