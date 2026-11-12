package edu.nju.sehw.takeout.server.task;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Provides a bounded FIFO handoff queue between the receiver and server workers.
 *
 * <p>Taking a task releases the queue slot before the worker begins operation processing.
 */
public final class TaskPool {
  /** Thread-safe FIFO handoff queue of verified tasks awaiting a worker. */
  private final BlockingQueue<VerifiedTask> queue;

  /** Positive maximum number of tasks waiting in the queue. */
  private final int capacity;

  /**
   * Creates this component with the dependencies documented below.
   *
   * @param capacity positive maximum number of tasks waiting in the queue
   * @throws IllegalArgumentException if capacity is nonpositive
   */
  public TaskPool(int capacity) {
    if (capacity <= 0) {
      throw new IllegalArgumentException("队列容量须为正整数");
    }
    this.capacity = capacity;
    queue = new ArrayBlockingQueue<>(capacity);
  }

  /**
   * Attempts to add a verified task to the bounded queue immediately.
   *
   * @param task authenticated request, reply channel, and optional staged upload to execute
   * @return true when the task is queued; false when the queue is full
   * @throws NullPointerException if task is null
   */
  public boolean offer(VerifiedTask task) {
    return queue.offer(Objects.requireNonNull(task, "task"));
  }

  /**
   * Waits interruptibly for the oldest queued task and removes it from the queue.
   *
   * @return the next verified task for processing outside the queue
   * @throws InterruptedException if the waiting thread is interrupted
   */
  public VerifiedTask take() throws InterruptedException {
    return queue.take();
  }

  /**
   * Returns the number of tasks currently waiting in the queue.
   *
   * @return the current queued-task count
   */
  public int size() {
    return queue.size();
  }

  /**
   * Returns the configured upper bound on queued tasks.
   *
   * @return the positive queue capacity
   */
  public int capacity() {
    return capacity;
  }
}
