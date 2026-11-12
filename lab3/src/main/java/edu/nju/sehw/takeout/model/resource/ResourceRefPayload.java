package edu.nju.sehw.takeout.model.resource;

import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Objects;

/**
 * Carries the server's reference to a stored resource after upload.
 *
 * @param reference server resource identifier together with media type and byte length
 */
public record ResourceRefPayload(RemoteResourceRef reference) implements MessagePayload {
  /**
   * Creates the ResourceRefPayload value from the components documented below.
   *
   * @param reference server resource identifier together with media type and byte length
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public ResourceRefPayload {
    Objects.requireNonNull(reference, "reference");
  }

  /**
   * Creates a payload or message snapshot with independent mutable state.
   *
   * @return a separate snapshot preserving the same argument values and request identities
   */
  @Override
  public ResourceRefPayload deepCopy() {
    return new ResourceRefPayload(reference);
  }
}
