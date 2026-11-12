package edu.nju.sehw.takeout.model.common;

import java.util.Objects;
import java.util.UUID;

/**
 * Wraps the stable UUID of an application user.
 *
 * @param value stable UUID identifying the user
 */
public record UserId(UUID value) {
  /**
   * Creates the UserId value from the components documented below.
   *
   * @param value stable UUID identifying the user
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public UserId {
    Objects.requireNonNull(value, "value");
  }
}
