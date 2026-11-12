package edu.nju.sehw.takeout.model.message;

/** Represents an operation that carries no additional arguments. */
public record EmptyPayload() implements MessagePayload {
  /**
   * Creates a payload or message snapshot with independent mutable state.
   *
   * @return a separate snapshot preserving the same argument values and request identities
   */
  @Override
  public EmptyPayload deepCopy() {
    return new EmptyPayload();
  }
}
