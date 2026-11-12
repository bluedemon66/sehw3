package edu.nju.sehw.takeout.model.ui;

import java.util.Objects;

/**
 * Describes the page, element, action, and typed values of one UI interaction.
 *
 * @param pageId identifier of the page in the client's local page catalogue
 * @param elementId identifier of the UI element to read, populate, or bind
 * @param actionCode number identifying the action performed on the UI element
 * @param values typed values associated with this input or page
 */
public record UiInput(int pageId, int elementId, int actionCode, InputValues values) {
  /**
   * Creates the UiInput value from the components documented below.
   *
   * @param pageId identifier of the page in the client's local page catalogue
   * @param elementId identifier of the UI element to read, populate, or bind
   * @param actionCode number identifying the action performed on the UI element
   * @param values typed values associated with this input or page
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public UiInput {
    Objects.requireNonNull(values, "values");
  }
}
