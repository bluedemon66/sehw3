package edu.nju.sehw.takeout.server.order;

import edu.nju.sehw.takeout.model.common.ResultStatus;
import edu.nju.sehw.takeout.model.order.CaseStatus;
import edu.nju.sehw.takeout.model.order.CommunicationCase;
import edu.nju.sehw.takeout.server.operation.OperationFailure;
import edu.nju.sehw.takeout.server.persistence.Jdbc;
import edu.nju.sehw.takeout.server.persistence.TransactionContext;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Stores communication cases created during order exception handling. */
public final class JdbcExceptionCaseRepository implements ExceptionCaseRepository {
  private final Clock clock;

  /** Uses the given clock for case timestamps. */
  public JdbcExceptionCaseRepository(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
  }

  @Override
  public CommunicationCase open(TransactionContext transaction, UUID orderId) {
    UUID id = UUID.randomUUID();
    var now = clock.instant();
    Jdbc.update(
        transaction,
        """
        INSERT INTO exception_cases (case_id, order_id, status, created_at) VALUES (?, ?, 'OPEN', ?)
        """,
        id,
        orderId,
        now.atOffset(ZoneOffset.UTC));
    return new CommunicationCase(
        id, orderId, CaseStatus.OPEN, now, Optional.empty(), Optional.empty());
  }

  @Override
  public Optional<CommunicationCase> find(TransactionContext transaction, UUID caseId) {
    return Jdbc.first(
        transaction,
        "SELECT * FROM exception_cases WHERE case_id=?",
        row ->
            new CommunicationCase(
                caseId,
                row.getObject("order_id", UUID.class),
                CaseStatus.valueOf(row.getString("status")),
                row.getObject("created_at", OffsetDateTime.class).toInstant(),
                Optional.ofNullable(row.getString("resolution")),
                Optional.ofNullable(row.getObject("resolved_at", OffsetDateTime.class))
                    .map(OffsetDateTime::toInstant)),
        caseId);
  }

  @Override
  public CommunicationCase resolve(TransactionContext transaction, UUID caseId, String resolution) {
    if (Jdbc.update(
            transaction,
            """
            UPDATE exception_cases SET status='RESOLVED', resolution=?, resolved_at=?
            WHERE case_id=? AND status='OPEN'
            """,
            resolution,
            clock.instant().atOffset(ZoneOffset.UTC),
            caseId)
        != 1) {
      boolean exists = find(transaction, caseId).isPresent();
      throw new OperationFailure(
          exists ? ResultStatus.CONFLICT : ResultStatus.NOT_FOUND,
          exists ? "Case already resolved" : "Case not found");
    }
    return find(transaction, caseId).orElseThrow();
  }
}
