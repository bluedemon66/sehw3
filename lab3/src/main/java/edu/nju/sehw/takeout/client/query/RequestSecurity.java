package edu.nju.sehw.takeout.client.query;

import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.network.SecureRequest;

/**
 * Defines construction of a request envelope containing the message, credential, timestamp, and
 * digest.
 */
public interface RequestSecurity {
  /**
   * Encodes the message and attaches the session credential, request time, and body digest.
   *
   * @param message message envelope containing request identity, routing fields, operation code,
   *     and arguments
   * @return a request envelope whose request ID matches the supplied message
   */
  SecureRequest prepare(Message message);
}
