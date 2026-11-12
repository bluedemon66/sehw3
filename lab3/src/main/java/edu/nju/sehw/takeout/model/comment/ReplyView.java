package edu.nju.sehw.takeout.model.comment;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Carries the public display fields of one merchant reply.
 *
 * @param replyId stable identifier of the merchant reply
 * @param merchantLabel public display label of the replying merchant
 * @param text comment or reply text supplied for validation, persistence, or display
 * @param createdAt instant at which the represented persistent record was created
 */
public record ReplyView(UUID replyId, String merchantLabel, String text, Instant createdAt) {
  /**
   * Creates the ReplyView value from the components documented below.
   *
   * @param replyId stable identifier of the merchant reply
   * @param merchantLabel public display label of the replying merchant
   * @param text comment or reply text supplied for validation, persistence, or display
   * @param createdAt instant at which the represented persistent record was created
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public ReplyView {
    Objects.requireNonNull(replyId, "replyId");
    Objects.requireNonNull(merchantLabel, "merchantLabel");
    Objects.requireNonNull(text, "text");
    Objects.requireNonNull(createdAt, "createdAt");
  }
}
