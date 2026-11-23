package edu.nju.sehw.takeout.server.resource;

import edu.nju.sehw.takeout.model.resource.RemoteResourceRef;
import edu.nju.sehw.takeout.model.resource.ResourceBlob;
import edu.nju.sehw.takeout.server.persistence.TransactionContext;
import edu.nju.sehw.takeout.server.security.ServerRequestContext;
import edu.nju.sehw.takeout.server.task.VerifiedTask;
import java.util.Objects;

/** Registers a verified staged upload and returns its server resource reference. */
public final class ServerUploadHandler {
  /** Server-controlled resource storage and ownership-checking port. */
  private final ServerResourceStore resources;

  /**
   * Creates this component with the dependencies documented below.
   *
   * @param resources server-controlled resource storage and ownership-checking port
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public ServerUploadHandler(ServerResourceStore resources) {
    this.resources = Objects.requireNonNull(resources, "resources");
  }

  /**
   * Registers the task's verified upload as a stored resource for the authenticated operation.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param task authenticated request, reply channel, and optional staged upload to execute
   * @return the server reference assigned to the stored resource
   */
  public RemoteResourceRef handle(TransactionContext transaction, VerifiedTask task) {
    return resources.write(
        transaction,
        task.upload()
            .orElseThrow(
                () -> new IllegalArgumentException("Verified upload content is required")));
  }

  /** Reads a resource for the authenticated caller. */
  public ResourceBlob read(
      TransactionContext transaction, String id, ServerRequestContext context) {
    return resources.read(transaction, id, context);
  }
}
