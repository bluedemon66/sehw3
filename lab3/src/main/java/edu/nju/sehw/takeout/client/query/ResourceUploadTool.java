package edu.nju.sehw.takeout.client.query;

import edu.nju.sehw.takeout.client.core.Core;
import edu.nju.sehw.takeout.client.core.MessageParticipant;
import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.network.SecureResponse;
import edu.nju.sehw.takeout.model.resource.LocalResourceRef;
import edu.nju.sehw.takeout.scaffold.NotImplementedYetException;
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
  private final Map<UUID, ReplyRoute> pending = new ConcurrentHashMap<>();

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
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  @Override
  public void parse(Message message) {
    throw new NotImplementedYetException("ResourceUploadTool.parse");
  }

  /**
   * Uploads the bytes resolved from the selected local resource reference.
   *
   * @param file local resource reference identifying the bytes selected for upload
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  private void upload(LocalResourceRef file) {
    throw new NotImplementedYetException("ResourceUploadTool.upload");
  }

  /**
   * Forwards the upload result to the owning client workflow using its reply route.
   *
   * @param reply remote request identifier, result status, and typed result payload
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  private void onUploaded(SecureResponse reply) {
    throw new NotImplementedYetException("ResourceUploadTool.onUploaded");
  }
}
