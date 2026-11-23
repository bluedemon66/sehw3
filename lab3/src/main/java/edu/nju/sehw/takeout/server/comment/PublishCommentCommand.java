package edu.nju.sehw.takeout.server.comment;

import edu.nju.sehw.takeout.model.comment.PublishCommentPayload;
import edu.nju.sehw.takeout.model.comment.PurchaseMark;
import edu.nju.sehw.takeout.server.security.ServerRequestContext;
import java.util.Objects;

/**
 * Pairs a comment publication request with its authenticated server context.
 *
 * @param context authenticated principal, server-established permissions, and remote request
 *     identity
 * @param payload typed operation arguments or result data from which an isolated snapshot is taken
 */
public record PublishCommentCommand(
    ServerRequestContext context,
    PublishCommentPayload payload,
    PurchaseMark purchaseMark,
    int bodyLength) {
  /** Creates a command using the default Unicode code-point length policy. */
  public PublishCommentCommand(ServerRequestContext context, PublishCommentPayload payload) {
    this(
        context,
        payload,
        PurchaseMark.NOT_PURCHASED,
        payload.text().codePointCount(0, payload.text().length()));
  }

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
    Objects.requireNonNull(purchaseMark, "purchaseMark");
  }
}
