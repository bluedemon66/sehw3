package edu.nju.sehw.takeout.model.common;

import java.util.Objects;

/**
 * Reports whether an operation check passed and supplies a machine-readable code and explanation.
 *
 * @param allowed whether the requested permission or validation check passed
 * @param code machine-readable code describing this check or error
 * @param description displayable explanation associated with this check or routing outcome
 */
public record CheckResult(boolean allowed, String code, String description) {
  /**
   * Creates the CheckResult value from the components documented below.
   *
   * @param allowed whether the requested permission or validation check passed
   * @param code machine-readable code describing this check or error
   * @param description displayable explanation associated with this check or routing outcome
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public CheckResult {
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(description, "description");
  }
}
