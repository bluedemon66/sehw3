package edu.nju.sehw.takeout.model.resource;

import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Objects;

/**
 * Binds staged upload bytes to the verified request.
 *
 * @param mediaType declared image media type
 * @param contentLength byte length of the upload
 * @param contentDigest hex SHA-256 digest of the upload
 */
public record AttachmentUploadPayload(String mediaType, long contentLength, String contentDigest)
    implements MessagePayload {
  /** Validates and snapshots the supplied fields. */
  public AttachmentUploadPayload {
    Objects.requireNonNull(mediaType, "mediaType");
    Objects.requireNonNull(contentDigest, "contentDigest");
    if (!mediaType.matches("image/(png|jpeg|gif)")
        || contentLength < 1
        || contentLength > 5 * 1024 * 1024
        || !contentDigest.matches("[0-9a-f]{64}")) {
      throw new IllegalArgumentException("Invalid image attachment metadata");
    }
  }

  @Override
  public AttachmentUploadPayload deepCopy() {
    return new AttachmentUploadPayload(mediaType, contentLength, contentDigest);
  }
}
