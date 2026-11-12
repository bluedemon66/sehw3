package edu.nju.sehw.takeout.model.resource;

import java.util.Objects;
import java.util.Optional;

/**
 * Describes one UI element's resource using local and remote location hints.
 *
 * @param elementId identifier of the UI element to read, populate, or bind
 * @param localRef optional numeric local reference for an already available resource
 * @param remoteResourceId optional server identifier from which this resource can be retrieved
 * @param kind UI layout, image, text, or binary resource category
 */
public record ResourceDescriptor(
    int elementId,
    Optional<LocalResourceRef> localRef,
    Optional<String> remoteResourceId,
    ResourceKind kind) {
  /**
   * Creates the ResourceDescriptor value from the components documented below.
   *
   * @param elementId identifier of the UI element to read, populate, or bind
   * @param localRef optional numeric local reference for an already available resource
   * @param remoteResourceId optional server identifier from which this resource can be retrieved
   * @param kind UI layout, image, text, or binary resource category
   * @throws NullPointerException if a required component, dependency, or collection element is null
   * @throws IllegalArgumentException if both local and remote location hints are empty
   */
  public ResourceDescriptor {
    Objects.requireNonNull(localRef, "localRef");
    Objects.requireNonNull(remoteResourceId, "remoteResourceId");
    Objects.requireNonNull(kind, "kind");
    if (localRef.isEmpty() && remoteResourceId.isEmpty()) {
      throw new IllegalArgumentException("资源需要本地或远端定位信息");
    }
  }
}
