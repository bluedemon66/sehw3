package edu.nju.sehw.takeout.server.security;

import edu.nju.sehw.takeout.model.common.CheckResult;
import edu.nju.sehw.takeout.model.common.TargetDescriptor;
import edu.nju.sehw.takeout.scaffold.NotImplementedYetException;
import edu.nju.sehw.takeout.server.operation.PreparedOperation;

/** Evaluates an authenticated principal's operation permission and target-object access. */
public final class DefaultServerPermissionChecker implements ServerPermissionChecker {

  /** Creates this component with the dependencies documented below. */
  public DefaultServerPermissionChecker() {}

  /**
   * Authorizes the verified principal for the prepared operation and target object.
   *
   * @param context authenticated principal, server-established permissions, and remote request
   *     identity
   * @param operation indexed operation metadata including permission, schema, and transaction
   *     policy
   * @param target kind and identifier of the business object being authorized
   * @return the server authorization decision and its code and explanation
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  @Override
  public CheckResult check(
      ServerRequestContext context, PreparedOperation operation, TargetDescriptor target) {
    throw new NotImplementedYetException("DefaultServerPermissionChecker.check");
  }
}
