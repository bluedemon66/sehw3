package edu.nju.sehw.takeout.client.resource;

import edu.nju.sehw.takeout.model.resource.LocalResourceRef;
import edu.nju.sehw.takeout.model.resource.ResourceDescriptor;
import edu.nju.sehw.takeout.model.resource.ResourceKind;
import edu.nju.sehw.takeout.model.ui.PageResourceManifest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

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
   * @throws IllegalArgumentException if the page identifier or manifest is invalid
   * @throws UncheckedIOException if the manifest cannot be read
   */
  @Override
  public Optional<PageResourceManifest> findPage(int pageId) {
    if (pageId <= 0) {
      throw new IllegalArgumentException("pageId must be positive");
    }
    Path path = root.resolve("pages").resolve(pageId + ".properties");
    Properties fields = new Properties();
    try (var reader = Files.newBufferedReader(path)) {
      fields.load(reader);
      int count = Integer.parseInt(required(fields, "entry.count"));
      if (count <= 0 || count > fields.size()) {
        throw new IllegalArgumentException("Invalid entry.count");
      }
      var entries = new ArrayList<ResourceDescriptor>();
      for (int i = 0; i < count; i++) {
        String prefix = "entry." + i + ".";
        try {
          int element = Integer.parseInt(required(fields, prefix + "elementId"));
          ResourceKind kind = ResourceKind.valueOf(required(fields, prefix + "kind"));
          Optional<LocalResourceRef> local =
              Optional.ofNullable(fields.getProperty(prefix + "folders"))
                  .map(
                      folders ->
                          new LocalResourceRef(
                              Arrays.stream(folders.split(",", -1))
                                  .map(String::trim)
                                  .map(Integer::parseInt)
                                  .toList(),
                              Integer.parseInt(required(fields, prefix + "entryIndex")),
                              kind));
          Optional<String> remote =
              Optional.ofNullable(fields.getProperty(prefix + "remoteResourceId"))
                  .map(String::strip)
                  .filter(value -> !value.isEmpty());
          entries.add(new ResourceDescriptor(element, local, remote, kind));
        } catch (IllegalArgumentException ex) {
          throw new IllegalArgumentException("Invalid fields in " + prefix + ex.getMessage(), ex);
        }
      }
      return Optional.of(new PageResourceManifest(entries));
    } catch (NoSuchFileException ex) {
      return Optional.empty();
    } catch (IOException ex) {
      throw new UncheckedIOException("Cannot read " + path, ex);
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException(
          "Invalid page manifest " + path + ". " + ex.getMessage(), ex);
    }
  }

  private static String required(Properties fields, String key) {
    String value = fields.getProperty(key);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Missing " + key);
    }
    return value.strip();
  }
}
