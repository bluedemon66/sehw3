package edu.nju.sehw.takeout.model.ui;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Represents an exact decimal field value, such as a displayed monetary amount.
 *
 * @param value exact decimal displayed or submitted by the UI element
 */
public record DecimalValue(BigDecimal value) implements UiValue {
  /**
   * Creates the DecimalValue value from the components documented below.
   *
   * @param value exact decimal displayed or submitted by the UI element
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public DecimalValue {
    Objects.requireNonNull(value, "value");
  }
}
