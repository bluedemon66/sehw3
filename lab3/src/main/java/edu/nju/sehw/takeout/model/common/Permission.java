package edu.nju.sehw.takeout.model.common;

/** Enumerates the permissions used by server authorization and client preflight checks. */
public enum Permission {
  /** Permission to create an order for the authenticated customer. */
  PLACE_ORDER,
  /** Permission to request cancellation of an owned order. */
  CANCEL_OWN_ORDER,
  /** Permission to manage the authenticated merchant's shop. */
  MANAGE_SHOP,
  /** Permission to perform assigned delivery actions. */
  DELIVER_ORDER,
  /** Permission to browse visible comments. */
  READ_COMMENT,
  /** Permission to publish a comment. */
  PUBLISH_COMMENT,
  /** Permission to withdraw an authored comment. */
  WITHDRAW_OWN_COMMENT,
  /** Permission to reply on behalf of the relevant shop. */
  REPLY_SHOP_COMMENT,
  /** Permission to perform platform comment moderation. */
  HIDE_COMMENT,
  /** Permission to upload a server resource. */
  UPLOAD_RESOURCE,
  /** Permission to read an authorized server resource. */
  READ_RESOURCE
}
