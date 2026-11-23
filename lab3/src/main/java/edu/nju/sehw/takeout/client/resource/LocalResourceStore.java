package edu.nju.sehw.takeout.client.resource;

import edu.nju.sehw.takeout.model.resource.LocalResourceRef;
import edu.nju.sehw.takeout.model.resource.ResourceBlob;
import edu.nju.sehw.takeout.model.resource.ResourceDescriptor;
import java.nio.file.Path;
import java.util.Optional;

/** Defines presence checks, path resolution, and storage for client-side resources. */
public interface LocalResourceStore {
  /**
   * Checks whether the described resource is available in the local resource store.
   *
   * @param descriptor resource identity, kind, and available local or remote location hints
   * @return true when a usable local copy is available for the descriptor
   */
  boolean contains(ResourceDescriptor descriptor);

  /**
   * Finds the usable local reference for a descriptor.
   *
   * @param descriptor resource hints and kind
   * @return the local reference when found
   */
  default Optional<LocalResourceRef> find(ResourceDescriptor descriptor) {
    return contains(descriptor) ? descriptor.localRef() : Optional.empty();
  }

  /**
   * Resolves a numeric local resource reference using this store's root mapping.
   *
   * @param ref numeric local catalogue reference to resolve or bind
   * @return the local filesystem path selected by the resource reference
   */
  Path resolve(LocalResourceRef ref);

  /**
   * Stores downloaded resource bytes in the client cache and creates a local reference.
   *
   * @param resource complete resource metadata and downloaded bytes to store locally
   * @return the local catalogue reference assigned to the stored resource
   */
  LocalResourceRef save(ResourceBlob resource);
}
