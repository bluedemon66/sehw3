package edu.nju.sehw.takeout.server.task;

import edu.nju.sehw.takeout.server.security.ServerRequestContext;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

/**
 * Describes an authenticated upload staged at a server-controlled local path.
 *
 * @param context authenticated principal, server-established permissions, and remote request
 *     identity
 * @param stagingFile server-controlled local path containing the staged upload bytes
 * @param mediaType media type describing the resource bytes, such as image/png
 * @param contentLength declared length of the complete resource or staged upload in bytes
 * @param receivedAt instant at which the server received and staged the upload
 */
public record VerifiedUpload(
    ServerRequestContext context,
    Path stagingFile,
    String mediaType,
    long contentLength,
    Instant receivedAt) {
  /**
   * Creates the VerifiedUpload value from the components documented below.
   *
   * @param context authenticated principal, server-established permissions, and remote request
   *     identity
   * @param stagingFile server-controlled local path containing the staged upload bytes
   * @param mediaType media type describing the resource bytes, such as image/png
   * @param contentLength declared length of the complete resource or staged upload in bytes
   * @param receivedAt instant at which the server received and staged the upload
   * @throws NullPointerException if a required component, dependency, or collection element is null
   * @throws IllegalArgumentException if the declared byte length is negative
   */
  public VerifiedUpload {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(stagingFile, "stagingFile");
    Objects.requireNonNull(mediaType, "mediaType");
    Objects.requireNonNull(receivedAt, "receivedAt");
    if (contentLength < 0) {
      throw new IllegalArgumentException("上传长度须为非负数");
    }
  }
}
