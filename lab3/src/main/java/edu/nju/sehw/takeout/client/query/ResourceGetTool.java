package edu.nju.sehw.takeout.client.query;

import edu.nju.sehw.takeout.client.core.Core;
import edu.nju.sehw.takeout.client.core.MessageParticipant;
import edu.nju.sehw.takeout.client.resource.LocalResourceStore;
import edu.nju.sehw.takeout.model.common.ResultStatus;
import edu.nju.sehw.takeout.model.message.ErrorPayload;
import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.message.MessagePayload;
import edu.nju.sehw.takeout.model.network.SecureResponse;
import edu.nju.sehw.takeout.model.resource.LocalResourceRef;
import edu.nju.sehw.takeout.model.resource.ResourceBlob;
import edu.nju.sehw.takeout.model.resource.ResourceBundle;
import edu.nju.sehw.takeout.model.resource.ResourceBundlePayload;
import edu.nju.sehw.takeout.model.resource.ResourcePayload;
import edu.nju.sehw.takeout.model.resource.ResourceRequestPayload;
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
  private final Map<UUID, Message> pending = new ConcurrentHashMap<>();

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
   */
  @Override
  public void parse(Message message) {
    if (!(message.payload() instanceof ResourceRequestPayload)) {
      reply(message, new ErrorPayload("INVALID_REQUEST", "Expected a remote resource request"));
      return;
    }
    if (pending.putIfAbsent(message.requestId(), message) == null) {
      download(message.requestId());
    }
  }

  /**
   * Requests the server bytes for the selected remote resource identifier.
   *
   * @param requestId identifier of the pending download
   */
  private void download(UUID requestId) {
    Message message = pending.get(requestId);
    try {
      transport
          .submit(security.prepare(message))
          .whenComplete((response, failure) -> finish(requestId, response, failure));
    } catch (RuntimeException ex) {
      finish(requestId, null, ex);
    }
  }

  private void finish(UUID requestId, SecureResponse response, Throwable failure) {
    Message message = pending.remove(requestId);
    if (message == null) {
      return;
    }
    MessagePayload result = new ErrorPayload("DOWNLOAD_FAILED", "Could not download resource");
    try {
      if (failure == null
          && response != null
          && response.requestId().equals(requestId)
          && response.status() == ResultStatus.SUCCESS
          && response.payload() instanceof ResourcePayload blob
          && message.payload() instanceof ResourceRequestPayload request
          && request
              .descriptor()
              .remoteResourceId()
              .orElseThrow()
              .equals(blob.resource().reference().resourceId())) {
        var ref = onResource(blob.resource());
        if (ref.kind() == request.descriptor().kind()) {
          result =
              new ResourceBundlePayload(
                  new ResourceBundle(requestId, Map.of(request.descriptor().elementId(), ref)));
        }
      }
    } catch (RuntimeException ex) {
      result = new ErrorPayload("CACHE_FAILED", "Could not cache downloaded resource");
    }
    reply(message, result);
  }

  /**
   * Stores received resource bytes locally and creates the reference returned to page assembly.
   *
   * @param reply complete resource metadata and downloaded bytes to store locally
   * @return the local reference assigned by the client resource store
   */
  private LocalResourceRef onResource(ResourceBlob reply) {
    return store.save(reply);
  }
}
