package edu.nju.sehw.takeout.model.ui;

import java.util.Objects;

/**
 * Represents a text field value in a UI page or input.
 *
 * @param value text displayed or submitted by the UI element
 */
public record TextValue(String value) implements UiValue {
  /**
   * Creates the TextValue value from the components documented below.
   *
   * @param value text displayed or submitted by the UI element
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public TextValue {
    Objects.requireNonNull(value, "value");
  }
}
