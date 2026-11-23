package edu.nju.sehw.takeout.server.security;

import edu.nju.sehw.takeout.model.common.CheckResult;
import edu.nju.sehw.takeout.model.common.TargetDescriptor;
import edu.nju.sehw.takeout.model.message.OperationCode;
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
   */
  @Override
  public CheckResult check(
      ServerRequestContext context, PreparedOperation operation, TargetDescriptor target) {
    if (!targetMatches(OperationCode.fromCode(operation.operationCode()), target.kind())) {
      return new CheckResult(false, "INVALID_TARGET", "Operation does not accept this target kind");
    }
    boolean allowed = context.verifiedPermissions().contains(operation.requiredPermission());
    return new CheckResult(
        allowed,
        allowed ? "OK" : "FORBIDDEN",
        allowed ? "Operation permitted" : "Permission required: " + operation.requiredPermission());
  }

  private static boolean targetMatches(OperationCode operation, String kind) {
    return switch (operation) {
      case PLACE_ORDER, MERCHANT_TURNOVER -> kind.equals("shop");
      case CANCEL_ORDER,
          CONFIRM_RECEIPT,
          REPORT_NOT_RECEIVED,
          START_PREPARATION,
          RIDER_PICKUP,
          CONFIRM_DELIVERY,
          ACCEPT_ORDER,
          REJECT_ORDER,
          FINISH_PREPARATION,
          ORDER_DETAIL ->
          kind.equals("order");
      case RESOLVE_CASE -> kind.equals("case");
      case RIDER_TASKS -> kind.equals("rider");
      case COMMENT_LIST, COMMENT_PUBLISH ->
          kind.equals("SHOP") || kind.equals("DISH") || kind.equals("RIDER_SERVICE");
      case COMMENT_WITHDRAW, COMMENT_REPLY, COMMENT_HIDE -> kind.equals("comment");
      case GET_RESOURCE, UPLOAD_ATTACHMENT -> kind.equals("resource");
      case UI_INPUT, OPEN_PAGE -> false;
    };
  }
}
