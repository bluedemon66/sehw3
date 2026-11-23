package edu.nju.sehw.takeout.server.network;

import edu.nju.sehw.takeout.model.common.Permission;
import edu.nju.sehw.takeout.model.common.ResultStatus;
import edu.nju.sehw.takeout.model.message.ErrorPayload;
import edu.nju.sehw.takeout.model.message.OperationCode;
import edu.nju.sehw.takeout.model.network.MessageDigests;
import edu.nju.sehw.takeout.model.network.ReplyChannel;
import edu.nju.sehw.takeout.model.network.SecureRequest;
import edu.nju.sehw.takeout.model.network.SecureResponse;
import edu.nju.sehw.takeout.model.resource.AttachmentUploadPayload;
import edu.nju.sehw.takeout.model.resource.ImageMedia;
import edu.nju.sehw.takeout.server.security.ServerRequestContext;
import edu.nju.sehw.takeout.server.security.ServerRequestVerifier;
import edu.nju.sehw.takeout.server.task.TaskPool;
import edu.nju.sehw.takeout.server.task.VerifiedTask;
import edu.nju.sehw.takeout.server.task.VerifiedUpload;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

/**
 * Authenticates incoming requests, stages uploads, and submits verified work to the bounded task
 * pool.
 */
public final class ServerReceiver {
  private static final System.Logger LOGGER = System.getLogger(ServerReceiver.class.getName());

  /** Server request validator that establishes the authenticated task context. */
  private final ServerRequestVerifier verifier;

  /** Bounded queue shared by the receiver and server workers. */
  private final TaskPool pool;

  /** Service that writes completed responses to their originating channels. */
  private final ResponseSender responseSender;

  /**
   * Creates this component with the dependencies documented below.
   *
   * @param verifier server request validator that establishes the authenticated task context
   * @param pool bounded queue shared by the receiver and server workers
   * @param responseSender service that writes completed responses to their originating channels
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public ServerReceiver(
      ServerRequestVerifier verifier, TaskPool pool, ResponseSender responseSender) {
    this.verifier = Objects.requireNonNull(verifier, "verifier");
    this.pool = Objects.requireNonNull(pool, "pool");
    this.responseSender = Objects.requireNonNull(responseSender, "responseSender");
  }

  /**
   * Verifies an incoming operation request and offers its task to the bounded worker queue.
   *
   * @param request outgoing or received request envelope containing credential, timestamp, digest,
   *     and message
   * @param channel reply connection associated with the request that produced this response
   */
  public void receive(SecureRequest request, ReplyChannel channel) {
    try {
      if (request.message().operationCode() == OperationCode.UPLOAD_ATTACHMENT.code()) {
        reject(channel, request, ResultStatus.INVALID_REQUEST, "Use the upload endpoint");
        return;
      }
      var context = verifier.verify(request);
      enqueue(request, new VerifiedTask(context, request.message(), channel, Optional.empty()));
    } catch (SecurityException ex) {
      reject(channel, request, ResultStatus.UNAUTHENTICATED, "Request authentication failed");
    }
  }

  /**
   * Verifies an upload request, stages its bytes, and queues work carrying the staged reference.
   *
   * @param request outgoing or received request envelope containing credential, timestamp, digest,
   *     and message
   * @param content stream containing the upload bytes received from the client connection
   * @param channel reply connection associated with the request that produced this response
   */
  public void receiveUpload(SecureRequest request, InputStream content, ReplyChannel channel) {
    try {
      var context = verifier.verify(request);
      if (!context.verifiedPermissions().contains(Permission.UPLOAD_RESOURCE)) {
        reject(channel, request, ResultStatus.FORBIDDEN, "Upload permission required");
        return;
      }
      var upload = stageUpload(request, content, context);
      enqueue(request, new VerifiedTask(context, request.message(), channel, Optional.of(upload)));
    } catch (SecurityException ex) {
      reject(channel, request, ResultStatus.UNAUTHENTICATED, "Request authentication failed");
    } catch (IllegalArgumentException ex) {
      reject(channel, request, ResultStatus.INVALID_REQUEST, ex.getMessage());
    } catch (IOException ex) {
      reject(channel, request, ResultStatus.FAILED, "Could not stage upload");
    }
  }

  /**
   * Copies incoming upload bytes to server-controlled staging storage with verified metadata.
   *
   * @param request outgoing or received request envelope containing credential, timestamp, digest,
   *     and message
   * @param content stream containing the upload bytes received from the client connection
   * @param context verified identity and permissions
   * @return the authenticated staging-file description to attach to the task
   * @throws IOException if the request body or staging file cannot be read or written
   */
  private VerifiedUpload stageUpload(
      SecureRequest request, InputStream content, ServerRequestContext context) throws IOException {
    if (request.message().operationCode() != OperationCode.UPLOAD_ATTACHMENT.code()
        || !(request.message().payload() instanceof AttachmentUploadPayload metadata)) {
      throw new IllegalArgumentException("Upload metadata is required");
    }
    byte[] bytes = content.readNBytes(Math.toIntExact(metadata.contentLength()) + 1);
    if (bytes.length != metadata.contentLength()
        || !HexFormat.of().formatHex(MessageDigests.sha256(bytes)).equals(metadata.contentDigest())
        || !ImageMedia.detect(bytes).equals(metadata.mediaType())) {
      throw new IllegalArgumentException("Upload content differs from its declared metadata");
    }
    Path file = Files.createTempFile("sehw3-attachment-", ".upload");
    try {
      Files.write(file, bytes);
      return new VerifiedUpload(context, file, metadata.mediaType(), bytes.length, Instant.now());
    } catch (IOException | RuntimeException ex) {
      try {
        Files.deleteIfExists(file);
      } catch (IOException cleanup) {
        ex.addSuppressed(cleanup);
      }
      throw ex;
    }
  }

  private void enqueue(SecureRequest request, VerifiedTask task) {
    if (!pool.offer(task)) {
      task.upload().ifPresent(VerifiedUpload::discard);
      reject(task.replyChannel(), request, ResultStatus.BUSY, "Server queue is full");
    }
  }

  private void reject(
      ReplyChannel channel, SecureRequest request, ResultStatus status, String detail) {
    try {
      responseSender.reply(
          channel,
          new SecureResponse(request.requestId(), status, new ErrorPayload(status.name(), detail)));
    } catch (IOException ex) {
      LOGGER.log(System.Logger.Level.WARNING, "Could not send rejection", ex);
    }
  }
}
