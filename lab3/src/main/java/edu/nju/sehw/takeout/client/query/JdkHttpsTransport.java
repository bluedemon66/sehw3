package edu.nju.sehw.takeout.client.query;

import edu.nju.sehw.takeout.client.resource.LocalResourceStore;
import edu.nju.sehw.takeout.model.message.BinaryMessageCodec;
import edu.nju.sehw.takeout.model.message.EnvelopeCodec;
import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.message.MessageCodec;
import edu.nju.sehw.takeout.model.network.MessageDigests;
import edu.nju.sehw.takeout.model.network.SecureRequest;
import edu.nju.sehw.takeout.model.network.SecureResponse;
import edu.nju.sehw.takeout.model.resource.AttachmentUploadPayload;
import edu.nju.sehw.takeout.model.resource.ImageMedia;
import edu.nju.sehw.takeout.model.resource.LocalResourceRef;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Adapts the HTTPS transport contract to a JDK HTTP client and server URI. */
public final class JdkHttpsTransport implements HttpsTransport {
  /** Base URI of the server's HTTPS endpoint. */
  private final URI serverUri;

  /** JDK HTTP client used to submit requests and transfer upload bytes. */
  private final HttpClient client;

  /** Shared encoder and decoder that defines deterministic message bytes. */
  private final MessageCodec codec;

  private final EnvelopeCodec envelopes;
  private final Optional<LocalResourceStore> localResources;

  /**
   * Creates this component with the dependencies documented below.
   *
   * @param serverUri base URI of the server's HTTPS endpoint
   * @param client JDK HTTP client used to submit requests and transfer upload bytes
   * @param codec shared encoder and decoder that defines deterministic message bytes
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public JdkHttpsTransport(URI serverUri, HttpClient client, MessageCodec codec) {
    this(serverUri, client, codec, null);
  }

  /** Creates an HTTPS transport with access to local images for uploads. */
  public JdkHttpsTransport(
      URI serverUri, HttpClient client, MessageCodec codec, LocalResourceStore localResources) {
    this.serverUri = Objects.requireNonNull(serverUri, "serverUri");
    if (!"https".equalsIgnoreCase(serverUri.getScheme()) || serverUri.getHost() == null) {
      throw new IllegalArgumentException("Server URI must use HTTPS");
    }
    this.localResources = Optional.ofNullable(localResources);
    this.envelopes = new EnvelopeCodec(codec);
    this.client = Objects.requireNonNull(client, "client");
    this.codec = Objects.requireNonNull(codec, "codec");
  }

  /**
   * Submits an authenticated operation request to the server asynchronously.
   *
   * @param request outgoing or received request envelope containing credential, timestamp, digest,
   *     and message
   * @return a stage completed with the matching server response, or exceptionally on transport
   *     failure
   */
  @Override
  public CompletionStage<SecureResponse> submit(SecureRequest request) {
    return send("/api/operations", request, new byte[0]);
  }

  /**
   * Uploads the referenced local bytes together with their authenticated request envelope.
   *
   * @param request outgoing or received request envelope containing credential, timestamp, digest,
   *     and message
   * @param file local resource reference identifying the bytes selected for upload
   * @return a stage completed with the upload response, or exceptionally on transport failure
   */
  @Override
  public CompletionStage<SecureResponse> upload(SecureRequest request, LocalResourceRef file) {
    try {
      if (localResources.isEmpty()) {
        return CompletableFuture.failedFuture(
            new IllegalStateException("Local resource store required"));
      }
      byte[] content;
      try (var input = Files.newInputStream(localResources.orElseThrow().resolve(file))) {
        content = input.readNBytes(5 * 1024 * 1024 + 1);
      }
      var metadata =
          new AttachmentUploadPayload(
              ImageMedia.detect(content),
              content.length,
              HexFormat.of().formatHex(MessageDigests.sha256(content)));
      Message original = request.message();
      var message =
          new Message(
              original.requestId(),
              original.correlationId(),
              original.senderId(),
              original.targetId(),
              original.operationCode(),
              metadata);
      var bound =
          new SecureRequest(
              request.requestId(),
              request.accessToken(),
              request.sentAt(),
              MessageDigests.sha256(codec.encode(message)),
              message);
      return send("/api/uploads", bound, content);
    } catch (IOException | RuntimeException ex) {
      return CompletableFuture.failedFuture(ex);
    }
  }

  private CompletionStage<SecureResponse> send(
      String path, SecureRequest request, byte[] attachment) {
    byte[] envelope = envelopes.encodeRequest(request);
    byte[] size = ByteBuffer.allocate(Integer.BYTES).putInt(envelope.length).array();
    var httpRequest =
        HttpRequest.newBuilder(serverUri.resolve(path))
            .timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/octet-stream")
            .POST(HttpRequest.BodyPublishers.ofByteArrays(List.of(size, envelope, attachment)))
            .build();
    return client
        .sendAsync(httpRequest, HttpResponse.BodyHandlers.ofInputStream())
        .thenApply(
            response -> {
              try (var body = response.body()) {
                if (response.statusCode() != 200) {
                  throw new IOException("HTTPS request returned " + response.statusCode());
                }
                SecureResponse reply =
                    envelopes.decodeResponse(body.readNBytes(BinaryMessageCodec.MAX_BYTES + 1));
                if (!reply.requestId().equals(request.requestId())) {
                  throw new IOException("Response request ID does not match");
                }
                return reply;
              } catch (IOException ex) {
                throw new UncheckedIOException(ex);
              }
            });
  }
}
