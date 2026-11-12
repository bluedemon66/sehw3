package edu.nju.sehw.takeout.model.ui;

import edu.nju.sehw.takeout.model.resource.LocalResourceRef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Stores one immutable UI element node, its optional resource and value, and its ordered child
 * nodes.
 *
 * @param elementId identifier of the UI element to read, populate, or bind
 * @param resource numeric local resource reference to bind to the UI element
 * @param value optional typed field value associated with this node
 * @param children child UI nodes in display order
 */
public record UiResourceNode(
    int elementId,
    Optional<LocalResourceRef> resource,
    Optional<UiValue> value,
    List<UiResourceNode> children) {
  /**
   * Creates the UiResourceNode value from the components documented below.
   *
   * @param elementId identifier of the UI element to read, populate, or bind
   * @param resource numeric local resource reference to bind to the UI element
   * @param value optional typed field value associated with this node
   * @param children child UI nodes in display order
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public UiResourceNode {
    Objects.requireNonNull(resource, "resource");
    Objects.requireNonNull(value, "value");
    Objects.requireNonNull(children, "children");
    children = List.copyOf(children);
  }
}
