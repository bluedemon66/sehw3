package edu.nju.sehw.takeout.model.resource;

import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Objects;

/**
 * Carries downloaded resource bytes and metadata as a response payload.
 *
 * @param resource complete resource metadata and downloaded bytes to store locally
 */
public record ResourcePayload(ResourceBlob resource) implements MessagePayload {
  /**
   * Creates the ResourcePayload value from the components documented below.
   *
   * @param resource complete resource metadata and downloaded bytes to store locally
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public ResourcePayload {
    Objects.requireNonNull(resource, "resource");
  }

  /**
   * Creates a payload or message snapshot with independent mutable state.
   *
   * @return a separate snapshot preserving the same argument values and request identities
   */
  @Override
  public ResourcePayload deepCopy() {
    return new ResourcePayload(resource);
  }
}
