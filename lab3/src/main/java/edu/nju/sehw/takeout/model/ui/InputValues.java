package edu.nju.sehw.takeout.model.ui;

import java.util.Map;
import java.util.Objects;

/**
 * Associates UI element identifiers with their submitted typed values.
 *
 * @param values submitted typed values keyed by input element identifier
 */
public record InputValues(Map<Integer, UiValue> values) {
  /**
   * Creates the InputValues value from the components documented below.
   *
   * @param values submitted typed values keyed by input element identifier
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public InputValues {
    Objects.requireNonNull(values, "values");
    values = Map.copyOf(values);
  }
}
