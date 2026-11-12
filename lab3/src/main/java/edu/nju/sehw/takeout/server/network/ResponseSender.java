package edu.nju.sehw.takeout.server.network;

import edu.nju.sehw.takeout.model.network.ReplyChannel;
import edu.nju.sehw.takeout.model.network.SecureResponse;
import edu.nju.sehw.takeout.scaffold.NotImplementedYetException;
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
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  public void reply(ReplyChannel channel, SecureResponse response) throws IOException {
    throw new NotImplementedYetException("ResponseSender.reply");
  }
}
