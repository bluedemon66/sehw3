package edu.nju.sehw.takeout.client.logic;

import edu.nju.sehw.takeout.client.core.ClientParticipants;
import edu.nju.sehw.takeout.client.core.Core;
import edu.nju.sehw.takeout.client.core.MessageParticipant;
import edu.nju.sehw.takeout.client.security.ClientPermissionChecker;
import edu.nju.sehw.takeout.model.common.ResultStatus;
import edu.nju.sehw.takeout.model.common.TargetDescriptor;
import edu.nju.sehw.takeout.model.message.ErrorPayload;
import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.message.MessagePayload;
import edu.nju.sehw.takeout.model.message.OperationBatchPayload;
import edu.nju.sehw.takeout.model.message.OperationReplyPayload;
import edu.nju.sehw.takeout.model.message.OperationRequest;
import edu.nju.sehw.takeout.model.message.OperationResultsPayload;
import edu.nju.sehw.takeout.model.message.TargetPayload;
import edu.nju.sehw.takeout.model.order.OrderCommandPayload;
import edu.nju.sehw.takeout.model.order.PlaceOrderPayload;
import edu.nju.sehw.takeout.model.order.ResolveCasePayload;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Validates local order and scalar requests and routes their results through the Core. */
public final class CrudOperationService extends MessageParticipant {
  private final ClientPermissionChecker checker;
  private final Map<UUID, Batch> pending = new ConcurrentHashMap<>();

  /** Creates the service with its local permission checker. */
  public CrudOperationService(int participantId, Core core, ClientPermissionChecker checker) {
    super(participantId, core);
    this.checker = Objects.requireNonNull(checker);
  }

  @Override
  public void parse(Message message) {
    if (message.senderId() == ClientParticipants.GATEWAY) {
      completeOperation(message);
      return;
    }
    var operations = planOperations(message);
    if (operations.isEmpty()
        || new HashSet<>(operations.stream().map(OperationRequest::requestId).toList()).size()
            != operations.size()) {
      finish(message, new ErrorPayload("INVALID_REQUEST", "Expected distinct operation IDs"));
      return;
    }
    try {
      for (OperationRequest operation : operations) {
        var check =
            checker.check(
                operation.operationCode(),
                new TargetDescriptor("domain", targetId(operation.payload())));
        if (!check.allowed()) {
          finish(message, new ErrorPayload(check.code(), check.description()));
          return;
        }
      }
    } catch (IllegalArgumentException ex) {
      finish(message, new ErrorPayload("INVALID_REQUEST", ex.getMessage()));
      return;
    }
    submit(new Batch(message, operations.iterator(), new ArrayList<>()));
  }

  private static List<OperationRequest> planOperations(Message message) {
    var payload = message.payload();
    if (payload instanceof OperationBatchPayload batch) {
      return batch.requests();
    }
    return List.of(new OperationRequest(message.requestId(), message.operationCode(), payload));
  }

  private void submit(Batch batch) {
    var operation = batch.operations().next();
    if (pending.putIfAbsent(operation.requestId(), batch) != null) {
      finish(batch.request(), new ErrorPayload("IN_PROGRESS", "Request is already pending"));
      return;
    }
    var sent =
        dispatch(
            prepareMessage(
                operation.requestId(),
                batch.request().correlationId(),
                ClientParticipants.GATEWAY,
                operation.operationCode(),
                operation.payload()));
    if (!sent.accepted() && pending.remove(operation.requestId(), batch)) {
      finish(batch.request(), new ErrorPayload(sent.errorCode().name(), sent.description()));
    }
  }

  private void completeOperation(Message message) {
    Batch batch = pending.remove(message.requestId());
    if (batch == null) {
      return;
    }
    if (!message.correlationId().equals(batch.request().correlationId())
        || !(message.payload() instanceof OperationReplyPayload reply)) {
      finish(batch.request(), new ErrorPayload("INVALID_REPLY", "Unexpected operation result"));
      return;
    }
    batch.results().add(reply);
    if (batch.operations().hasNext() && reply.status() == ResultStatus.SUCCESS) {
      submit(batch);
    } else {
      finish(
          batch.request(),
          batch.results().size() == 1
              ? reply
              : new OperationReplyPayload(
                  reply.status(), new OperationResultsPayload(batch.results())));
    }
  }

  private void finish(Message message, MessagePayload result) {
    int target =
        message.senderId() == ClientParticipants.EVENTS
            ? ClientParticipants.UI
            : message.senderId();
    dispatch(
        prepareMessage(
            message.requestId(), message.correlationId(), target, message.operationCode(), result));
  }

  private static UUID targetId(MessagePayload payload) {
    if (payload instanceof TargetPayload target) {
      return target.targetId();
    }
    if (payload instanceof OrderCommandPayload order) {
      return order.orderId();
    }
    if (payload instanceof PlaceOrderPayload order) {
      return order.shopId();
    }
    if (payload instanceof ResolveCasePayload resolution) {
      return resolution.caseId();
    }
    return new UUID(0, 0);
  }

  private record Batch(
      Message request,
      Iterator<OperationRequest> operations,
      List<OperationReplyPayload> results) {}
}
