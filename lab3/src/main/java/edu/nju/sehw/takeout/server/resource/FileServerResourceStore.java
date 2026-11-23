package edu.nju.sehw.takeout.server.resource;

import edu.nju.sehw.takeout.model.common.Permission;
import edu.nju.sehw.takeout.model.common.ResultStatus;
import edu.nju.sehw.takeout.model.common.UserId;
import edu.nju.sehw.takeout.model.resource.RemoteResourceRef;
import edu.nju.sehw.takeout.model.resource.ResourceBlob;
import edu.nju.sehw.takeout.server.operation.OperationFailure;
import edu.nju.sehw.takeout.server.persistence.Jdbc;
import edu.nju.sehw.takeout.server.persistence.TransactionContext;
import edu.nju.sehw.takeout.server.persistence.TransactionRunner;
import edu.nju.sehw.takeout.server.security.ServerRequestContext;
import edu.nju.sehw.takeout.server.task.VerifiedUpload;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapts server resource reads, staged uploads, and ownership checks to server-controlled storage.
 */
public final class FileServerResourceStore implements ServerResourceStore {
  /** Server-controlled root directory for persisted resource files. */
  private final Path root;

  private final Optional<TransactionRunner> transactions;

  /**
   * Creates this component with the dependencies documented below.
   *
   * @param root server-controlled root directory for persisted resource files
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public FileServerResourceStore(Path root) {
    this(root, null);
  }

  /** Creates a store that can also run standalone operations. */
  public FileServerResourceStore(Path root, TransactionRunner transactions) {
    this.root = Objects.requireNonNull(root, "root");
    this.transactions = Optional.ofNullable(transactions);
  }

  /**
   * Reads resource bytes after applying the current principal's resource visibility rules.
   *
   * @param resourceId opaque identifier assigned to a resource by the server
   * @param context authenticated principal, server-established permissions, and remote request
   *     identity
   * @return the resource descriptor and an isolated copy of its complete bytes
   */
  @Override
  public ResourceBlob read(String resourceId, ServerRequestContext context) {
    if (!context.verifiedPermissions().contains(Permission.READ_RESOURCE)) {
      throw new OperationFailure(ResultStatus.FORBIDDEN, "Resource permission required");
    }
    return transactions
        .orElseThrow(() -> new IllegalStateException("Resource database is required"))
        .inTransaction(transaction -> read(transaction, resourceId, context));
  }

  @Override
  public ResourceBlob read(
      TransactionContext transaction, String resourceId, ServerRequestContext context) {
    if (!context.verifiedPermissions().contains(Permission.READ_RESOURCE)) {
      throw new OperationFailure(ResultStatus.FORBIDDEN, "Resource permission required");
    }
    UUID id = UUID.fromString(resourceId);
    var reference =
        Jdbc.first(
                transaction,
                """
                SELECT media_type, content_length FROM resources r WHERE r.resource_id=?
                AND (r.owner_id=? OR EXISTS (SELECT 1 FROM comment_images i JOIN comments c
                  ON c.comment_id=i.comment_id
                  WHERE i.resource_id=r.resource_id AND c.status='PUBLISHED'))
                """,
                row -> new RemoteResourceRef(id.toString(), row.getString(1), row.getLong(2)),
                id,
                context.principalId().value())
            .orElseThrow(() -> new OperationFailure(ResultStatus.NOT_FOUND, "Resource not found"));
    try {
      return new ResourceBlob(reference, Files.readAllBytes(root.resolve(id.toString())));
    } catch (IOException ex) {
      throw new UncheckedIOException("Cannot read resource", ex);
    }
  }

  /**
   * Persists the verified staged upload and assigns its server resource identity.
   *
   * @param upload authenticated staged upload to persist as a server resource
   * @return the stored resource identifier, media type, and complete byte length
   */
  @Override
  public RemoteResourceRef write(VerifiedUpload upload) {
    return transactions
        .orElseThrow(() -> new IllegalStateException("Resource database is required"))
        .inTransaction(transaction -> write(transaction, upload));
  }

  @Override
  public RemoteResourceRef write(TransactionContext transaction, VerifiedUpload upload) {
    if (!upload.context().verifiedPermissions().contains(Permission.UPLOAD_RESOURCE)) {
      throw new OperationFailure(ResultStatus.FORBIDDEN, "Upload permission required");
    }
    UUID id = UUID.randomUUID();
    Path target = root.resolve(id.toString());
    transaction.onRollback(
        () -> {
          try {
            Files.deleteIfExists(target);
          } catch (IOException ex) {
            throw new UncheckedIOException(ex);
          }
        });
    try {
      Files.createDirectories(root);
      Files.copy(upload.stagingFile(), target);
      if (Files.size(target) != upload.contentLength()) {
        throw new IllegalArgumentException("Staged file length changed");
      }
    } catch (IOException ex) {
      throw new UncheckedIOException("Cannot store upload", ex);
    }
    Jdbc.update(
        transaction,
        """
        INSERT INTO resources(resource_id, owner_id, media_type, content_length) VALUES (?, ?, ?, ?)
        """,
        id,
        upload.context().principalId().value(),
        upload.mediaType(),
        upload.contentLength());
    return new RemoteResourceRef(id.toString(), upload.mediaType(), upload.contentLength());
  }

  /**
   * Checks ownership of every selected resource using the current transaction and actor identity.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param actor authenticated user whose ownership, purchase evidence, or idempotency record is
   *     being inspected
   * @param resourceIds server resource identifiers whose ownership must be checked
   */
  @Override
  public void checkOwned(TransactionContext transaction, UserId actor, List<String> resourceIds) {
    for (String resourceId : resourceIds) {
      if (Jdbc.first(
              transaction,
              "SELECT 1 FROM resources WHERE resource_id=? AND owner_id=?",
              row -> row.getInt(1),
              UUID.fromString(resourceId),
              actor.value())
          .isEmpty()) {
        throw new OperationFailure(
            ResultStatus.FORBIDDEN, "Attachment is outside the author's ownership");
      }
    }
  }
}
