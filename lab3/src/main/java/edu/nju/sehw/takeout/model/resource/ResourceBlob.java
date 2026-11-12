package edu.nju.sehw.takeout.model.resource;

import java.util.Objects;

/**
 * Carries a remote resource descriptor and an isolated copy of its complete bytes.
 *
 * @param reference server resource identifier together with media type and byte length
 * @param content complete resource bytes copied into the resource snapshot
 */
public record ResourceBlob(RemoteResourceRef reference, byte[] content) {
  /**
   * Creates the ResourceBlob value from the components documented below.
   *
   * @param reference server resource identifier together with media type and byte length
   * @param content complete resource bytes copied into the resource snapshot
   * @throws NullPointerException if a required component, dependency, or collection element is null
   * @throws IllegalArgumentException if the byte-array length differs from the declared resource
   *     length
   */
  public ResourceBlob {
    Objects.requireNonNull(reference, "reference");
    Objects.requireNonNull(content, "content");
    content = content.clone();
    if (reference.contentLength() != content.length) {
      throw new IllegalArgumentException("资源长度不匹配");
    }
  }

  /**
   * Returns a defensive copy of the stored resource bytes.
   *
   * @return a new array containing the stored resource bytes
   */
  @Override
  public byte[] content() {
    return content.clone();
  }
}
