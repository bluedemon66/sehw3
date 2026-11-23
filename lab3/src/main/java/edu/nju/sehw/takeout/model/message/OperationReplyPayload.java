package edu.nju.sehw.takeout.model.message;

import edu.nju.sehw.takeout.model.common.ResultStatus;
import java.util.Objects;

/**
 * Carries a network operation result through the local Core.
 *
 * @param status outcome of the operation
 * @param payload returned domain data
 */
public record OperationReplyPayload(ResultStatus status, MessagePayload payload)
    implements MessagePayload {
  /** Creates a reply with a defensive copy of its payload. */
  public OperationReplyPayload {
    Objects.requireNonNull(status, "status");
    payload = Objects.requireNonNull(payload, "payload").deepCopy();
  }

  @Override
  public MessagePayload payload() {
    return payload.deepCopy();
  }

  @Override
  public OperationReplyPayload deepCopy() {
    return new OperationReplyPayload(status, payload);
  }
}
