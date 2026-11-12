package edu.nju.sehw.takeout.client.resource;

import edu.nju.sehw.takeout.model.ui.PageResourceManifest;
import java.util.Optional;

/** Defines lookup of a page's layout and resource manifest by local page identifier. */
public interface LocalUiLibrary {
  /**
   * Looks up the layout and resource manifest associated with a local page identifier.
   *
   * @param pageId identifier of the page in the client's local page catalogue
   * @return the page manifest, or Optional.empty() when the page is not registered
   */
  Optional<PageResourceManifest> findPage(int pageId);
}
