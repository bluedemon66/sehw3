package edu.nju.sehw.takeout.server.operation;

/** Describes the scalar field types understood by an operation's parameter schema. */
public enum ValueType {
  /** A textual parameter value. */
  STRING,
  /** An integral numeric parameter value. */
  INTEGER,
  /** An exact decimal parameter value. */
  DECIMAL,
  /** A boolean parameter value. */
  BOOLEAN,
  /** A UUID parameter value. */
  UUID
}
