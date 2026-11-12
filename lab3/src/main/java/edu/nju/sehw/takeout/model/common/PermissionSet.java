package edu.nju.sehw.takeout.model.common;

import java.util.Objects;
import java.util.Set;

/**
 * Stores an immutable snapshot of granted permissions.
 *
 * @param values granted permissions copied into an immutable set
 */
public record PermissionSet(Set<Permission> values) {
  /**
   * Creates the PermissionSet value from the components documented below.
   *
   * @param values granted permissions copied into an immutable set
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public PermissionSet {
    Objects.requireNonNull(values, "values");
    values = Set.copyOf(values);
  }

  /**
   * Tests whether the granted permission snapshot contains the selected permission.
   *
   * @param permission permission to test for membership in the granted set
   * @return true when the permission is present in this snapshot
   * @throws NullPointerException if permission is null
   */
  public boolean contains(Permission permission) {
    return values.contains(permission);
  }
}
