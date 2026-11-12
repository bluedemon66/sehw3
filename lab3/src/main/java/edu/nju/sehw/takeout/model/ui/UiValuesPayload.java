package edu.nju.sehw.takeout.model.ui;

import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Map;
import java.util.Objects;

/**
 * Carries typed field values keyed by UI element identifier.
 *
 * @param values typed output values keyed by UI element identifier
 */
public record UiValuesPayload(Map<Integer, UiValue> values) implements MessagePayload {
  /**
   * Creates the UiValuesPayload value from the components documented below.
   *
   * @param values typed output values keyed by UI element identifier
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public UiValuesPayload {
    Objects.requireNonNull(values, "values");
    values = Map.copyOf(values);
  }

  /**
   * Creates a payload or message snapshot with independent mutable state.
   *
   * @return a separate snapshot preserving the same argument values and request identities
   */
  @Override
  public UiValuesPayload deepCopy() {
    return new UiValuesPayload(values);
  }
}
