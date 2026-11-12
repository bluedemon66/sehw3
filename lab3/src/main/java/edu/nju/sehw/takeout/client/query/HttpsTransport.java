package edu.nju.sehw.takeout.client.query;

import edu.nju.sehw.takeout.model.network.SecureRequest;
import edu.nju.sehw.takeout.model.network.SecureResponse;
import edu.nju.sehw.takeout.model.resource.LocalResourceRef;
import java.util.concurrent.CompletionStage;

/**
 * Defines asynchronous submission and attachment upload over the application's HTTPS connection.
 *
 * <p>A completed stage carries a SecureResponse, including its business status. Connection,
 * encoding, and transfer failures complete the stage exceptionally.
 */
public interface HttpsTransport {
  /**
   * Submits an authenticated operation request to the server asynchronously.
   *
   * @param request outgoing or received request envelope containing credential, timestamp, digest,
   *     and message
   * @return a stage completed with the matching server response, or exceptionally on transport
   *     failure
   */
  CompletionStage<SecureResponse> submit(SecureRequest request);

  /**
   * Uploads the referenced local bytes together with their authenticated request envelope.
   *
   * @param request outgoing or received request envelope containing credential, timestamp, digest,
   *     and message
   * @param file local resource reference identifying the bytes selected for upload
   * @return a stage completed with the upload response, or exceptionally on transport failure
   */
  CompletionStage<SecureResponse> upload(SecureRequest request, LocalResourceRef file);
}
