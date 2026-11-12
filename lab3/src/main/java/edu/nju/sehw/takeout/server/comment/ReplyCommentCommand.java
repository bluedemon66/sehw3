package edu.nju.sehw.takeout.server.comment;

import edu.nju.sehw.takeout.model.comment.ReplyCommentPayload;
import edu.nju.sehw.takeout.server.security.ServerRequestContext;
import java.util.Objects;

/**
 * Pairs merchant reply text with the authenticated server context that issued it.
 *
 * @param context authenticated principal, server-established permissions, and remote request
 *     identity
 * @param payload typed operation arguments or result data from which an isolated snapshot is taken
 */
public record ReplyCommentCommand(ServerRequestContext context, ReplyCommentPayload payload) {
  /**
   * Creates the ReplyCommentCommand value from the components documented below.
   *
   * @param context authenticated principal, server-established permissions, and remote request
   *     identity
   * @param payload typed operation arguments or result data from which an isolated snapshot is
   *     taken
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public ReplyCommentCommand {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(payload, "payload");
  }
}
