package edu.nju.sehw.takeout.client.query;

import edu.nju.sehw.takeout.client.core.Core;
import edu.nju.sehw.takeout.client.core.MessageParticipant;
import edu.nju.sehw.takeout.model.common.ResultStatus;
import edu.nju.sehw.takeout.model.message.ErrorPayload;
import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.message.OperationReplyPayload;
import edu.nju.sehw.takeout.model.network.SecureResponse;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sends numbered data operations to the server and correlates responses with client return routes.
 *
 * <p>Each remote request is associated with a ReplyRoute so that responses can be returned to the
 * originating participant and UI operation.
 */
public final class DatabaseGateway extends MessageParticipant {
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
  public DatabaseGateway(
      int participantId, Core core, RequestSecurity security, HttpsTransport transport) {
    super(participantId, core);
    this.security = Objects.requireNonNull(security, "security");
    this.transport = Objects.requireNonNull(transport, "transport");
  }

  /**
   * Prepares a numbered data request for remote execution or handles its correlated response.
   *
   * @param message message envelope containing request identity, routing fields, operation code,
   *     and arguments
   */
  @Override
  public void parse(Message message) {
    if (pending.putIfAbsent(message.requestId(), message) == null) {
      executeOperation(message.requestId());
    }
  }

  /**
   * Submits the selected CRUD or scalar-query operation through the configured transport.
   *
   * @param requestId identifier of the pending request
   */
  private void executeOperation(UUID requestId) {
    Message message = pending.get(requestId);
    try {
      transport
          .submit(security.prepare(message))
          .whenComplete((response, failure) -> onReply(requestId, response, failure));
    } catch (RuntimeException ex) {
      onReply(requestId, null, ex);
    }
  }

  /**
   * Uses the response request ID to recover the client return route and forward the result.
   *
   * @param requestId identifier of the pending request
   * @param response remote response when transport succeeds
   * @param failure cause of a transport failure, when present
   */
  private void onReply(UUID requestId, SecureResponse response, Throwable failure) {
    Message request = pending.remove(requestId);
    if (request == null) {
      return;
    }
    if (failure != null || response == null || !requestId.equals(response.requestId())) {
      reply(
          request,
          new OperationReplyPayload(
              ResultStatus.FAILED,
              new ErrorPayload("TRANSPORT", "Could not complete the server request")));
      return;
    }
    reply(request, new OperationReplyPayload(response.status(), response.payload()));
  }
}
