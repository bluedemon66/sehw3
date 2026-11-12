package edu.nju.sehw.takeout.client.processing;

import edu.nju.sehw.takeout.model.resource.ResourceBundle;
import edu.nju.sehw.takeout.model.ui.PageResourceManifest;
import edu.nju.sehw.takeout.model.ui.UiValue;
import java.util.Map;
import java.util.Objects;

/**
 * Groups the context, manifest, resources, and values needed to assemble one UI page.
 *
 * @param context UI origin, root correlation, and operation arguments retained by the client
 * @param manifest layout-first list of resources required to assemble the page
 * @param resources local resource references collected for the page request
 * @param values typed business values keyed by UI element identifier
 */
public record UiPageAssembly(
    RequestContext context,
    PageResourceManifest manifest,
    ResourceBundle resources,
    Map<Integer, UiValue> values) {
  /**
   * Creates the UiPageAssembly value from the components documented below.
   *
   * @param context UI origin, root correlation, and operation arguments retained by the client
   * @param manifest layout-first list of resources required to assemble the page
   * @param resources local resource references collected for the page request
   * @param values typed business values keyed by UI element identifier
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public UiPageAssembly {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(manifest, "manifest");
    Objects.requireNonNull(resources, "resources");
    Objects.requireNonNull(values, "values");
    values = Map.copyOf(values);
  }
}
