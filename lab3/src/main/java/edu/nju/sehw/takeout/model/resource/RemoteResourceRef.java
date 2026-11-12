package edu.nju.sehw.takeout.model.resource;

import java.util.Objects;

/**
 * Identifies a server-owned resource together with its media type and byte length.
 *
 * @param resourceId opaque identifier assigned to a resource by the server
 * @param mediaType media type describing the resource bytes, such as image/png
 * @param contentLength declared length of the complete resource or staged upload in bytes
 */
public record RemoteResourceRef(String resourceId, String mediaType, long contentLength) {
  /**
   * Creates the RemoteResourceRef value from the components documented below.
   *
   * @param resourceId opaque identifier assigned to a resource by the server
   * @param mediaType media type describing the resource bytes, such as image/png
   * @param contentLength declared length of the complete resource or staged upload in bytes
   * @throws NullPointerException if a required component, dependency, or collection element is null
   * @throws IllegalArgumentException if the declared byte length is negative
   */
  public RemoteResourceRef {
    Objects.requireNonNull(resourceId, "resourceId");
    Objects.requireNonNull(mediaType, "mediaType");
    if (contentLength < 0) {
      throw new IllegalArgumentException("资源长度须为非负数");
    }
  }
}
