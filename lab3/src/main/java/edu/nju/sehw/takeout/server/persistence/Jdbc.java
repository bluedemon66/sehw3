package edu.nju.sehw.takeout.server.persistence;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Executes parameterized JDBC statements within the supplied transaction. */
public final class Jdbc {
  private Jdbc() {}

  /** Converts a result row to a domain value. */
  @FunctionalInterface
  public interface Row<T> {
    /** Reads the current row. */
    T read(ResultSet row) throws SQLException;
  }

  /** Executes a statement and returns the number of changed rows. */
  public static int update(TransactionContext transaction, String sql, Object... arguments) {
    try (var statement = prepare(transaction, sql, arguments)) {
      return statement.executeUpdate();
    } catch (SQLException ex) {
      throw new IllegalStateException("Database update failed", ex);
    }
  }

  /** Returns all rows mapped in result order. */
  public static <T> List<T> query(
      TransactionContext transaction, String sql, Row<T> mapper, Object... arguments) {
    try (var statement = prepare(transaction, sql, arguments);
        var rows = statement.executeQuery()) {
      List<T> result = new ArrayList<>();
      while (rows.next()) {
        result.add(mapper.read(rows));
      }
      return result;
    } catch (SQLException ex) {
      throw new IllegalStateException("Database query failed", ex);
    }
  }

  /** Returns the first mapped row. Queries that can match several rows should use a limit. */
  public static <T> Optional<T> first(
      TransactionContext transaction, String sql, Row<T> mapper, Object... arguments) {
    try (var statement = prepare(transaction, sql, arguments);
        var rows = statement.executeQuery()) {
      return rows.next() ? Optional.of(mapper.read(rows)) : Optional.empty();
    } catch (SQLException ex) {
      throw new IllegalStateException("Database query failed", ex);
    }
  }

  private static PreparedStatement prepare(
      TransactionContext transaction, String sql, Object[] values) throws SQLException {
    var statement = transaction.connection().prepareStatement(sql);
    try {
      for (int i = 0; i < values.length; i++) {
        statement.setObject(i + 1, values[i]);
      }
      return statement;
    } catch (SQLException | RuntimeException ex) {
      try {
        statement.close();
      } catch (SQLException closing) {
        ex.addSuppressed(closing);
      }
      throw ex;
    }
  }
}
