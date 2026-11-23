package edu.nju.sehw.takeout.server.network;

import edu.nju.sehw.takeout.model.network.ReplyChannel;
import edu.nju.sehw.takeout.model.network.SecureResponse;
import java.io.IOException;

/** Delivers a completed server response through its originating request channel. */
public final class ResponseSender {

  /** Creates this component with the dependencies documented below. */
  public ResponseSender() {}

  /**
   * Writes the completed response to the request's originating reply channel.
   *
   * @param channel reply connection associated with the request that produced this response
   * @param response remote request identifier, result status, and typed result payload
   * @throws IOException if the reply channel fails while writing the response
   */
  public void reply(ReplyChannel channel, SecureResponse response) throws IOException {
    channel.send(response);
  }
}
