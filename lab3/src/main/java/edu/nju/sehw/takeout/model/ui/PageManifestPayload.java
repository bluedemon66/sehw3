package edu.nju.sehw.takeout.model.ui;

import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Objects;

/**
 * Carries a page's layout and resource manifest in a message.
 *
 * @param manifest layout-first list of resources required to assemble the page
 */
public record PageManifestPayload(PageResourceManifest manifest) implements MessagePayload {
  /**
   * Creates the PageManifestPayload value from the components documented below.
   *
   * @param manifest layout-first list of resources required to assemble the page
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public PageManifestPayload {
    Objects.requireNonNull(manifest, "manifest");
  }

  /**
   * Creates a payload or message snapshot with independent mutable state.
   *
   * @return a separate snapshot preserving the same argument values and request identities
   */
  @Override
  public PageManifestPayload deepCopy() {
    return new PageManifestPayload(manifest);
  }
}
