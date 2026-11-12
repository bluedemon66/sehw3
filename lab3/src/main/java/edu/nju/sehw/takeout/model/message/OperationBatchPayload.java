package edu.nju.sehw.takeout.model.message;

import java.util.List;
import java.util.Objects;

/**
 * Carries an ordered batch of numbered operations belonging to a client workflow.
 *
 * @param requests ordered numbered operations belonging to the planned client workflow
 */
public record OperationBatchPayload(List<OperationRequest> requests) implements MessagePayload {
  /**
   * Creates the OperationBatchPayload value from the components documented below.
   *
   * @param requests ordered numbered operations belonging to the planned client workflow
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public OperationBatchPayload {
    Objects.requireNonNull(requests, "requests");
    requests = List.copyOf(requests);
  }

  /**
   * Creates a payload or message snapshot with independent mutable state.
   *
   * @return a separate snapshot preserving the same argument values and request identities
   */
  @Override
  public OperationBatchPayload deepCopy() {
    return new OperationBatchPayload(requests);
  }
}
