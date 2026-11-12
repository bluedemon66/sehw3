package edu.nju.sehw.takeout.client.resource;

import edu.nju.sehw.takeout.model.resource.LocalResourceRef;
import edu.nju.sehw.takeout.model.resource.ResourceBlob;
import edu.nju.sehw.takeout.model.resource.ResourceDescriptor;
import edu.nju.sehw.takeout.scaffold.NotImplementedYetException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Adapts packaged resources and downloaded cache files to the local resource-store contract.
 *
 * <p>Packaged UI assets are rooted at resRoot; downloaded resources are stored under cacheRoot.
 * Numeric references are interpreted by this adapter.
 */
public final class FileLocalResourceStore implements LocalResourceStore {
  /** Root directory containing packaged UI layouts and static client resources. */
  private final Path resRoot;

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
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  @Override
  public boolean contains(ResourceDescriptor descriptor) {
    throw new NotImplementedYetException("FileLocalResourceStore.contains");
  }

  /**
   * Resolves a numeric local resource reference using this store's root mapping.
   *
   * @param ref numeric local catalogue reference to resolve or bind
   * @return the local filesystem path selected by the resource reference
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  @Override
  public Path resolve(LocalResourceRef ref) {
    throw new NotImplementedYetException("FileLocalResourceStore.resolve");
  }

  /**
   * Stores downloaded resource bytes in the client cache and creates a local reference.
   *
   * @param resource complete resource metadata and downloaded bytes to store locally
   * @return the local catalogue reference assigned to the stored resource
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  @Override
  public LocalResourceRef save(ResourceBlob resource) {
    throw new NotImplementedYetException("FileLocalResourceStore.save");
  }
}
