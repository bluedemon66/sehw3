package edu.nju.sehw.takeout.server.comment;

import edu.nju.sehw.takeout.model.comment.WithdrawCommentPayload;
import edu.nju.sehw.takeout.server.security.ServerRequestContext;
import java.util.Objects;

/**
 * Pairs an author-withdrawal request with its authenticated server context.
 *
 * @param context authenticated principal, server-established permissions, and remote request
 *     identity
 * @param payload typed operation arguments or result data from which an isolated snapshot is taken
 */
public record WithdrawCommentCommand(ServerRequestContext context, WithdrawCommentPayload payload) {
  /**
   * Creates the WithdrawCommentCommand value from the components documented below.
   *
   * @param context authenticated principal, server-established permissions, and remote request
   *     identity
   * @param payload typed operation arguments or result data from which an isolated snapshot is
   *     taken
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public WithdrawCommentCommand {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(payload, "payload");
  }
}
