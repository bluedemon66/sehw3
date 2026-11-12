package edu.nju.sehw.takeout.client.core;

import java.util.Objects;

/**
 * Reports acceptance or failure of one local Core dispatch attempt.
 *
 * @param accepted whether the local Core accepted and delivered this dispatch
 * @param errorCode machine-readable reason for this local dispatch outcome
 * @param description displayable explanation associated with this check or routing outcome
 */
public record DispatchResult(boolean accepted, DispatchError errorCode, String description) {
  /**
   * Creates the DispatchResult value from the components documented below.
   *
   * @param accepted whether the local Core accepted and delivered this dispatch
   * @param errorCode machine-readable reason for this local dispatch outcome
   * @param description displayable explanation associated with this check or routing outcome
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public DispatchResult {
    Objects.requireNonNull(errorCode, "errorCode");
    Objects.requireNonNull(description, "description");
  }

  /**
   * Creates the successful local-delivery result.
   *
   * @return an accepted result with DispatchError.NONE
   */
  public static DispatchResult success() {
    return new DispatchResult(true, DispatchError.NONE, "已路由");
  }

  /**
   * Creates a local routing failure with its category and explanation.
   *
   * @param error routing failure category to report
   * @param description displayable explanation associated with this check or routing outcome
   * @return a rejected result containing the supplied routing error
   */
  public static DispatchResult rejected(DispatchError error, String description) {
    return new DispatchResult(false, error, description);
  }
}
