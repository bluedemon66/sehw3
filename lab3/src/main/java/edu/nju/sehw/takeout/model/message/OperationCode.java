package edu.nju.sehw.takeout.model.message;

import java.util.Arrays;

/** Registers the built-in numeric operation identifiers used by the framework. */
public enum OperationCode {
  /** Create an order from the customer's selected items. */
  PLACE_ORDER(1101),
  /** Request cancellation of the customer's queued order. */
  CANCEL_ORDER(1102),
  /** Record the customer's confirmation that the order was received. */
  CONFIRM_RECEIPT(1103),
  /** Report a delivered order as not received and start exception handling. */
  REPORT_NOT_RECEIVED(1104),
  /** Move an accepted queued order into preparation. */
  START_PREPARATION(1105),
  /** Record the assigned rider's verified pickup. */
  RIDER_PICKUP(1106),
  /** Record the rider's delivery and await customer confirmation. */
  CONFIRM_DELIVERY(1107),
  /** Request navigation to or refresh of a local page. */
  OPEN_PAGE(2101),
  /** Request retrieval of a remote resource. */
  GET_RESOURCE(3101),
  /** Query visible comments with the selected ordering and cursor. */
  COMMENT_LIST(4101),
  /** Publish comment text and referenced uploaded images. */
  COMMENT_PUBLISH(4102),
  /** Withdraw a comment as its author. */
  COMMENT_WITHDRAW(4103),
  /** Add a merchant reply to an eligible comment. */
  COMMENT_REPLY(4104),
  /** Hide a comment through platform moderation. */
  COMMENT_HIDE(4105),
  /** Upload an image or other permitted attachment. */
  UPLOAD_ATTACHMENT(4106);

  /** Numeric operation identifier to register or resolve. */
  private final int code;

  /**
   * Associates this symbolic operation with its configured numeric protocol entry.
   *
   * @param code numeric operation identifier to register or resolve
   */
  OperationCode(int code) {
    this.code = code;
  }

  /**
   * Returns the numeric identifier assigned to this symbolic operation.
   *
   * @return the stable global operation code
   */
  public int code() {
    return code;
  }

  /**
   * Resolves a built-in symbolic operation from its numeric identifier.
   *
   * @param code numeric operation identifier to register or resolve
   * @return the built-in operation registered for the supplied number
   * @throws IllegalArgumentException if the number is not a registered built-in operation
   */
  public static OperationCode fromCode(int code) {
    return Arrays.stream(values())
        .filter(value -> value.code == code)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("未知操作编号：" + code));
  }
}
