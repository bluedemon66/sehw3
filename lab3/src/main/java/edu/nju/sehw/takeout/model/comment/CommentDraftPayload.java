package edu.nju.sehw.takeout.model.comment;

import edu.nju.sehw.takeout.model.message.MessagePayload;
import edu.nju.sehw.takeout.model.resource.LocalResourceRef;
import java.util.List;
import java.util.Objects;

/**
 * Submits a comment and its local image drafts to the client service.
 *
 * @param comment comment text and existing attachment references
 * @param files local image files to upload before publishing
 */
public record CommentDraftPayload(PublishCommentPayload comment, List<LocalResourceRef> files)
    implements MessagePayload {
  /** Validates and snapshots the supplied fields. */
  public CommentDraftPayload {
    Objects.requireNonNull(comment, "comment");
    Objects.requireNonNull(files, "files");
    files = List.copyOf(files);
  }

  @Override
  public CommentDraftPayload deepCopy() {
    return new CommentDraftPayload(comment, files);
  }
}
