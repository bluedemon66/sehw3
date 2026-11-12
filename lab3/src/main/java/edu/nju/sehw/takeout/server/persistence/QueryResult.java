package edu.nju.sehw.takeout.server.persistence;

import edu.nju.sehw.takeout.model.common.ResultStatus;
import edu.nju.sehw.takeout.model.message.MessagePayload;
import java.util.Objects;

/**
 * Carries the result status and copied payload produced by a data operation.
 *
 * @param status business or protocol outcome of the server operation
 * @param payload typed operation arguments or result data from which an isolated snapshot is taken
 */
public record QueryResult(ResultStatus status, MessagePayload payload) {
  /**
   * Creates the QueryResult value from the components documented below.
   *
   * @param status business or protocol outcome of the server operation
   * @param payload typed operation arguments or result data from which an isolated snapshot is
   *     taken
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public QueryResult {
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(payload, "payload");
    payload = Objects.requireNonNull(payload.deepCopy(), "payload snapshot");
  }

  /**
   * Returns an isolated snapshot of the stored operation data.
   *
   * @return a copy of the stored typed payload
   */
  @Override
  public MessagePayload payload() {
    return payload.deepCopy();
  }
}
