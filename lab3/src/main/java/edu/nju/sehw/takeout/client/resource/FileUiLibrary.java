package edu.nju.sehw.takeout.client.resource;

import edu.nju.sehw.takeout.model.ui.PageResourceManifest;
import edu.nju.sehw.takeout.scaffold.NotImplementedYetException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Loads page resource manifests from a configured local UI library directory.
 *
 * <p>The root directory anchors lookup of page manifests and their local layout references.
 */
public final class FileUiLibrary implements LocalUiLibrary {
  /** Root directory containing the local page catalogue and layout resources. */
  private final Path root;

  /**
   * Creates this component with the dependencies documented below.
   *
   * @param root root directory containing the local page catalogue and layout resources
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public FileUiLibrary(Path root) {
    this.root = Objects.requireNonNull(root, "root");
  }

  /**
   * Looks up the layout and resource manifest associated with a local page identifier.
   *
   * @param pageId identifier of the page in the client's local page catalogue
   * @return the page manifest, or Optional.empty() when the page is not registered
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  @Override
  public Optional<PageResourceManifest> findPage(int pageId) {
    throw new NotImplementedYetException("FileUiLibrary.findPage");
  }
}
