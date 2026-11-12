package edu.nju.sehw.takeout.model.network;

import java.util.Objects;

/**
 * Stores an authentication credential and provides a redacted diagnostic representation.
 *
 * <p>The value accessor supplies authentication text, while toString supplies a redacted diagnostic
 * label.
 */
public final class AccessToken {
  /** Credential text used for session authentication. */
  private final String value;

  /**
   * Creates this component with the dependencies documented below.
   *
   * @param value credential text used for session authentication
   * @throws NullPointerException if a required component, dependency, or collection element is null
   * @throws IllegalArgumentException if the credential text is blank
   */
  public AccessToken(String value) {
    this.value = Objects.requireNonNull(value, "value");
    if (value.isBlank()) {
      throw new IllegalArgumentException("凭据为空");
    }
  }

  /**
   * Returns the credential text used to prepare an authenticated request.
   *
   * @return the stored credential text
   */
  public String value() {
    return value;
  }

  /**
   * Returns the redacted diagnostic representation of this credential.
   *
   * @return the literal label AccessToken[REDACTED]
   */
  @Override
  public String toString() {
    return "AccessToken[REDACTED]";
  }
}
