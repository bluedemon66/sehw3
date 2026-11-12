package edu.nju.sehw.takeout.model.message;

/** Defines the copy-isolation contract for extensible message arguments and results. */
public interface MessagePayload {
  /**
   * Creates a payload or message snapshot with independent mutable state.
   *
   * @return a separate snapshot preserving the same argument values and request identities
   */
  MessagePayload deepCopy();
}
