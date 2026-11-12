package edu.nju.sehw.takeout.client.core;

import java.util.Objects;
import java.util.Set;

/**
 * Describes allowed operation codes and UI-reference access between two local participants.
 *
 * @param senderId registered identifier of the participant sending the message
 * @param targetId registered identifier of the participant that should receive the message
 * @param allowedOperationCodes operation identifiers permitted between the rule's sender and target
 * @param allowUiReturnRef whether this route also permits returning the target's UiDataPort
 */
public record RouteRule(
    int senderId, int targetId, Set<Integer> allowedOperationCodes, boolean allowUiReturnRef) {
  /**
   * Creates the RouteRule value from the components documented below.
   *
   * @param senderId registered identifier of the participant sending the message
   * @param targetId registered identifier of the participant that should receive the message
   * @param allowedOperationCodes operation identifiers permitted between the rule's sender and
   *     target
   * @param allowUiReturnRef whether this route also permits returning the target's UiDataPort
   * @throws NullPointerException if a required component, dependency, or collection element is null
   * @throws IllegalArgumentException if a participant or permitted operation identifier is
   *     nonpositive
   */
  public RouteRule {
    Objects.requireNonNull(allowedOperationCodes, "allowedOperationCodes");
    allowedOperationCodes = Set.copyOf(allowedOperationCodes);
    if (senderId <= 0 || targetId <= 0 || allowedOperationCodes.stream().anyMatch(i -> i <= 0)) {
      throw new IllegalArgumentException("路由编号须为正整数");
    }
  }
}
