package edu.nju.sehw.takeout.model.comment;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Identifies the shop, dish, or delivery service being reviewed and its relevant parent
 * identifiers.
 *
 * @param kind category of the reviewed shop, dish, or delivery service
 * @param targetId registered identifier of the participant that should receive the message
 * @param shopId identifier of the shop owning the order or review target
 * @param orderId stable identifier of the order associated with this command, snapshot, or review
 *     target
 */
public record CommentSubject(
    SubjectKind kind, UUID targetId, Optional<UUID> shopId, Optional<UUID> orderId) {
  /**
   * Creates the CommentSubject value from the components documented below.
   *
   * @param kind category of the reviewed shop, dish, or delivery service
   * @param targetId registered identifier of the participant that should receive the message
   * @param shopId identifier of the shop owning the order or review target
   * @param orderId stable identifier of the order associated with this command, snapshot, or review
   *     target
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public CommentSubject {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(targetId, "targetId");
    Objects.requireNonNull(shopId, "shopId");
    Objects.requireNonNull(orderId, "orderId");
  }
}
