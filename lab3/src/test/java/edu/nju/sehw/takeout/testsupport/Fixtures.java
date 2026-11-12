package edu.nju.sehw.takeout.testsupport;

import edu.nju.sehw.takeout.client.core.Core;
import edu.nju.sehw.takeout.client.core.MessageParticipant;
import edu.nju.sehw.takeout.model.common.PermissionSet;
import edu.nju.sehw.takeout.model.common.UserId;
import edu.nju.sehw.takeout.model.message.EmptyPayload;
import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.message.MessagePayload;
import edu.nju.sehw.takeout.model.message.OperationCode;
import edu.nju.sehw.takeout.server.security.ServerRequestContext;
import edu.nju.sehw.takeout.server.task.VerifiedTask;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/** Test endpoints and named request contexts used by the contract tests. */
public final class Fixtures {
  private Fixtures() {}

  public static Message message(UUID requestId, int sender, int target) {
    return new Message(
        requestId, sender, target, OperationCode.OPEN_PAGE.code(), new EmptyPayload());
  }

  public static VerifiedTask task(UUID requestId) {
    ServerRequestContext context =
        new ServerRequestContext(
            new UserId(UUID.randomUUID()), new PermissionSet(Set.of()), requestId);
    return new VerifiedTask(context, message(requestId, 1, 2), response -> {}, Optional.empty());
  }

  /** In-memory endpoint that records routed messages for assertions. */
  public static class Probe extends MessageParticipant {
    private final List<Message> received = new CopyOnWriteArrayList<>();

    public Probe(int id, Core core) {
      super(id, core);
    }

    public Message snapshot(UUID requestId, int targetId, MessagePayload payload) {
      return prepareMessage(requestId, targetId, OperationCode.OPEN_PAGE.code(), payload);
    }

    @Override
    public void parse(Message message) {
      received.add(message);
    }

    public List<Message> received() {
      return List.copyOf(received);
    }
  }
}
