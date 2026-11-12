package edu.nju.sehw.takeout.model.message;

import java.util.Objects;

/**
 * Carries a machine-readable error code and a displayable explanation.
 *
 * @param code machine-readable code describing this check or error
 * @param description displayable explanation associated with this check or routing outcome
 */
public record ErrorPayload(String code, String description) implements MessagePayload {
  /**
   * Creates the ErrorPayload value from the components documented below.
   *
   * @param code machine-readable code describing this check or error
   * @param description displayable explanation associated with this check or routing outcome
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public ErrorPayload {
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(description, "description");
  }

  /**
   * Creates a payload or message snapshot with independent mutable state.
   *
   * @return a separate snapshot preserving the same argument values and request identities
   */
  @Override
  public ErrorPayload deepCopy() {
    return new ErrorPayload(code, description);
  }
}
