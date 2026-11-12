package edu.nju.sehw.takeout.model.ui;

import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Objects;

/**
 * Carries a UI interaction into the Core message flow.
 *
 * @param input page, element, action, and typed input values produced by the UI
 */
public record UiInputPayload(UiInput input) implements MessagePayload {
  /**
   * Creates the UiInputPayload value from the components documented below.
   *
   * @param input page, element, action, and typed input values produced by the UI
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public UiInputPayload {
    Objects.requireNonNull(input, "input");
  }

  /**
   * Creates a payload or message snapshot with independent mutable state.
   *
   * @return a separate snapshot preserving the same argument values and request identities
   */
  @Override
  public UiInputPayload deepCopy() {
    return new UiInputPayload(input);
  }
}
