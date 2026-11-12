package edu.nju.sehw.takeout.model.ui;

import edu.nju.sehw.takeout.model.resource.ResourceDescriptor;
import edu.nju.sehw.takeout.model.resource.ResourceKind;
import java.util.List;
import java.util.Objects;

/**
 * Lists a page's layout first and the additional resources bound to its unique element identifiers.
 *
 * @param entries ordered resource descriptors; the first entry is the UI layout and element
 *     identifiers are unique
 */
public record PageResourceManifest(List<ResourceDescriptor> entries) {
  /**
   * Creates the PageResourceManifest value from the components documented below.
   *
   * @param entries ordered resource descriptors; the first entry is the UI layout and element
   *     identifiers are unique
   * @throws NullPointerException if a required component, dependency, or collection element is null
   * @throws IllegalArgumentException if the manifest lacks its leading UI layout or repeats an
   *     element identifier
   */
  public PageResourceManifest {
    Objects.requireNonNull(entries, "entries");
    entries = List.copyOf(entries);
    if (entries.isEmpty() || entries.get(0).kind() != ResourceKind.UI_LAYOUT) {
      throw new IllegalArgumentException("页面清单第零项为UI布局");
    }
    if (entries.stream().map(ResourceDescriptor::elementId).distinct().count() != entries.size()) {
      throw new IllegalArgumentException("页面元素编号须唯一");
    }
  }

  /**
   * Returns the leading layout descriptor required to assemble this page.
   *
   * @return the first descriptor, whose kind is UI_LAYOUT
   */
  public ResourceDescriptor uiLayout() {
    return entries.get(0);
  }
}
