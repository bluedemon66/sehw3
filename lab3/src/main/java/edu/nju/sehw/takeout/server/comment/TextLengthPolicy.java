package edu.nju.sehw.takeout.server.comment;

/** Defines the shared text-length unit used for comment validation and long-comment ordering. */
@FunctionalInterface
public interface TextLengthPolicy {
  /**
   * Measures comment text using the unit chosen for validation and long-comment ordering.
   *
   * @param text comment or reply text supplied for validation, persistence, or display
   * @return the text length expressed in the unit defined by this policy
   */
  int measure(String text);
}
