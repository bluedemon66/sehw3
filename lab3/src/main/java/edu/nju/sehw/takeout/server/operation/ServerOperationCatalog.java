package edu.nju.sehw.takeout.server.operation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Indexes predefined server operations by unique positive operation code. */
public final class ServerOperationCatalog {
  /** Immutable lookup of prepared operation metadata by numeric operation identifier. */
  private final Map<Integer, PreparedOperation> operations;

  /**
   * Creates this component with the dependencies documented below.
   *
   * @param definitions operation metadata to index by unique positive numeric code
   * @throws NullPointerException if a required component, dependency, or collection element is null
   * @throws IllegalArgumentException if an operation code is nonpositive or duplicated
   */
  public ServerOperationCatalog(Collection<PreparedOperation> definitions) {
    Map<Integer, PreparedOperation> indexed = new HashMap<>();
    for (PreparedOperation operation : definitions) {
      Objects.requireNonNull(operation, "operation");
      if (operation.operationCode() <= 0
          || indexed.putIfAbsent(operation.operationCode(), operation) != null) {
        throw new IllegalArgumentException("操作编号须为唯一正整数");
      }
    }
    operations = Map.copyOf(indexed);
  }

  /**
   * Resolves prepared operation metadata by its numeric identifier.
   *
   * @param code numeric operation identifier to register or resolve
   * @return the operation definition, or Optional.empty() for an unknown code
   */
  public Optional<PreparedOperation> find(int code) {
    return Optional.ofNullable(operations.get(code));
  }
}
