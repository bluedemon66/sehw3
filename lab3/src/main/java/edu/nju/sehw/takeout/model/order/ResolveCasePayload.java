package edu.nju.sehw.takeout.model.order;

import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Objects;
import java.util.UUID;

/**
 * Carries a communication case resolution.
 *
 * @param caseId communication case identifier
 * @param resolution resolution text
 */
public record ResolveCasePayload(UUID caseId, String resolution) implements MessagePayload {
  /** Validates and snapshots the supplied fields. */
  public ResolveCasePayload {
    Objects.requireNonNull(caseId, "caseId");
    Objects.requireNonNull(resolution, "resolution");
    if (resolution.isBlank() || resolution.length() > 2000) {
      throw new IllegalArgumentException("Invalid resolution text");
    }
  }

  @Override
  public ResolveCasePayload deepCopy() {
    return new ResolveCasePayload(caseId, resolution);
  }
}
