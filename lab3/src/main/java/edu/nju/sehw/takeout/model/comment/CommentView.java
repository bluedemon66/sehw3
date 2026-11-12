package edu.nju.sehw.takeout.model.comment;

import edu.nju.sehw.takeout.model.resource.RemoteResourceRef;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Carries a display projection of a comment together with visible images and merchant replies.
 *
 * @param commentId stable identifier of the comment being read or changed
 * @param subject kind and identifiers of the shop, dish, or delivery service being reviewed
 * @param authorLabel public display label of the comment author
 * @param text comment or reply text supplied for validation, persistence, or display
 * @param purchaseMark purchase-evidence label calculated by the server for the review author
 * @param status publication, withdrawal, or platform visibility state of the comment
 * @param createdAt instant at which the represented persistent record was created
 * @param images ordered remote image references visible with the comment
 * @param replies merchant reply projections visible with the comment
 */
public record CommentView(
    UUID commentId,
    CommentSubject subject,
    String authorLabel,
    String text,
    PurchaseMark purchaseMark,
    CommentStatus status,
    Instant createdAt,
    List<RemoteResourceRef> images,
    List<ReplyView> replies) {
  /**
   * Creates the CommentView value from the components documented below.
   *
   * @param commentId stable identifier of the comment being read or changed
   * @param subject kind and identifiers of the shop, dish, or delivery service being reviewed
   * @param authorLabel public display label of the comment author
   * @param text comment or reply text supplied for validation, persistence, or display
   * @param purchaseMark purchase-evidence label calculated by the server for the review author
   * @param status publication, withdrawal, or platform visibility state of the comment
   * @param createdAt instant at which the represented persistent record was created
   * @param images ordered remote image references visible with the comment
   * @param replies merchant reply projections visible with the comment
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public CommentView {
    Objects.requireNonNull(commentId, "commentId");
    Objects.requireNonNull(subject, "subject");
    Objects.requireNonNull(authorLabel, "authorLabel");
    Objects.requireNonNull(text, "text");
    Objects.requireNonNull(purchaseMark, "purchaseMark");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(createdAt, "createdAt");
    Objects.requireNonNull(images, "images");
    images = List.copyOf(images);
    Objects.requireNonNull(replies, "replies");
    replies = List.copyOf(replies);
  }

  /**
   * Checks whether the public comment projection contains visible images.
   *
   * @return true when the image-reference list is nonempty
   */
  public boolean hasImage() {
    return !images.isEmpty();
  }
}
