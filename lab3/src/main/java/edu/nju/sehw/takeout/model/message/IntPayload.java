package edu.nju.sehw.takeout.model.message;

import java.util.List;
import java.util.Objects;

/**
 * Carries an ordered immutable list of integer arguments for numbered operations.
 *
 * @param values ordered integer arguments copied into an immutable list
 */
public record IntPayload(List<Integer> values) implements MessagePayload {
  /**
   * Creates the IntPayload value from the components documented below.
   *
   * @param values ordered integer arguments copied into an immutable list
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public IntPayload {
    Objects.requireNonNull(values, "values");
    values = List.copyOf(values);
  }

  /**
   * Creates a payload or message snapshot with independent mutable state.
   *
   * @return a separate snapshot preserving the same argument values and request identities
   */
  @Override
  public IntPayload deepCopy() {
    return new IntPayload(values);
  }
}
