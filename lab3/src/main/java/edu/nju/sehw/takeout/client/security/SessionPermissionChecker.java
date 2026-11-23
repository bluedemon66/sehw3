package edu.nju.sehw.takeout.client.security;

import edu.nju.sehw.takeout.client.query.ClientSession;
import edu.nju.sehw.takeout.model.common.CheckResult;
import edu.nju.sehw.takeout.model.common.Permission;
import edu.nju.sehw.takeout.model.common.TargetDescriptor;
import edu.nju.sehw.takeout.model.message.OperationCode;
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
   */
  @Override
  public CheckResult check(int operationCode, TargetDescriptor target) {
    var operation = OperationCode.fromCode(operationCode);
    Permission permission =
        switch (operation) {
          case OPEN_PAGE ->
              switch (target.kind()) {
                case "comment" -> Permission.READ_COMMENT;
                case "turnover" -> Permission.MANAGE_SHOP;
                case "rider" -> Permission.DELIVER_ORDER;
                default -> null;
              };
          case UI_INPUT -> null;
          case ORDER_DETAIL -> Permission.READ_ORDER;
          case PLACE_ORDER, CONFIRM_RECEIPT, REPORT_NOT_RECEIVED -> Permission.PLACE_ORDER;
          case CANCEL_ORDER -> Permission.CANCEL_OWN_ORDER;
          case ACCEPT_ORDER,
              REJECT_ORDER,
              START_PREPARATION,
              FINISH_PREPARATION,
              MERCHANT_TURNOVER ->
              Permission.MANAGE_SHOP;
          case RESOLVE_CASE -> Permission.RESOLVE_CASE;
          case RIDER_PICKUP, CONFIRM_DELIVERY, RIDER_TASKS -> Permission.DELIVER_ORDER;
          case COMMENT_LIST -> Permission.READ_COMMENT;
          case COMMENT_PUBLISH -> Permission.PUBLISH_COMMENT;
          case COMMENT_WITHDRAW -> Permission.WITHDRAW_OWN_COMMENT;
          case COMMENT_REPLY -> Permission.REPLY_SHOP_COMMENT;
          case COMMENT_HIDE -> Permission.HIDE_COMMENT;
          case GET_RESOURCE -> Permission.READ_RESOURCE;
          case UPLOAD_ATTACHMENT -> Permission.UPLOAD_RESOURCE;
        };
    boolean allowed = permission == null || session.permissions().contains(permission);
    return new CheckResult(
        allowed,
        allowed ? "OK" : "FORBIDDEN",
        allowed ? "Operation permitted" : "Permission required: " + permission);
  }
}
