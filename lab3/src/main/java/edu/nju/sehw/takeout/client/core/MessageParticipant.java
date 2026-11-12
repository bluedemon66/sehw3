package edu.nju.sehw.takeout.client.core;

import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Provides participant identity, a private Core reference, and synchronized message snapshot
 * creation.
 *
 * <p>A draft lock covers template construction and the private maker call. Sending takes place
 * after snapshot preparation returns.
 */
public abstract class MessageParticipant implements CoreEndpoint {
  /** Positive identifier assigned to this participant in its local Core. */
  private final int participantId;

  /** Local Core that owns this participant and routes its messages. */
  private final Core core;

  /** Lock protecting replacement and copying of the participant's private message draft. */
  private final ReentrantLock draftLock = new ReentrantLock();

  /** Last prepared message draft, accessed while holding draftLock. */
  private Message message;

  /**
   * Creates this participant with the dependencies documented below.
   *
   * @param participantId positive identifier assigned to this participant in its local Core
   * @param core local Core that owns this participant and routes its messages
   * @throws NullPointerException if a required component, dependency, or collection element is null
   * @throws IllegalArgumentException if participantId is nonpositive
   */
  protected MessageParticipant(int participantId, Core core) {
    if (participantId <= 0) {
      throw new IllegalArgumentException("参与者编号须为正整数");
    }
    this.participantId = participantId;
    this.core = Objects.requireNonNull(core, "core");
  }

  /**
   * Returns the identifier assigned to this local participant.
   *
   * @return the positive local participant identifier
   */
  public final int participantId() {
    return participantId;
  }

  /**
   * Returns the Core stored by this participant.
   *
   * @return the Core instance that owns this participant
   */
  protected final Core core() {
    return core;
  }

  /**
   * Builds the private draft under its lock and returns a separate message snapshot.
   *
   * @param requestId identifier of this individual request; retries reuse the same identifier
   * @param targetId registered identifier of the participant that should receive the message
   * @param operationCode numeric identifier selecting the operation to execute
   * @param payload typed operation arguments or result data from which an isolated snapshot is
   *     taken
   * @return a new message carrying this participant's sender ID and the supplied request arguments
   * @throws NullPointerException if a required request identifier or payload is null
   * @throws IllegalArgumentException if the target or operation identifier is nonpositive
   */
  protected final Message prepareMessage(
      UUID requestId, int targetId, int operationCode, MessagePayload payload) {
    return prepareMessage(requestId, requestId, targetId, operationCode, payload);
  }

  /**
   * Builds the private draft under its lock and returns a separate message snapshot.
   *
   * @param requestId identifier of this individual request; retries reuse the same identifier
   * @param correlationId identifier shared by the child requests of one root UI operation
   * @param targetId registered identifier of the participant that should receive the message
   * @param operationCode numeric identifier selecting the operation to execute
   * @param payload typed operation arguments or result data from which an isolated snapshot is
   *     taken
   * @return a new message carrying this participant's sender ID and the supplied request arguments
   * @throws NullPointerException if a required request identifier or payload is null
   * @throws IllegalArgumentException if the target or operation identifier is nonpositive
   */
  protected final Message prepareMessage(
      UUID requestId, UUID correlationId, int targetId, int operationCode, MessagePayload payload) {
    draftLock.lock();
    try {
      message =
          new Message(requestId, correlationId, participantId, targetId, operationCode, payload);
      return maker();
    } finally {
      draftLock.unlock();
    }
  }

  /**
   * Copies the prepared private draft while the caller holds the draft lock.
   *
   * @return a separate Message snapshot with isolated mutable payload state
   */
  private Message maker() {
    return message.deepCopy();
  }

  /**
   * Submits the supplied snapshot through the participant's owning Core.
   *
   * @param senderId registered identifier of the participant sending the message
   * @param targetId registered identifier of the participant that should receive the message
   * @param suppliedCore Core instance supplied by the caller and checked against the participant's
   *     stored Core
   * @param snapshot prepared message snapshot to deliver through the participant's Core
   * @return local routing acceptance or the routing failure category
   */
  @Override
  public final DispatchResult send(
      int senderId, int targetId, Core suppliedCore, Message snapshot) {
    if (suppliedCore != core) {
      return DispatchResult.rejected(DispatchError.WRONG_CORE, "请使用参与者所属Core");
    }
    if (senderId != participantId) {
      return DispatchResult.rejected(DispatchError.INVALID_ENVELOPE, "发送者编号需与当前参与者一致");
    }
    return core.dispatch(this, senderId, targetId, snapshot);
  }

  /**
   * Consumes a message delivered by Core and selects this participant's handling strategy.
   *
   * @param message message envelope containing request identity, routing fields, operation code,
   *     and arguments
   */
  @Override
  public abstract void parse(Message message);
}
