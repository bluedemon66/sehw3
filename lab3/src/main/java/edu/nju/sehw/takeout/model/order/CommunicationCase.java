package edu.nju.sehw.takeout.model.order;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores an exception order's communication case and its recorded resolution.
 *
 * <p>Resolving this case records specialist communication progress; the related order retains its
 * EXCEPTION state.
 *
 * @param caseId stable identifier of the specialist communication case
 * @param orderId stable identifier of the order associated with this command, snapshot, or review
 *     target
 * @param status open or resolved state of the specialist communication case
 * @param createdAt instant at which the represented persistent record was created
 * @param resolution recorded outcome of specialist communication for the exception order
 * @param resolvedAt optional instant at which the communication case was resolved
 */
public record CommunicationCase(
    UUID caseId,
    UUID orderId,
    CaseStatus status,
    Instant createdAt,
    Optional<String> resolution,
    Optional<Instant> resolvedAt) {
  /**
   * Creates the CommunicationCase value from the components documented below.
   *
   * @param caseId stable identifier of the specialist communication case
   * @param orderId stable identifier of the order associated with this command, snapshot, or review
   *     target
   * @param status open or resolved state of the specialist communication case
   * @param createdAt instant at which the represented persistent record was created
   * @param resolution recorded outcome of specialist communication for the exception order
   * @param resolvedAt optional instant at which the communication case was resolved
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public CommunicationCase {
    Objects.requireNonNull(caseId, "caseId");
    Objects.requireNonNull(orderId, "orderId");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(createdAt, "createdAt");
    Objects.requireNonNull(resolution, "resolution");
    Objects.requireNonNull(resolvedAt, "resolvedAt");
  }
}
