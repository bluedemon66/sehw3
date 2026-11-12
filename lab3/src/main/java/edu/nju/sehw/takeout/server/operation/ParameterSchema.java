package edu.nju.sehw.takeout.server.operation;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Describes operation fields, their expected value types, and the required field names.
 *
 * @param fields parameter names mapped to their expected scalar value types
 * @param requiredFields names that must be present and must also be defined in the field-type map
 */
public record ParameterSchema(Map<String, ValueType> fields, Set<String> requiredFields) {
  /**
   * Creates the ParameterSchema value from the components documented below.
   *
   * @param fields parameter names mapped to their expected scalar value types
   * @param requiredFields names that must be present and must also be defined in the field-type map
   * @throws NullPointerException if a required component, dependency, or collection element is null
   * @throws IllegalArgumentException if a required field has no type definition
   */
  public ParameterSchema {
    Objects.requireNonNull(fields, "fields");
    fields = Map.copyOf(fields);
    Objects.requireNonNull(requiredFields, "requiredFields");
    requiredFields = Set.copyOf(requiredFields);
    if (!fields.keySet().containsAll(requiredFields)) {
      throw new IllegalArgumentException("必填项须在字段目录中定义");
    }
  }
}
