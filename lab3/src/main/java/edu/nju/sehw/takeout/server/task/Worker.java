package edu.nju.sehw.takeout.server.task;

import edu.nju.sehw.takeout.model.common.ResultStatus;
import edu.nju.sehw.takeout.model.message.ErrorPayload;
import edu.nju.sehw.takeout.model.network.SecureResponse;
import edu.nju.sehw.takeout.server.network.ResponseSender;
import edu.nju.sehw.takeout.server.operation.OperationExecutor;
import java.io.IOException;
import java.util.Objects;

/**
 * Consumes verified tasks, invokes the configured operation executor, and sends their responses.
 */
public final class Worker implements Runnable {
  private static final System.Logger LOGGER = System.getLogger(Worker.class.getName());

  /** Bounded queue shared by the receiver and server workers. */
  private final TaskPool pool;

  /** Operation execution port invoked for each task removed from the queue. */
  private final OperationExecutor executor;

  /** Service that writes completed responses to their originating channels. */
  private final ResponseSender responseSender;

  /**
   * Creates this component with the dependencies documented below.
   *
   * @param pool bounded queue shared by the receiver and server workers
   * @param executor operation execution port invoked for each task removed from the queue
   * @param responseSender service that writes completed responses to their originating channels
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public Worker(TaskPool pool, OperationExecutor executor, ResponseSender responseSender) {
    this.pool = Objects.requireNonNull(pool, "pool");
    this.executor = Objects.requireNonNull(executor, "executor");
    this.responseSender = Objects.requireNonNull(responseSender, "responseSender");
  }

  /** Consumes tasks, invokes the operation executor, and sends each resulting response. */
  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      VerifiedTask task;
      try {
        task = pool.take();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        return;
      }
      try {
        SecureResponse response;
        try {
          response = parseTask(task);
        } catch (RuntimeException ex) {
          LOGGER.log(System.Logger.Level.ERROR, "Operation execution failed", ex);
          response =
              new SecureResponse(
                  task.context().requestId(),
                  ResultStatus.FAILED,
                  new ErrorPayload("FAILED", "Server operation failed"));
        }
        responseSender.reply(task.replyChannel(), response);
      } catch (IOException | RuntimeException ex) {
        LOGGER.log(System.Logger.Level.WARNING, "Could not deliver operation response", ex);
      } finally {
        task.upload().ifPresent(VerifiedUpload::discard);
      }
    }
  }

  /**
   * Extracts the request message carried by a verified task.
   *
   * @param task authenticated request, reply channel, and optional staged upload to execute
   * @return the authenticated task's message envelope
   */
  private SecureResponse parseTask(VerifiedTask task) {
    return executor.execute(task);
  }
}
