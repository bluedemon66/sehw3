package edu.nju.sehw.takeout.server.security;

import edu.nju.sehw.takeout.model.common.CheckResult;
import edu.nju.sehw.takeout.model.common.TargetDescriptor;
import edu.nju.sehw.takeout.server.operation.PreparedOperation;

/** Defines authorization of an operation and target using a verified server identity. */
public interface ServerPermissionChecker {
  /**
   * Authorizes the verified principal for the prepared operation and target object.
   *
   * @param context authenticated principal, server-established permissions, and remote request
   *     identity
   * @param operation indexed operation metadata including permission, schema, and transaction
   *     policy
   * @param target kind and identifier of the business object being authorized
   * @return the server authorization decision and its code and explanation
   */
  CheckResult check(
      ServerRequestContext context, PreparedOperation operation, TargetDescriptor target);
}
