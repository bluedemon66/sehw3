package edu.nju.sehw.takeout.server.comment;

import edu.nju.sehw.takeout.model.comment.HideCommentPayload;
import edu.nju.sehw.takeout.server.security.ServerRequestContext;
import java.util.Objects;

/**
 * Pairs a platform hide request with the authenticated server context that issued it.
 *
 * @param context authenticated principal, server-established permissions, and remote request
 *     identity
 * @param payload typed operation arguments or result data from which an isolated snapshot is taken
 */
public record HideCommentCommand(ServerRequestContext context, HideCommentPayload payload) {
  /**
   * Creates the HideCommentCommand value from the components documented below.
   *
   * @param context authenticated principal, server-established permissions, and remote request
   *     identity
   * @param payload typed operation arguments or result data from which an isolated snapshot is
   *     taken
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public HideCommentCommand {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(payload, "payload");
  }
}
