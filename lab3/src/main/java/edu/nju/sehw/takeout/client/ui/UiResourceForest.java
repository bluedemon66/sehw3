package edu.nju.sehw.takeout.client.ui;

import edu.nju.sehw.takeout.model.resource.LocalResourceRef;
import edu.nju.sehw.takeout.model.ui.UiPage;
import edu.nju.sehw.takeout.model.ui.UiResourceNode;
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
   */
  public void install(UiPage page) {
    roots.put(page.pageId(), page.root());
  }

  /**
   * Associates a local resource reference with an element of a stored page.
   *
   * @param pageId identifier of the page in the client's local page catalogue
   * @param elementId identifier of the UI element to read, populate, or bind
   * @param resource numeric local resource reference to bind to the UI element
   */
  public void bind(int pageId, int elementId, LocalResourceRef resource) {
    roots.computeIfPresent(pageId, (id, root) -> bindNode(root, elementId, resource));
  }

  /**
   * Looks up the resource-tree root for a stored page.
   *
   * @param pageId identifier of the page in the client's local page catalogue
   * @return the page root, or Optional.empty() when the page has not been installed
   */
  public Optional<UiResourceNode> findPage(int pageId) {
    return Optional.ofNullable(roots.get(pageId));
  }

  private UiResourceNode bindNode(UiResourceNode node, int elementId, LocalResourceRef resource) {
    if (node.elementId() == elementId) {
      return new UiResourceNode(elementId, Optional.of(resource), node.value(), node.children());
    }
    var children =
        node.children().stream().map(child -> bindNode(child, elementId, resource)).toList();
    return children.equals(node.children())
        ? node
        : new UiResourceNode(node.elementId(), node.resource(), node.value(), children);
  }
}
