package edu.nju.sehw.takeout.server.persistence;

/** Classifies an attempt to claim an authenticated request identifier for execution. */
public enum ClaimStatus {
  /** The caller owns the request's execution claim. */
  CLAIMED,
  /** A saved response is available for this request identifier. */
  REPLAY,
  /** The request has been accepted for processing and has no final result yet. */
  IN_PROGRESS,
  /** The request identifier is already associated with a different body digest. */
  DIGEST_MISMATCH
}
