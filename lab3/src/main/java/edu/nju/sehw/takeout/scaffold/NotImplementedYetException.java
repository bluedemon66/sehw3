package edu.nju.sehw.takeout.scaffold;

/**
 * Identifies a framework entry point whose business or infrastructure implementation is pending.
 */
public final class NotImplementedYetException extends UnsupportedOperationException {
  /** Serialization version of this exception class. */
  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception identifying the pending framework entry point.
   *
   * @param operation qualified name of the entry point awaiting implementation
   */
  public NotImplementedYetException(String operation) {
    super("待实现：" + operation);
  }
}
