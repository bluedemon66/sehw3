package edu.nju.sehw.takeout.server.operation;

import edu.nju.sehw.takeout.model.common.ResultStatus;
import java.util.Objects;

/** Rejects a domain operation with a status visible to its caller. */
public final class OperationFailure extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final ResultStatus status;

  /** Creates a failure that will cause the current operation to roll back. */
  public OperationFailure(ResultStatus status, String description) {
    super(description);
    this.status = Objects.requireNonNull(status);
  }

  /** Returns the status reported to the caller. */
  public ResultStatus status() {
    return status;
  }
}
