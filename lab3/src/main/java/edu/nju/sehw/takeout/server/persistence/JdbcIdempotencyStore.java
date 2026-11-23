package edu.nju.sehw.takeout.server.persistence;

import edu.nju.sehw.takeout.model.common.UserId;
import edu.nju.sehw.takeout.model.message.BinaryMessageCodec;
import edu.nju.sehw.takeout.model.message.EnvelopeCodec;
import edu.nju.sehw.takeout.model.network.SecureResponse;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/** Adapts idempotency claim and completion records to the current JDBC transaction. */
public final class JdbcIdempotencyStore implements IdempotencyStore {

  /** Creates this component with the dependencies documented below. */
  public JdbcIdempotencyStore() {}

  private final EnvelopeCodec codec = new EnvelopeCodec(new BinaryMessageCodec());

  /**
   * Claims the actor and request identifier using the body digest in the current transaction.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param actor authenticated user whose ownership, purchase evidence, or idempotency record is
   *     being inspected
   * @param requestId identifier of this individual request; retries reuse the same identifier
   * @param digest digest identifying the request body for idempotent replay and mismatch detection
   * @return the claim disposition and, for a replay, the previously saved response
   */
  @Override
  public ClaimResult claim(
      TransactionContext transaction, UserId actor, UUID requestId, byte[] digest) {
    var connection = transaction.connection();
    try {
      var point = connection.setSavepoint();
      try (var insert =
          connection.prepareStatement(
              "INSERT INTO idempotency (actor_id, request_id, digest) VALUES (?, ?, ?)")) {
        insert.setObject(1, actor.value());
        insert.setObject(2, requestId);
        insert.setBytes(3, digest);
        insert.executeUpdate();
      } catch (SQLException ex) {
        if (!"23505".equals(ex.getSQLState())) {
          throw ex;
        }
        connection.rollback(point);
        return Jdbc.first(
                transaction,
                "SELECT digest, response FROM idempotency WHERE actor_id=? AND request_id=?",
                row -> {
                  if (!MessageDigest.isEqual(digest, row.getBytes(1))) {
                    return new ClaimResult(ClaimStatus.DIGEST_MISMATCH, Optional.empty());
                  }
                  byte[] response = row.getBytes(2);
                  return response == null
                      ? new ClaimResult(ClaimStatus.IN_PROGRESS, Optional.empty())
                      : new ClaimResult(
                          ClaimStatus.REPLAY, Optional.of(codec.decodeResponse(response)));
                },
                actor.value(),
                requestId)
            .orElseThrow();
      } finally {
        connection.releaseSavepoint(point);
      }
      return new ClaimResult(ClaimStatus.CLAIMED, Optional.empty());
    } catch (SQLException ex) {
      throw new IllegalStateException("Cannot claim request", ex);
    }
  }

  /**
   * Saves the final response for replay in the transaction that commits the business changes.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param actor authenticated user whose ownership, purchase evidence, or idempotency record is
   *     being inspected
   * @param requestId identifier of this individual request; retries reuse the same identifier
   * @param result remote request identifier, result status, and typed result payload
   */
  @Override
  public void complete(
      TransactionContext transaction, UserId actor, UUID requestId, SecureResponse result) {
    if (!requestId.equals(result.requestId())
        || Jdbc.update(
                transaction,
                """
                UPDATE idempotency SET response=?
                WHERE actor_id=? AND request_id=? AND response IS NULL
                """,
                codec.encodeResponse(result),
                actor.value(),
                requestId)
            != 1) {
      throw new IllegalStateException("No matching idempotency claim");
    }
  }
}
