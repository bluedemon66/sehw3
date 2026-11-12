package edu.nju.sehw.takeout.client.security;

import edu.nju.sehw.takeout.model.common.CheckResult;
import edu.nju.sehw.takeout.model.common.TargetDescriptor;

/** Defines client-side permission preflight for an operation and its target object. */
public interface ClientPermissionChecker {
  /**
   * Checks the client session's permission preflight for an operation and target.
   *
   * @param operationCode numeric identifier selecting the operation to execute
   * @param target kind and identifier of the business object being authorized
   * @return the preflight decision, machine-readable code, and displayable explanation
   */
  CheckResult check(int operationCode, TargetDescriptor target);
}
