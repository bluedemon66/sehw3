package edu.nju.sehw.takeout.model.ui;

/**
 * Represents a boolean field value in a UI page or input.
 *
 * @param value boolean state displayed or submitted by the UI element
 */
public record BooleanValue(boolean value) implements UiValue {
  /**
   * Creates the BooleanValue value from the components documented below.
   *
   * @param value boolean state displayed or submitted by the UI element
   */
  public BooleanValue {}
}
