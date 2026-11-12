package edu.nju.sehw.takeout.model.comment;

/** Describes the purchase-evidence label calculated by the server for a review author. */
public enum PurchaseMark {
  /** Server-side evidence associates the author with a relevant purchase. */
  PURCHASED,
  /** The server did not find the relevant purchase evidence. */
  NOT_PURCHASED
}
