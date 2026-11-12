package edu.nju.sehw.takeout.client.query;

import edu.nju.sehw.takeout.model.message.MessageCodec;
import edu.nju.sehw.takeout.model.network.SecureRequest;
import edu.nju.sehw.takeout.model.network.SecureResponse;
import edu.nju.sehw.takeout.model.resource.LocalResourceRef;
import edu.nju.sehw.takeout.scaffold.NotImplementedYetException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/** Adapts the HTTPS transport contract to a JDK HTTP client and server URI. */
public final class JdkHttpsTransport implements HttpsTransport {
  /** Base URI of the server's HTTPS endpoint. */
  private final URI serverUri;

  /** JDK HTTP client used to submit requests and transfer upload bytes. */
  private final HttpClient client;

  /** Shared encoder and decoder that defines deterministic message bytes. */
  private final MessageCodec codec;

  /**
   * Creates this component with the dependencies documented below.
   *
   * @param serverUri base URI of the server's HTTPS endpoint
   * @param client JDK HTTP client used to submit requests and transfer upload bytes
   * @param codec shared encoder and decoder that defines deterministic message bytes
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public JdkHttpsTransport(URI serverUri, HttpClient client, MessageCodec codec) {
    this.serverUri = Objects.requireNonNull(serverUri, "serverUri");
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
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  @Override
  public CompletionStage<SecureResponse> submit(SecureRequest request) {
    throw new NotImplementedYetException("JdkHttpsTransport.submit");
  }

  /**
   * Uploads the referenced local bytes together with their authenticated request envelope.
   *
   * @param request outgoing or received request envelope containing credential, timestamp, digest,
   *     and message
   * @param file local resource reference identifying the bytes selected for upload
   * @return a stage completed with the upload response, or exceptionally on transport failure
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  @Override
  public CompletionStage<SecureResponse> upload(SecureRequest request, LocalResourceRef file) {
    throw new NotImplementedYetException("JdkHttpsTransport.upload");
  }
}
