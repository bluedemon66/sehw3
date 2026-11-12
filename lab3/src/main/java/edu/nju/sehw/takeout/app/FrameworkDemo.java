package edu.nju.sehw.takeout.app;

import edu.nju.sehw.takeout.client.core.Core;
import edu.nju.sehw.takeout.client.core.DispatchResult;
import edu.nju.sehw.takeout.client.core.MessageParticipant;
import edu.nju.sehw.takeout.client.core.RouteRule;
import edu.nju.sehw.takeout.model.message.EmptyPayload;
import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.message.OperationCode;
import java.util.Set;
import java.util.UUID;

/** Runs a local Core registration and message-delivery demonstration. */
public final class FrameworkDemo {
  /** Initializes the framework demonstration utility. */
  private FrameworkDemo() {}

  /**
   * Runs the local Core registration, authorization, and request-delivery smoke demonstration.
   *
   * @param arguments command-line arguments supplied to the framework demonstration
   */
  public static void main(String[] arguments) {
    Core core = new Core();
    DemoEndpoint sender = new DemoEndpoint(1, core);
    DemoEndpoint receiver = new DemoEndpoint(2, core);
    core.register(1, sender);
    core.register(2, receiver);
    core.allow(new RouteRule(1, 2, Set.of(OperationCode.OPEN_PAGE.code()), false));
    UUID requestId = UUID.randomUUID();
    Message message = sender.snapshot(requestId);
    DispatchResult result = sender.send(1, 2, core, message);
    if (!result.accepted() || !requestId.equals(receiver.receivedRequest)) {
      throw new IllegalStateException("消息路由检查失败");
    }
    System.out.println("Framework smoke check passed: Core -> parse, requestId=" + requestId);
  }

  /** Receives demonstration messages and records the last delivered request identifier. */
  private static final class DemoEndpoint extends MessageParticipant {
    /** Identifier of the most recently delivered demonstration message. */
    private UUID receivedRequest;

    /**
     * Creates this component with the dependencies documented below.
     *
     * @param id positive identifier assigned to this participant in its local Core
     * @param core local Core that owns this participant and routes its messages
     */
    private DemoEndpoint(int id, Core core) {
      super(id, core);
    }

    /**
     * Creates the page-operation message addressed to the demonstration receiver.
     *
     * @param requestId identifier of this individual request; retries reuse the same identifier
     * @return a new message correlated to the supplied demonstration request
     */
    private Message snapshot(UUID requestId) {
      return prepareMessage(requestId, 2, OperationCode.OPEN_PAGE.code(), new EmptyPayload());
    }

    /**
     * Records the request identifier of the message delivered by Core.
     *
     * @param message message envelope containing request identity, routing fields, operation code,
     *     and arguments
     */
    @Override
    public void parse(Message message) {
      receivedRequest = message.requestId();
    }
  }
}
