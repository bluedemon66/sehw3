package edu.nju.sehw.takeout.server.task;

import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.network.ReplyChannel;
import edu.nju.sehw.takeout.server.security.ServerRequestContext;
import java.util.Objects;
import java.util.Optional;

/**
 * Carries an authenticated message, its reply channel, and an optional upload verified for the same
 * context.
 *
 * @param context authenticated principal, server-established permissions, and remote request
 *     identity
 * @param message message envelope containing request identity, routing fields, operation code, and
 *     arguments
 * @param replyChannel reply connection through which the task's result should be sent
 * @param upload authenticated staged upload to persist as a server resource
 */
public record VerifiedTask(
    ServerRequestContext context,
    Message message,
    ReplyChannel replyChannel,
    Optional<VerifiedUpload> upload) {
  /**
   * Creates the VerifiedTask value from the components documented below.
   *
   * @param context authenticated principal, server-established permissions, and remote request
   *     identity
   * @param message message envelope containing request identity, routing fields, operation code,
   *     and arguments
   * @param replyChannel reply connection through which the task's result should be sent
   * @param upload authenticated staged upload to persist as a server resource
   * @throws NullPointerException if a required component, dependency, or collection element is null
   * @throws IllegalArgumentException if the request IDs or the staged upload's authenticated
   *     context disagree
   */
  public VerifiedTask {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(replyChannel, "replyChannel");
    Objects.requireNonNull(upload, "upload");
    if (!context.requestId().equals(message.requestId())) {
      throw new IllegalArgumentException("任务请求标识不一致");
    }
    if (upload.isPresent() && !upload.orElseThrow().context().equals(context)) {
      throw new IllegalArgumentException("上传任务需要同一授权上下文");
    }
  }
}
