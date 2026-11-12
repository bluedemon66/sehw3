package edu.nju.sehw.takeout.server.order;

import edu.nju.sehw.takeout.model.order.CommunicationCase;
import edu.nju.sehw.takeout.server.persistence.TransactionContext;
import java.util.Optional;
import java.util.UUID;

/**
 * Defines communication-case lookup and writes in the same transaction as related order updates.
 */
public interface ExceptionCaseRepository {
  /**
   * Creates a specialist communication case for the selected exception order.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param orderId stable identifier of the order associated with this command, snapshot, or review
   *     target
   * @return the newly opened case associated with the order
   */
  CommunicationCase open(TransactionContext transaction, UUID orderId);

  /**
   * Reads the specialist communication case by its identifier.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param caseId stable identifier of the specialist communication case
   * @return the case, or Optional.empty() when it is absent
   */
  Optional<CommunicationCase> find(TransactionContext transaction, UUID caseId);

  /**
   * Records the communication outcome and closes the case while preserving the order's exception
   * marker.
   *
   * @param transaction active transaction context owned by the enclosing TransactionRunner
   * @param caseId stable identifier of the specialist communication case
   * @param resolution recorded outcome of specialist communication for the exception order
   * @return the case with its recorded resolution and resolved state
   */
  CommunicationCase resolve(TransactionContext transaction, UUID caseId, String resolution);
}
