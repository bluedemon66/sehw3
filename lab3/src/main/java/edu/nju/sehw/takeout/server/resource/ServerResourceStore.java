package edu.nju.sehw.takeout.server.resource;

import edu.nju.sehw.takeout.model.common.UserId;
import edu.nju.sehw.takeout.model.resource.RemoteResourceRef;
import edu.nju.sehw.takeout.model.resource.ResourceBlob;
import edu.nju.sehw.takeout.server.persistence.TransactionContext;
import edu.nju.sehw.takeout.server.security.ServerRequestContext;
import edu.nju.sehw.takeout.server.task.VerifiedUpload;
import java.util.List;

/**
 * Defines authenticated resource reads, staged-upload persistence, and transactional ownership
 * checks.
 */
public interface ServerResourceStore {
  /**
   * Reads resource bytes after applying the current principal's resource visibility rules.
   *
   * @param resourceId opaque identifier assigned to a resource by the server
   * @param context authenticated principal, server-established permissions, and remote request
   *     identity
   * @return the resource descriptor and an isolated copy of its complete bytes
   */
  ResourceBlob read(String resourceId, ServerRequestContext context);

  /**
   * Persists the verified staged upload and assigns its server resource identity.
   *
   * @param upload authenticated staged upload to persist as a server resource
   * @return the stored resource identifier, media type, and complete byte length
   */
  RemoteResourceRef write(VerifiedUpload upload);

  /**
   * Checks ownership of every selected resource using the current transaction and actor identity.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param actor authenticated user whose ownership, purchase evidence, or idempotency record is
   *     being inspected
   * @param resourceIds server resource identifiers whose ownership must be checked
   */
  void checkOwned(TransactionContext transaction, UserId actor, List<String> resourceIds);
}
