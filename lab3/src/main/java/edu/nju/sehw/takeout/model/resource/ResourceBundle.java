package edu.nju.sehw.takeout.model.resource;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Associates a request with the local resources collected for its UI elements.
 *
 * @param requestId identifier of this individual request; retries reuse the same identifier
 * @param localResources local resource references keyed by the UI elements that use them
 */
public record ResourceBundle(UUID requestId, Map<Integer, LocalResourceRef> localResources) {
  /**
   * Creates the ResourceBundle value from the components documented below.
   *
   * @param requestId identifier of this individual request; retries reuse the same identifier
   * @param localResources local resource references keyed by the UI elements that use them
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public ResourceBundle {
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(localResources, "localResources");
    localResources = Map.copyOf(localResources);
  }
}
