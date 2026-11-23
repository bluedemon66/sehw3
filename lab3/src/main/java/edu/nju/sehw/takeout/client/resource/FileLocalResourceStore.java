package edu.nju.sehw.takeout.client.resource;

import edu.nju.sehw.takeout.model.resource.LocalResourceRef;
import edu.nju.sehw.takeout.model.resource.ResourceBlob;
import edu.nju.sehw.takeout.model.resource.ResourceDescriptor;
import edu.nju.sehw.takeout.model.resource.ResourceKind;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * Adapts packaged resources and downloaded cache files to the local resource-store contract.
 *
 * <p>Packaged UI assets are rooted at resRoot; downloaded resources are stored under cacheRoot.
 * Numeric references are interpreted by this adapter.
 */
public final class FileLocalResourceStore implements LocalResourceStore {
  /** Root directory containing packaged UI layouts and static client resources. */
  private final Path resRoot;

  private static final Object CACHE_WRITES = new Object();

  /** Root directory used for downloaded client resources. */
  private final Path cacheRoot;

  /**
   * Creates this component with the dependencies documented below.
   *
   * @param resRoot root directory containing packaged UI layouts and static client resources
   * @param cacheRoot root directory used for downloaded client resources
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public FileLocalResourceStore(Path resRoot, Path cacheRoot) {
    this.resRoot = Objects.requireNonNull(resRoot, "resRoot");
    this.cacheRoot = Objects.requireNonNull(cacheRoot, "cacheRoot");
  }

  /**
   * Checks whether the described resource is available in the local resource store.
   *
   * @param descriptor resource identity, kind, and available local or remote location hints
   * @return true when a usable local copy is available for the descriptor
   */
  @Override
  public boolean contains(ResourceDescriptor descriptor) {
    return find(descriptor).isPresent();
  }

  @Override
  public synchronized Optional<LocalResourceRef> find(ResourceDescriptor descriptor) {
    Optional<LocalResourceRef> local =
        descriptor.localRef().filter(ref -> Files.isRegularFile(resolve(ref)));
    if (local.isPresent() || descriptor.remoteResourceId().isEmpty()) {
      return local;
    }
    try {
      String key = "resource." + descriptor.remoteResourceId().orElseThrow();
      return Optional.ofNullable(loadIndex().getProperty(key))
          .map(this::decode)
          .filter(ref -> ref.kind() == descriptor.kind())
          .filter(ref -> Files.isRegularFile(resolve(ref)));
    } catch (IOException ex) {
      throw new UncheckedIOException("Cannot read cache index at " + cacheRoot, ex);
    }
  }

  /**
   * Resolves a numeric local resource reference using this store's root mapping.
   *
   * @param ref numeric local catalogue reference to resolve or bind
   * @return the local filesystem path selected by the resource reference
   */
  @Override
  public Path resolve(LocalResourceRef ref) {
    List<Integer> folders = ref.folderIndices();
    if (folders.isEmpty()) {
      throw new IllegalArgumentException("Missing resource root index");
    }
    Path target =
        switch (folders.get(0)) {
          case 0 -> resRoot;
          case 1 -> cacheRoot;
          default -> throw new IllegalArgumentException("Unknown resource root " + folders.get(0));
        };
    for (int folder : folders.subList(1, folders.size())) {
      target = target.resolve(Integer.toString(folder));
    }
    return target.resolve(Integer.toString(ref.entryIndex()));
  }

  /**
   * Stores downloaded resource bytes in the client cache and creates a local reference.
   *
   * @param resource complete resource metadata and downloaded bytes to store locally
   * @return the local catalogue reference assigned to the stored resource
   */
  @Override
  public LocalResourceRef save(ResourceBlob resource) {
    synchronized (CACHE_WRITES) {
      try {
        Files.createDirectories(cacheRoot);
        try (var channel =
                FileChannel.open(
                    cacheRoot.resolve("index.lock"),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE);
            var lock = channel.lock()) {
          if (!lock.isValid()) {
            throw new IOException("Cannot lock resource cache");
          }
          return saveLocked(resource);
        }
      } catch (IOException ex) {
        throw new UncheckedIOException("Cannot save resource in " + cacheRoot, ex);
      }
    }
  }

  private LocalResourceRef saveLocked(ResourceBlob resource) throws IOException {
    Properties index = loadIndex();
    String key = "resource." + resource.reference().resourceId();
    String previous = index.getProperty(key);
    int next = Integer.parseInt(index.getProperty("next", "1"));
    int number = previous == null ? next : decode(previous).entryIndex();
    String media = resource.reference().mediaType();
    ResourceKind kind =
        media.startsWith("image/")
            ? ResourceKind.IMAGE
            : media.startsWith("text/") ? ResourceKind.TEXT : ResourceKind.BINARY;
    LocalResourceRef ref = new LocalResourceRef(List.of(1, 0), number, kind);
    Path target = resolve(ref);
    Files.createDirectories(target.getParent());
    Path temporary = Files.createTempFile(target.getParent(), "download-", ".tmp");
    try {
      Files.write(temporary, resource.content());
      Files.move(
          temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } finally {
      Files.deleteIfExists(temporary);
    }

    index.setProperty(key, number + "|" + kind.name());
    index.setProperty(
        "next", Integer.toString(previous == null ? Math.incrementExact(next) : next));
    Path indexFile = Files.createTempFile(cacheRoot, "index-", ".tmp");
    try {
      try (var writer = Files.newBufferedWriter(indexFile)) {
        index.store(writer, "Local resource cache");
      }
      Files.move(
          indexFile,
          cacheRoot.resolve("index.properties"),
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.ATOMIC_MOVE);
    } finally {
      Files.deleteIfExists(indexFile);
    }
    return ref;
  }

  private Properties loadIndex() throws IOException {
    Properties index = new Properties();
    try (var reader = Files.newBufferedReader(cacheRoot.resolve("index.properties"))) {
      index.load(reader);
    } catch (NoSuchFileException ex) {
      return index;
    }
    return index;
  }

  private LocalResourceRef decode(String entry) {
    String[] parts = entry.split("[|]", -1);
    if (parts.length != 2) {
      throw new IllegalArgumentException("Invalid resource cache entry");
    }
    return new LocalResourceRef(
        List.of(1, 0), Integer.parseInt(parts[0]), ResourceKind.valueOf(parts[1]));
  }
}
