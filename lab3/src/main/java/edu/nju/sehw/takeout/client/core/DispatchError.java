package edu.nju.sehw.takeout.client.core;

/** Identifies the reason a local routing attempt was rejected or failed. */
public enum DispatchError {
  /** The local routing operation completed successfully. */
  NONE,
  /** The message header and supplied sender or target identifiers disagree. */
  INVALID_ENVELOPE,
  /** The caller instance does not match the registered sender identity. */
  UNREGISTERED_SENDER,
  /** The requested recipient has no registered endpoint. */
  UNKNOWN_TARGET,
  /** The relation table does not authorize this sender, target, and operation. */
  FORBIDDEN_ROUTE,
  /** The caller supplied a different Core from the participant's stored Core. */
  WRONG_CORE,
  /** The target parser failed during local message delivery. */
  TARGET_FAILED
}
