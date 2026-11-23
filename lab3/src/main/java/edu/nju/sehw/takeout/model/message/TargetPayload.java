package edu.nju.sehw.takeout.model.message;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifies the target of a domain query.
 *
 * @param targetId identifier of the requested order or shop
 */
public record TargetPayload(UUID targetId) implements MessagePayload {
  /** Validates and snapshots the supplied fields. */
  public TargetPayload {
    Objects.requireNonNull(targetId, "targetId");
  }

  @Override
  public TargetPayload deepCopy() {
    return new TargetPayload(targetId);
  }
}
