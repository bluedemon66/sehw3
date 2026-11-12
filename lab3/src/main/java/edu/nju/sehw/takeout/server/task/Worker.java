package edu.nju.sehw.takeout.server.task;

import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.scaffold.NotImplementedYetException;
import edu.nju.sehw.takeout.server.network.ResponseSender;
import edu.nju.sehw.takeout.server.operation.OperationExecutor;
import java.util.Objects;

/**
 * Consumes verified tasks, invokes the configured operation executor, and sends their responses.
 */
public final class Worker implements Runnable {
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

  /**
   * Consumes tasks, invokes the operation executor, and sends each resulting response.
   *
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  @Override
  public void run() {
    throw new NotImplementedYetException("Worker.run");
  }

  /**
   * Extracts the request message carried by a verified task.
   *
   * @param task authenticated request, reply channel, and optional staged upload to execute
   * @return the authenticated task's message envelope
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  private Message parseTask(VerifiedTask task) {
    throw new NotImplementedYetException("Worker.parseTask");
  }
}
