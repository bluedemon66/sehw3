package edu.nju.sehw.takeout.model.comment;

import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Objects;

/**
 * Carries one merchant reply projection as a message payload.
 *
 * @param reply public display projection of the merchant reply
 */
public record ReplyViewPayload(ReplyView reply) implements MessagePayload {
  /**
   * Creates the ReplyViewPayload value from the components documented below.
   *
   * @param reply public display projection of the merchant reply
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public ReplyViewPayload {
    Objects.requireNonNull(reply, "reply");
  }

  /**
   * Creates a payload or message snapshot with independent mutable state.
   *
   * @return a separate snapshot preserving the same argument values and request identities
   */
  @Override
  public ReplyViewPayload deepCopy() {
    return new ReplyViewPayload(reply);
  }
}
