package edu.nju.sehw.takeout.server.operation;

import edu.nju.sehw.takeout.model.common.Permission;
import java.util.Objects;

/**
 * Stores an operation's stable code, required permission, input schema, and transaction policy.
 *
 * @param operationCode numeric identifier selecting the operation to execute
 * @param requiredPermission permission the verified principal must hold for this operation
 * @param parameterSchema field types and required names used to validate the operation's arguments
 * @param transactionPolicy read-only or atomic-write policy attached to this operation
 */
public record PreparedOperation(
    int operationCode,
    Permission requiredPermission,
    ParameterSchema parameterSchema,
    TransactionPolicy transactionPolicy) {
  /**
   * Creates the PreparedOperation value from the components documented below.
   *
   * @param operationCode numeric identifier selecting the operation to execute
   * @param requiredPermission permission the verified principal must hold for this operation
   * @param parameterSchema field types and required names used to validate the operation's
   *     arguments
   * @param transactionPolicy read-only or atomic-write policy attached to this operation
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public PreparedOperation {
    Objects.requireNonNull(requiredPermission, "requiredPermission");
    Objects.requireNonNull(parameterSchema, "parameterSchema");
    Objects.requireNonNull(transactionPolicy, "transactionPolicy");
  }
}
