package edu.nju.sehw.takeout.server.operation;

import edu.nju.sehw.takeout.model.network.SecureResponse;
import edu.nju.sehw.takeout.server.task.VerifiedTask;

/** Defines execution of one authenticated server task and construction of its protocol response. */
public interface OperationExecutor {
  /**
   * Executes one verified operation using its authorization, idempotency, and transaction
   * contracts.
   *
   * @param task authenticated request, reply channel, and optional staged upload to execute
   * @return the response associated with the task's remote request identifier
   */
  SecureResponse execute(VerifiedTask task);
}
