package edu.nju.sehw.takeout.server.operation;

/** Declares whether an operation uses read-only access or an atomic write transaction. */
public enum TransactionPolicy {
  /** The operation reads stored data. */
  READ_ONLY,
  /** The operation's persistent writes share one transaction. */
  ATOMIC_WRITE
}
