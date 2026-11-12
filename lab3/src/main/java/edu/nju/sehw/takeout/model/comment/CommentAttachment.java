package edu.nju.sehw.takeout.model.comment;

import java.util.Objects;
import java.util.UUID;

/**
 * Associates a remote image resource and its display position with a comment.
 *
 * @param commentId stable identifier of the comment being read or changed
 * @param resourceId opaque identifier assigned to a resource by the server
 * @param position zero-based display position of an attachment within its parent comment
 * @param mediaType media type describing the resource bytes, such as image/png
 */
public record CommentAttachment(UUID commentId, String resourceId, int position, String mediaType) {
  /**
   * Creates the CommentAttachment value from the components documented below.
   *
   * @param commentId stable identifier of the comment being read or changed
   * @param resourceId opaque identifier assigned to a resource by the server
   * @param position zero-based display position of an attachment within its parent comment
   * @param mediaType media type describing the resource bytes, such as image/png
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public CommentAttachment {
    Objects.requireNonNull(commentId, "commentId");
    Objects.requireNonNull(resourceId, "resourceId");
    Objects.requireNonNull(mediaType, "mediaType");
  }
}
