package edu.nju.sehw.takeout.model.network;

import java.io.IOException;

/** Defines delivery of a server response through the connection associated with a request. */
@FunctionalInterface
public interface ReplyChannel {
  /**
   * Writes the supplied response to the connection associated with its originating request.
   *
   * @param response remote request identifier, result status, and typed result payload
   * @throws IOException if the reply channel fails while writing the response
   */
  void send(SecureResponse response) throws IOException;
}
