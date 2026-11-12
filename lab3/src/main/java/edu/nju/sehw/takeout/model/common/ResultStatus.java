package edu.nju.sehw.takeout.model.common;

/** Classifies the business or protocol outcome returned by the server. */
public enum ResultStatus {
  /** The server operation completed successfully. */
  SUCCESS,
  /** Business processing rejected the requested operation. */
  REJECTED,
  /** The operation conflicted with the current persisted state or version. */
  CONFLICT,
  /** The requested business object or resource was not found. */
  NOT_FOUND,
  /** The authenticated principal lacks access to this operation or target. */
  FORBIDDEN,
  /** The request does not establish an authenticated server session. */
  UNAUTHENTICATED,
  /** The request envelope or arguments failed validation. */
  INVALID_REQUEST,
  /** The server cannot enqueue more work at the moment. */
  BUSY,
  /** The request has been accepted for processing and has no final result yet. */
  IN_PROGRESS,
  /** Execution of the server operation failed. */
  FAILED
}
