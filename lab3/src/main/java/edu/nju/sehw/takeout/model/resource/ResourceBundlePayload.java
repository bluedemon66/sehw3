package edu.nju.sehw.takeout.model.resource;

import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Objects;

/**
 * Carries a completed collection of local page resources in a message.
 *
 * @param resources local resource references collected for the page request
 */
public record ResourceBundlePayload(ResourceBundle resources) implements MessagePayload {
  /**
   * Creates the ResourceBundlePayload value from the components documented below.
   *
   * @param resources local resource references collected for the page request
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public ResourceBundlePayload {
    Objects.requireNonNull(resources, "resources");
  }

  /**
   * Creates a payload or message snapshot with independent mutable state.
   *
   * @return a separate snapshot preserving the same argument values and request identities
   */
  @Override
  public ResourceBundlePayload deepCopy() {
    return new ResourceBundlePayload(resources);
  }
}
