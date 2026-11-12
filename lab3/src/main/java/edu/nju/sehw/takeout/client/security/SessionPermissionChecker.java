package edu.nju.sehw.takeout.client.security;

import edu.nju.sehw.takeout.client.query.ClientSession;
import edu.nju.sehw.takeout.model.common.CheckResult;
import edu.nju.sehw.takeout.model.common.TargetDescriptor;
import edu.nju.sehw.takeout.scaffold.NotImplementedYetException;
import java.util.Objects;

/** Evaluates client preflight permissions using the current cached session. */
public final class SessionPermissionChecker implements ClientPermissionChecker {
  /** Current client identity, cached permissions, credential, and expiry. */
  private final ClientSession session;

  /**
   * Creates this component with the dependencies documented below.
   *
   * @param session current client identity, cached permissions, credential, and expiry
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public SessionPermissionChecker(ClientSession session) {
    this.session = Objects.requireNonNull(session, "session");
  }

  /**
   * Checks the client session's permission preflight for an operation and target.
   *
   * @param operationCode numeric identifier selecting the operation to execute
   * @param target kind and identifier of the business object being authorized
   * @return the preflight decision, machine-readable code, and displayable explanation
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  @Override
  public CheckResult check(int operationCode, TargetDescriptor target) {
    throw new NotImplementedYetException("SessionPermissionChecker.check");
  }
}
