package edu.nju.sehw.takeout.model.comment;

import edu.nju.sehw.takeout.model.common.UserId;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores a comment's identity, author, target, text, publication state, and lifecycle timestamps.
 *
 * @param commentId stable identifier of the comment being read or changed
 * @param authorId identifier of the user who authored the comment
 * @param subject kind and identifiers of the shop, dish, or delivery service being reviewed
 * @param text comment or reply text supplied for validation, persistence, or display
 * @param purchaseMark purchase-evidence label calculated by the server for the review author
 * @param status publication, withdrawal, or platform visibility state of the comment
 * @param createdAt instant at which the represented persistent record was created
 * @param withdrawnAt optional instant at which the author withdrew the comment
 */
public record Comment(
    UUID commentId,
    UserId authorId,
    CommentSubject subject,
    String text,
    PurchaseMark purchaseMark,
    CommentStatus status,
    Instant createdAt,
    Optional<Instant> withdrawnAt) {
  /**
   * Creates the Comment value from the components documented below.
   *
   * @param commentId stable identifier of the comment being read or changed
   * @param authorId identifier of the user who authored the comment
   * @param subject kind and identifiers of the shop, dish, or delivery service being reviewed
   * @param text comment or reply text supplied for validation, persistence, or display
   * @param purchaseMark purchase-evidence label calculated by the server for the review author
   * @param status publication, withdrawal, or platform visibility state of the comment
   * @param createdAt instant at which the represented persistent record was created
   * @param withdrawnAt optional instant at which the author withdrew the comment
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public Comment {
    Objects.requireNonNull(commentId, "commentId");
    Objects.requireNonNull(authorId, "authorId");
    Objects.requireNonNull(subject, "subject");
    Objects.requireNonNull(text, "text");
    Objects.requireNonNull(purchaseMark, "purchaseMark");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(createdAt, "createdAt");
    Objects.requireNonNull(withdrawnAt, "withdrawnAt");
  }
}
