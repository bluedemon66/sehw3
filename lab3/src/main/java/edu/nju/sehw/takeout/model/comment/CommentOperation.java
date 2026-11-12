package edu.nju.sehw.takeout.model.comment;

import edu.nju.sehw.takeout.model.message.OperationCode;

/** Maps each comment operation to its established global operation code. */
public enum CommentOperation {
  /** Query a page of visible comments. */
  LIST(OperationCode.COMMENT_LIST),
  /** Publish a new comment. */
  PUBLISH(OperationCode.COMMENT_PUBLISH),
  /** Withdraw the author's comment. */
  WITHDRAW(OperationCode.COMMENT_WITHDRAW),
  /** Add a merchant reply. */
  REPLY(OperationCode.COMMENT_REPLY),
  /** Apply platform moderation to a comment. */
  HIDE(OperationCode.COMMENT_HIDE),
  /** Upload an image or other permitted attachment. */
  UPLOAD_ATTACHMENT(OperationCode.UPLOAD_ATTACHMENT);

  /** Global operation entry associated with this symbolic comment action. */
  private final OperationCode operation;

  /**
   * Associates this symbolic operation with its configured numeric protocol entry.
   *
   * @param operation global operation entry associated with this symbolic comment action
   */
  CommentOperation(OperationCode operation) {
    this.operation = operation;
  }

  /**
   * Returns the numeric identifier assigned to this symbolic operation.
   *
   * @return the stable global operation code
   */
  public int code() {
    return operation.code();
  }
}
