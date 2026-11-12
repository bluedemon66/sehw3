package edu.nju.sehw.takeout.client.ui;

import edu.nju.sehw.takeout.model.resource.LocalResourceRef;
import edu.nju.sehw.takeout.model.ui.UiPage;
import edu.nju.sehw.takeout.model.ui.UiResourceNode;
import edu.nju.sehw.takeout.scaffold.NotImplementedYetException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Owns page resource trees and their element-to-resource bindings on the client. */
public final class UiResourceForest {

  /** Installed page identifiers mapped to their UI resource-tree roots. */
  private final Map<Integer, UiResourceNode> roots = new ConcurrentHashMap<>();

  /** Creates this component with the dependencies documented below. */
  public UiResourceForest() {}

  /**
   * Installs the assembled page root into the controller-owned page forest.
   *
   * @param page assembled page associated with the UI request being completed
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  public void install(UiPage page) {
    throw new NotImplementedYetException("UiResourceForest.install");
  }

  /**
   * Associates a local resource reference with an element of a stored page.
   *
   * @param pageId identifier of the page in the client's local page catalogue
   * @param elementId identifier of the UI element to read, populate, or bind
   * @param resource numeric local resource reference to bind to the UI element
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  public void bind(int pageId, int elementId, LocalResourceRef resource) {
    throw new NotImplementedYetException("UiResourceForest.bind");
  }

  /**
   * Looks up the resource-tree root for a stored page.
   *
   * @param pageId identifier of the page in the client's local page catalogue
   * @return the page root, or Optional.empty() when the page has not been installed
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  public Optional<UiResourceNode> findPage(int pageId) {
    throw new NotImplementedYetException("UiResourceForest.findPage");
  }
}
