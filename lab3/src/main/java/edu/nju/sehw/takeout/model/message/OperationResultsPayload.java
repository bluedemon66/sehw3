package edu.nju.sehw.takeout.model.message;

import java.util.List;

/**
 * Carries the results of a local batch in the order its operations were submitted.
 *
 * @param results completed operation results
 */
public record OperationResultsPayload(List<OperationReplyPayload> results)
    implements MessagePayload {
  /** Snapshots the batch results. */
  public OperationResultsPayload {
    results = List.copyOf(results);
  }

  @Override
  public OperationResultsPayload deepCopy() {
    return new OperationResultsPayload(results);
  }
}
