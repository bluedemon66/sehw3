package edu.nju.sehw.takeout.client.query;

import edu.nju.sehw.takeout.client.core.Core;
import edu.nju.sehw.takeout.client.core.MessageParticipant;
import edu.nju.sehw.takeout.client.resource.LocalResourceStore;
import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.resource.LocalResourceRef;
import edu.nju.sehw.takeout.model.resource.ResourceBlob;
import edu.nju.sehw.takeout.scaffold.NotImplementedYetException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Retrieves remote resource bytes and returns references to the locally stored result. */
public final class ResourceGetTool extends MessageParticipant {
  /** Builder that attaches the client credential, timestamp, and digest to an outgoing message. */
  private final RequestSecurity security;

  /** Asynchronous HTTPS port used to submit operations or upload resources. */
  private final HttpsTransport transport;

  /** Client resource store used to resolve references and cache downloaded bytes. */
  private final LocalResourceStore store;

  /** Remote request identifiers mapped to the original participant and root UI return route. */
  private final Map<UUID, ReplyRoute> pending = new ConcurrentHashMap<>();

  /**
   * Creates this participant with the dependencies documented below.
   *
   * @param participantId positive identifier assigned to this participant in its local Core
   * @param core local Core that owns this participant and routes its messages
   * @param security builder that attaches the client credential, timestamp, and digest to an
   *     outgoing message
   * @param transport asynchronous HTTPS port used to submit operations or upload resources
   * @param store client resource store used to resolve references and cache downloaded bytes
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public ResourceGetTool(
      int participantId,
      Core core,
      RequestSecurity security,
      HttpsTransport transport,
      LocalResourceStore store) {
    super(participantId, core);
    this.security = Objects.requireNonNull(security, "security");
    this.transport = Objects.requireNonNull(transport, "transport");
    this.store = Objects.requireNonNull(store, "store");
  }

  /**
   * Starts resource retrieval or processes the resource response for a pending request.
   *
   * @param message message envelope containing request identity, routing fields, operation code,
   *     and arguments
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  @Override
  public void parse(Message message) {
    throw new NotImplementedYetException("ResourceGetTool.parse");
  }

  /**
   * Requests the server bytes for the selected remote resource identifier.
   *
   * @param resourceId opaque identifier assigned to a resource by the server
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  private void download(String resourceId) {
    throw new NotImplementedYetException("ResourceGetTool.download");
  }

  /**
   * Stores received resource bytes locally and creates the reference returned to page assembly.
   *
   * @param reply complete resource metadata and downloaded bytes to store locally
   * @return the local reference assigned by the client resource store
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  private LocalResourceRef onResource(ResourceBlob reply) {
    throw new NotImplementedYetException("ResourceGetTool.onResource");
  }
}
