package edu.nju.sehw.takeout.model.ui;

import java.util.Objects;
import java.util.UUID;

/**
 * Carries an assembled page tree and the request identifier used to match the active UI operation.
 *
 * @param pageId identifier of the page in the client's local page catalogue
 * @param requestId identifier of this individual request; retries reuse the same identifier
 * @param root root node of the assembled UI element and resource tree
 */
public record UiPage(int pageId, UUID requestId, UiResourceNode root) {
  /**
   * Creates the UiPage value from the components documented below.
   *
   * @param pageId identifier of the page in the client's local page catalogue
   * @param requestId identifier of this individual request; retries reuse the same identifier
   * @param root root node of the assembled UI element and resource tree
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public UiPage {
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(root, "root");
  }
}
