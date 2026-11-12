package edu.nju.sehw.takeout.model.common;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifies the kind and identifier of an object subject to an authorization check.
 *
 * @param kind business object category understood by the authorization checker
 * @param targetId registered identifier of the participant that should receive the message
 */
public record TargetDescriptor(String kind, UUID targetId) {
  /**
   * Creates the TargetDescriptor value from the components documented below.
   *
   * @param kind business object category understood by the authorization checker
   * @param targetId registered identifier of the participant that should receive the message
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public TargetDescriptor {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(targetId, "targetId");
  }
}
