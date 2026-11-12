package edu.nju.sehw.takeout.model.ui;

import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Objects;

/**
 * Carries an assembled UI page as a message payload.
 *
 * @param page assembled page associated with the UI request being completed
 */
public record UiPagePayload(UiPage page) implements MessagePayload {
  /**
   * Creates the UiPagePayload value from the components documented below.
   *
   * @param page assembled page associated with the UI request being completed
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public UiPagePayload {
    Objects.requireNonNull(page, "page");
  }

  /**
   * Creates a payload or message snapshot with independent mutable state.
   *
   * @return a separate snapshot preserving the same argument values and request identities
   */
  @Override
  public UiPagePayload deepCopy() {
    return new UiPagePayload(page);
  }
}
