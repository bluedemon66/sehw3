package edu.nju.sehw.takeout.examples;

import edu.nju.sehw.takeout.model.resource.LocalResourceRef;
import edu.nju.sehw.takeout.model.resource.RemoteResourceRef;
import edu.nju.sehw.takeout.model.resource.ResourceBlob;
import edu.nju.sehw.takeout.model.resource.ResourceDescriptor;
import edu.nju.sehw.takeout.model.resource.ResourceKind;
import edu.nju.sehw.takeout.model.ui.PageResourceManifest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/** Reads a real manifest, resolves numeric references, and writes a small cache example. */
public final class FileLayerWalkthrough {
  private static int checks;

  private FileLayerWalkthrough() {}

  /**
   * Reads one numbered page manifest and builds the project's existing value objects.
   *
   * @param root directory containing pages and packaged resources
   * @param pageId page number used in the manifest filename
   * @return the parsed manifest, or an empty optional for a missing page file
   * @throws IllegalArgumentException if a required property or model value is invalid
   * @throws UncheckedIOException if an existing manifest cannot be read
   */
  public static Optional<PageResourceManifest> findPage(Path root, int pageId) {
    Path file = root.resolve("pages").resolve(pageId + ".properties");
    Properties properties = new Properties();
    try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      properties.load(reader);
    } catch (NoSuchFileException missing) {
      return Optional.empty();
    } catch (IOException failure) {
      throw new UncheckedIOException("Cannot read page manifest: " + file, failure);
    }
    int count = Integer.parseInt(required(properties, "entry.count"));
    List<ResourceDescriptor> entries = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String prefix = "entry." + i + ".";
      int elementId = Integer.parseInt(required(properties, prefix + "elementId"));
      ResourceKind kind = ResourceKind.valueOf(required(properties, prefix + "kind"));
      List<Integer> folders =
          Arrays.stream(required(properties, prefix + "folders").split(","))
              .map(String::trim)
              .map(Integer::parseInt)
              .toList();
      int entryIndex = Integer.parseInt(required(properties, prefix + "entryIndex"));
      LocalResourceRef local = new LocalResourceRef(folders, entryIndex, kind);
      entries.add(new ResourceDescriptor(elementId, Optional.of(local), Optional.empty(), kind));
    }
    return Optional.of(new PageResourceManifest(entries));
  }

  /**
   * Maps the leading selector to a root and the remaining numbers to directory names.
   *
   * @param resRoot packaged-resource root selected by zero
   * @param cacheRoot downloaded-resource root selected by one
   * @param reference numeric local resource reference to resolve
   * @return a path assembled from the chosen root and numeric names
   * @throws IllegalArgumentException if the storage selector is missing or unknown
   */
  public static Path resolve(Path resRoot, Path cacheRoot, LocalResourceRef reference) {
    List<Integer> folders = reference.folderIndices();
    if (folders.isEmpty()) {
      throw new IllegalArgumentException("A storage selector is required");
    }
    Path selected =
        switch (folders.get(0)) {
          case 0 -> resRoot;
          case 1 -> cacheRoot;
          default ->
              throw new IllegalArgumentException("Unknown storage selector: " + folders.get(0));
        };
    for (int i = 1; i < folders.size(); i++) {
      selected = selected.resolve(Integer.toString(folders.get(i)));
    }
    return selected.resolve(Integer.toString(reference.entryIndex()));
  }

  /** Returns the trimmed value of a required property. */
  private static String required(Properties properties, String key) {
    String value = properties.getProperty(key);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Missing property: " + key);
    }
    return value.trim();
  }

  /**
   * Writes one text resource and a readable index into the supplied example cache.
   *
   * @param resRoot packaged-resource root used by the shared resolver
   * @param cacheRoot example output directory
   * @param resource remote text metadata and bytes to save
   * @return the fixed reference used for this repeatable exercise
   * @throws IOException if the resource or index cannot be written
   */
  private static LocalResourceRef writeCacheExample(
      Path resRoot, Path cacheRoot, ResourceBlob resource) throws IOException {
    LocalResourceRef local = new LocalResourceRef(List.of(1, 0), 1, ResourceKind.TEXT);
    Path file = resolve(resRoot, cacheRoot, local);
    Files.createDirectories(file.getParent());
    Files.write(file, resource.content());
    Properties index = new Properties();
    index.setProperty("resource." + resource.reference().resourceId(), "1,0|1|TEXT");
    index.setProperty("next.entry", "2");
    try (var writer =
        Files.newBufferedWriter(cacheRoot.resolve("index.properties"), StandardCharsets.UTF_8)) {
      index.store(writer, "File-layer example index");
    }
    return local;
  }

  /** Checks one observable result and increments the successful-check count. */
  private static void check(boolean condition, String name) {
    if (!condition) {
      throw new AssertionError(name);
    }
    checks++;
  }

  /** Checks that an operation reports the expected failure category. */
  private static void expect(Class<? extends RuntimeException> type, Runnable action) {
    try {
      action.run();
    } catch (RuntimeException failure) {
      check(type.isInstance(failure), "Expected " + type.getSimpleName() + ", got " + failure);
      return;
    }
    throw new AssertionError("Expected " + type.getSimpleName());
  }

  /**
   * Runs the exercise against sample resources and a caller-selected output directory.
   *
   * @param arguments packaged-resource root followed by an example output directory
   * @throws IOException if a resource or exercise fixture cannot be read or written
   */
  public static void main(String[] arguments) throws IOException {
    if (arguments.length != 2) {
      throw new IllegalArgumentException(
          "Usage: FileLayerWalkthrough <res-root> <output-directory>");
    }
    Path resRoot = Path.of(arguments[0]).toAbsolutePath().normalize();
    Path cacheRoot = Path.of(arguments[1]).toAbsolutePath().normalize();
    PageResourceManifest page = findPage(resRoot, 1001).orElseThrow();
    check(page.entries().size() == 2, "Two manifest entries");
    LocalResourceRef layout = page.uiLayout().localRef().orElseThrow();
    Path layoutFile = resolve(resRoot, cacheRoot, layout);
    check(layoutFile.equals(resRoot.resolve("2").resolve("7")), "Stable numeric mapping");
    check(Files.isRegularFile(layoutFile), "Layout file exists");
    check(
        Files.readString(layoutFile, StandardCharsets.UTF_8).contains("<section>"), "Layout bytes");
    LocalResourceRef textRef = page.entries().get(1).localRef().orElseThrow();
    String text =
        Files.readString(resolve(resRoot, cacheRoot, textRef), StandardCharsets.UTF_8).strip();
    check(text.equals("欢迎来到首页"), "UTF-8 text decoding");
    check(findPage(resRoot, 9999).isEmpty(), "Missing page returns empty");
    LocalResourceRef absent = new LocalResourceRef(List.of(0, 8), 99, ResourceKind.TEXT);
    Path absentFile = resolve(resRoot, cacheRoot, absent);
    check(!Files.exists(absentFile), "Path construction leaves the filesystem unchanged");
    byte[] content = "下载评论正文".getBytes(StandardCharsets.UTF_8);
    ResourceBlob blob =
        new ResourceBlob(
            new RemoteResourceRef("example-comment", "text/plain", content.length), content);
    LocalResourceRef saved = writeCacheExample(resRoot, cacheRoot, blob);
    check(saved.folderIndices().get(0) == 1, "Cache storage selector");
    check(
        Arrays.equals(Files.readAllBytes(resolve(resRoot, cacheRoot, saved)), content),
        "Binary write and read");
    Properties restored = new Properties();
    try (var reader =
        Files.newBufferedReader(cacheRoot.resolve("index.properties"), StandardCharsets.UTF_8)) {
      restored.load(reader);
    }
    check(
        "1,0|1|TEXT".equals(restored.getProperty("resource.example-comment")),
        "Index survives reload");
    Path scratch = cacheRoot.resolve("exercise-fixtures");
    Files.createDirectories(scratch.resolve("pages"));
    Files.writeString(
        scratch.resolve("pages/2001.properties"), "entry.count=invalid\n", StandardCharsets.UTF_8);
    expect(IllegalArgumentException.class, () -> findPage(scratch, 2001));
    Files.writeString(
        scratch.resolve("pages/2002.properties"), "entry.count=0\n", StandardCharsets.UTF_8);
    expect(IllegalArgumentException.class, () -> findPage(scratch, 2002));
    Files.createDirectories(scratch.resolve("pages/2003.properties"));
    expect(UncheckedIOException.class, () -> findPage(scratch, 2003));
    expect(
        IllegalArgumentException.class,
        () -> resolve(resRoot, cacheRoot, new LocalResourceRef(List.of(2), 1, ResourceKind.TEXT)));
    System.out.println("manifest.entries=" + page.entries().size());
    System.out.println("layout.file=2/7");
    System.out.println("text=" + text);
    System.out.println("missing.page=" + findPage(resRoot, 9999).isEmpty());
    System.out.println(
        "cached.text="
            + Files.readString(resolve(resRoot, cacheRoot, saved), StandardCharsets.UTF_8));
    System.out.println("checks=" + checks);
  }
}
