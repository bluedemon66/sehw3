package edu.nju.sehw.takeout.client.core;

import edu.nju.sehw.takeout.model.message.Message;

/** Defines the send and receive operations required for participation in a local Core. */
public interface CoreEndpoint {
  /**
   * Submits the supplied snapshot through the participant's owning Core.
   *
   * @param senderId registered identifier of the participant sending the message
   * @param targetId registered identifier of the participant that should receive the message
   * @param core local Core that owns this participant and routes its messages
   * @param message message envelope containing request identity, routing fields, operation code,
   *     and arguments
   * @return local routing acceptance or the routing failure category
   */
  DispatchResult send(int senderId, int targetId, Core core, Message message);

  /**
   * Consumes a message delivered by Core and selects this participant's handling strategy.
   *
   * @param message message envelope containing request identity, routing fields, operation code,
   *     and arguments
   */
  void parse(Message message);
}
