package edu.nju.sehw.takeout.client.core;

import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.ui.UiDataPort;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Routes message snapshots between registered client participants using the relation table.
 *
 * <p>Dispatch checks the caller instance, sender and target identifiers, and the allowed operation
 * code before invoking the target parser.
 */
public final class Core {
  /** Participant identities and authorized operation routes used by this Core. */
  private final RelationTable relations = new RelationTable();

  /**
   * Binds a unique participant identifier to the exact endpoint instance.
   *
   * @param id positive identifier assigned to this participant in its local Core
   * @param endpoint participant instance to register or inspect by object identity
   * @throws NullPointerException if endpoint is null
   * @throws IllegalArgumentException if the identifier or instance is already registered, or its
   *     Core identity disagrees
   */
  public void register(int id, CoreEndpoint endpoint) {
    Objects.requireNonNull(endpoint, "endpoint");
    if (endpoint instanceof MessageParticipant participant
        && (participant.participantId() != id || participant.core() != this)) {
      throw new IllegalArgumentException("参与者所属Core及编号须一致");
    }
    relations.register(id, endpoint);
  }

  /**
   * Adds an authorized route between endpoints that have already been registered.
   *
   * @param rule allowed sender, target, operation codes, and UI-reference permission
   * @throws NullPointerException if rule is null
   * @throws IllegalArgumentException if either end of the route has not been registered
   */
  public void allow(RouteRule rule) {
    relations.allow(rule);
  }

  /**
   * Validates the caller and route, then invokes the target parser with a copied message.
   *
   * @param caller actual registered participant instance requesting dispatch or a UI reference
   * @param senderId registered identifier of the participant sending the message
   * @param targetId registered identifier of the participant that should receive the message
   * @param message message envelope containing request identity, routing fields, operation code,
   *     and arguments
   * @return acceptance after local delivery, or an error describing the rejected or failed dispatch
   */
  public DispatchResult dispatch(CoreEndpoint caller, int senderId, int targetId, Message message) {
    if (message == null || senderId != message.senderId() || targetId != message.targetId()) {
      return DispatchResult.rejected(DispatchError.INVALID_ENVELOPE, "消息头与调用参数需一致");
    }
    OptionalInt registered = relations.identityOf(caller);
    if (registered.isEmpty() || registered.getAsInt() != senderId) {
      return DispatchResult.rejected(DispatchError.UNREGISTERED_SENDER, "请使用已注册的发送实例");
    }
    CoreEndpoint target = relations.lookup(targetId).orElse(null);
    if (target == null) {
      return DispatchResult.rejected(DispatchError.UNKNOWN_TARGET, "目标尚未注册");
    }
    if (!relations.isAllowed(senderId, targetId, message.operationCode())) {
      return DispatchResult.rejected(DispatchError.FORBIDDEN_ROUTE, "该操作的路由尚未授权");
    }
    try {
      target.parse(message.deepCopy());
      return DispatchResult.success();
    } catch (RuntimeException exception) {
      return DispatchResult.rejected(DispatchError.TARGET_FAILED, "目标处理器执行失败");
    }
  }

  /**
   * Resolves an authorized local UI callback using the registered caller identity.
   *
   * @param caller actual registered participant instance requesting dispatch or a UI reference
   * @param targetId registered identifier of the participant that should receive the message
   * @return the target's UiDataPort for returning an assembled page
   * @throws SecurityException if the caller is unregistered or the UI-reference route is
   *     unauthorized
   * @throws IllegalArgumentException if the target is absent or does not implement UiDataPort
   */
  public UiDataPort getTargetRef(CoreEndpoint caller, int targetId) {
    OptionalInt sender = relations.identityOf(caller);
    if (sender.isEmpty() || !relations.canReturnUiRef(sender.getAsInt(), targetId)) {
      throw new SecurityException("UI回传端口需要关系表授权");
    }
    CoreEndpoint target =
        relations.lookup(targetId).orElseThrow(() -> new IllegalArgumentException("UI目标尚未注册"));
    if (!(target instanceof UiDataPort port)) {
      throw new IllegalArgumentException("目标需提供UiDataPort");
    }
    return port;
  }
}
