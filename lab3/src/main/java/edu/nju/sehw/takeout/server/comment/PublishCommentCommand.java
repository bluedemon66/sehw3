package edu.nju.sehw.takeout.server.comment;

import edu.nju.sehw.takeout.model.comment.PublishCommentPayload;
import edu.nju.sehw.takeout.server.security.ServerRequestContext;
import java.util.Objects;

/**
 * Pairs a comment publication request with its authenticated server context.
 *
 * @param context authenticated principal, server-established permissions, and remote request
 *     identity
 * @param payload typed operation arguments or result data from which an isolated snapshot is taken
 */
public record PublishCommentCommand(ServerRequestContext context, PublishCommentPayload payload) {
  /**
   * Creates the PublishCommentCommand value from the components documented below.
   *
   * @param context authenticated principal, server-established permissions, and remote request
   *     identity
   * @param payload typed operation arguments or result data from which an isolated snapshot is
   *     taken
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public PublishCommentCommand {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(payload, "payload");
  }
}
