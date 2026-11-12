package edu.nju.sehw.takeout.model.resource;

import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Objects;

/**
 * Carries the resource descriptor required by a download or cache lookup.
 *
 * @param descriptor resource identity, kind, and available local or remote location hints
 */
public record ResourceRequestPayload(ResourceDescriptor descriptor) implements MessagePayload {
  /**
   * Creates the ResourceRequestPayload value from the components documented below.
   *
   * @param descriptor resource identity, kind, and available local or remote location hints
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public ResourceRequestPayload {
    Objects.requireNonNull(descriptor, "descriptor");
  }

  /**
   * Creates a payload or message snapshot with independent mutable state.
   *
   * @return a separate snapshot preserving the same argument values and request identities
   */
  @Override
  public ResourceRequestPayload deepCopy() {
    return new ResourceRequestPayload(descriptor);
  }
}
