package edu.nju.sehw.takeout.client.query;

import edu.nju.sehw.takeout.client.core.Core;
import edu.nju.sehw.takeout.client.core.MessageParticipant;
import edu.nju.sehw.takeout.model.message.ErrorPayload;
import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.message.OperationReplyPayload;
import edu.nju.sehw.takeout.model.network.SecureResponse;
import edu.nju.sehw.takeout.model.resource.LocalResourceRef;
import edu.nju.sehw.takeout.model.resource.UploadResourcePayload;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Uploads a referenced local resource and routes the server's resource response back to the caller.
 */
public final class ResourceUploadTool extends MessageParticipant {
  /** Builder that attaches the client credential, timestamp, and digest to an outgoing message. */
  private final RequestSecurity security;

  /** Asynchronous HTTPS port used to submit operations or upload resources. */
  private final HttpsTransport transport;

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
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public ResourceUploadTool(
      int participantId, Core core, RequestSecurity security, HttpsTransport transport) {
    super(participantId, core);
    this.security = Objects.requireNonNull(security, "security");
    this.transport = Objects.requireNonNull(transport, "transport");
  }

  /**
   * Starts an attachment upload or processes the response for a pending upload request.
   *
   * @param message message envelope containing request identity, routing fields, operation code,
   *     and arguments
   */
  @Override
  public void parse(Message message) {
    if (!(message.payload() instanceof UploadResourcePayload input)) {
      reply(message, new ErrorPayload("INVALID_REQUEST", "Expected a local attachment"));
      return;
    }
    if (pending.putIfAbsent(message.requestId(), message) == null) {
      upload(message.requestId(), input.file());
    }
  }

  /**
   * Uploads the bytes resolved from the selected local resource reference.
   *
   * @param requestId identifier of the pending upload
   * @param file local resource reference identifying the bytes selected for upload
   */
  private void upload(UUID requestId, LocalResourceRef file) {
    Message message = pending.get(requestId);
    try {
      transport
          .upload(security.prepare(message), file)
          .whenComplete((response, failure) -> onUploaded(requestId, response, failure));
    } catch (RuntimeException ex) {
      onUploaded(requestId, null, ex);
    }
  }

  /**
   * Forwards the upload result to the owning client workflow using its reply route.
   *
   * @param requestId identifier of the pending upload
   * @param response remote response when transport succeeds
   * @param failure cause of a transport failure, when present
   */
  private void onUploaded(UUID requestId, SecureResponse response, Throwable failure) {
    Message message = pending.remove(requestId);
    if (message == null) {
      return;
    }
    if (failure != null || response == null || !response.requestId().equals(requestId)) {
      reply(message, new ErrorPayload("UPLOAD_FAILED", "Could not upload attachment"));
      return;
    }
    reply(message, new OperationReplyPayload(response.status(), response.payload()));
  }
}
