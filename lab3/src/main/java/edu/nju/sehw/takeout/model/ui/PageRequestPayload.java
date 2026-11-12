package edu.nju.sehw.takeout.model.ui;

import edu.nju.sehw.takeout.model.message.MessagePayload;

/**
 * Carries the local page identifier selected for navigation or refresh.
 *
 * @param pageId identifier of the page in the client's local page catalogue
 */
public record PageRequestPayload(int pageId) implements MessagePayload {
  /**
   * Creates the PageRequestPayload value from the components documented below.
   *
   * @param pageId identifier of the page in the client's local page catalogue
   */
  public PageRequestPayload {}

  /**
   * Creates a payload or message snapshot with independent mutable state.
   *
   * @return a separate snapshot preserving the same argument values and request identities
   */
  @Override
  public PageRequestPayload deepCopy() {
    return new PageRequestPayload(pageId);
  }
}
