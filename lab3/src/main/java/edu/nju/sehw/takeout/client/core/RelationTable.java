package edu.nju.sehw.takeout.client.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Stores participant instance identities and the routes permitted between them.
 *
 * <p>Registry and rule queries are synchronized. Participant identity is compared by object
 * identity.
 */
public final class RelationTable {
  /** Registered local participant identifiers mapped to endpoint instances. */
  private final Map<Integer, CoreEndpoint> targets = new HashMap<>();

  /** Reverse identity-based lookup from an endpoint instance to its registered identifier. */
  private final Map<CoreEndpoint, Integer> identities = new IdentityHashMap<>();

  /** Configured operation routes and UI-reference permissions. */
  private final Set<RouteRule> rules = new HashSet<>();

  /**
   * Registers a unique identifier and endpoint instance in both identity indexes.
   *
   * @param id positive identifier assigned to this participant in its local Core
   * @param endpoint participant instance to register or inspect by object identity
   * @throws NullPointerException if endpoint is null
   * @throws IllegalArgumentException if the identifier is nonpositive or either identity is already
   *     registered
   */
  public synchronized void register(int id, CoreEndpoint endpoint) {
    Objects.requireNonNull(endpoint, "endpoint");
    if (id <= 0 || targets.containsKey(id) || identities.containsKey(endpoint)) {
      throw new IllegalArgumentException("参与者编号和实例须唯一");
    }
    targets.put(id, endpoint);
    identities.put(endpoint, id);
  }

  /**
   * Adds a rule for a sender and target that are already present in the registry.
   *
   * @param rule allowed sender, target, operation codes, and UI-reference permission
   * @throws NullPointerException if rule is null
   * @throws IllegalArgumentException if either participant has not been registered
   */
  public synchronized void allow(RouteRule rule) {
    Objects.requireNonNull(rule, "rule");
    if (!targets.containsKey(rule.senderId()) || !targets.containsKey(rule.targetId())) {
      throw new IllegalArgumentException("配置路由前完成两端注册");
    }
    rules.add(rule);
  }

  /**
   * Looks up the endpoint registered for a local participant identifier.
   *
   * @param id registered participant identifier used for lookup
   * @return the endpoint, or Optional.empty() when the identifier is unregistered
   */
  public synchronized Optional<CoreEndpoint> lookup(int id) {
    return Optional.ofNullable(targets.get(id));
  }

  /**
   * Looks up the participant identifier using exact object identity.
   *
   * @param endpoint participant instance to register or inspect by object identity
   * @return the registered identifier, or an empty OptionalInt for an unregistered instance
   */
  public synchronized OptionalInt identityOf(CoreEndpoint endpoint) {
    Integer id = identities.get(endpoint);
    return id == null ? OptionalInt.empty() : OptionalInt.of(id);
  }

  /**
   * Checks the configured sender, target, and operation-code permissions.
   *
   * @param senderId registered identifier of the participant sending the message
   * @param targetId registered identifier of the participant that should receive the message
   * @param operationCode numeric identifier selecting the operation to execute
   * @return true when at least one route rule permits this operation
   */
  public synchronized boolean isAllowed(int senderId, int targetId, int operationCode) {
    return rules.stream()
        .anyMatch(
            rule ->
                rule.senderId() == senderId
                    && rule.targetId() == targetId
                    && rule.allowedOperationCodes().contains(operationCode));
  }

  /**
   * Checks whether the route permits access to the target's local UI callback.
   *
   * @param senderId registered identifier of the participant sending the message
   * @param targetId registered identifier of the participant that should receive the message
   * @return true when a matching rule enables UI-reference access
   */
  public synchronized boolean canReturnUiRef(int senderId, int targetId) {
    return rules.stream()
        .anyMatch(
            rule ->
                rule.senderId() == senderId
                    && rule.targetId() == targetId
                    && rule.allowUiReturnRef());
  }
}
