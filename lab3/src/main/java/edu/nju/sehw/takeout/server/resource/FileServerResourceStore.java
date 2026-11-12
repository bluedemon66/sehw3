package edu.nju.sehw.takeout.server.resource;

import edu.nju.sehw.takeout.model.common.UserId;
import edu.nju.sehw.takeout.model.resource.RemoteResourceRef;
import edu.nju.sehw.takeout.model.resource.ResourceBlob;
import edu.nju.sehw.takeout.scaffold.NotImplementedYetException;
import edu.nju.sehw.takeout.server.persistence.TransactionContext;
import edu.nju.sehw.takeout.server.security.ServerRequestContext;
import edu.nju.sehw.takeout.server.task.VerifiedUpload;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Adapts server resource reads, staged uploads, and ownership checks to server-controlled storage.
 */
public final class FileServerResourceStore implements ServerResourceStore {
  /** Server-controlled root directory for persisted resource files. */
  private final Path root;

  /**
   * Creates this component with the dependencies documented below.
   *
   * @param root server-controlled root directory for persisted resource files
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public FileServerResourceStore(Path root) {
    this.root = Objects.requireNonNull(root, "root");
  }

  /**
   * Reads resource bytes after applying the current principal's resource visibility rules.
   *
   * @param resourceId opaque identifier assigned to a resource by the server
   * @param context authenticated principal, server-established permissions, and remote request
   *     identity
   * @return the resource descriptor and an isolated copy of its complete bytes
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  @Override
  public ResourceBlob read(String resourceId, ServerRequestContext context) {
    throw new NotImplementedYetException("FileServerResourceStore.read");
  }

  /**
   * Persists the verified staged upload and assigns its server resource identity.
   *
   * @param upload authenticated staged upload to persist as a server resource
   * @return the stored resource identifier, media type, and complete byte length
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  @Override
  public RemoteResourceRef write(VerifiedUpload upload) {
    throw new NotImplementedYetException("FileServerResourceStore.write");
  }

  /**
   * Checks ownership of every selected resource using the current transaction and actor identity.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param actor authenticated user whose ownership, purchase evidence, or idempotency record is
   *     being inspected
   * @param resourceIds server resource identifiers whose ownership must be checked
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  @Override
  public void checkOwned(TransactionContext transaction, UserId actor, List<String> resourceIds) {
    throw new NotImplementedYetException("FileServerResourceStore.checkOwned");
  }
}
