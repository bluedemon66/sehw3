package edu.nju.sehw.takeout.model.resource;

import java.util.List;
import java.util.Objects;

/**
 * Locates a resource through numeric local catalogue indices and a resource kind.
 *
 * @param folderIndices ordered non-negative catalogue indices locating the resource's parent
 *     folders
 * @param entryIndex non-negative catalogue index of the resource within its selected folder
 * @param kind UI layout, image, text, or binary resource category
 */
public record LocalResourceRef(List<Integer> folderIndices, int entryIndex, ResourceKind kind) {
  /**
   * Creates the LocalResourceRef value from the components documented below.
   *
   * @param folderIndices ordered non-negative catalogue indices locating the resource's parent
   *     folders
   * @param entryIndex non-negative catalogue index of the resource within its selected folder
   * @param kind UI layout, image, text, or binary resource category
   * @throws NullPointerException if a required component, dependency, or collection element is null
   * @throws IllegalArgumentException if a folder index or the entry index is negative
   */
  public LocalResourceRef {
    Objects.requireNonNull(folderIndices, "folderIndices");
    folderIndices = List.copyOf(folderIndices);
    Objects.requireNonNull(kind, "kind");
    if (entryIndex < 0 || folderIndices.stream().anyMatch(i -> i < 0)) {
      throw new IllegalArgumentException("本地资源索引须为非负整数");
    }
  }
}
