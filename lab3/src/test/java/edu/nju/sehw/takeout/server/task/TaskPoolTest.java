package edu.nju.sehw.takeout.server.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.nju.sehw.takeout.testsupport.Fixtures;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TaskPoolTest {
  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void capacityMustBePositive(int capacity) {
    assertThrows(IllegalArgumentException.class, () -> new TaskPool(capacity));
  }

  @Test
  void fullQueueRejectsTheAdditionalTask() throws InterruptedException {
    TaskPool pool = new TaskPool(1);
    VerifiedTask first = Fixtures.task(UUID.randomUUID());
    assertTrue(pool.offer(first));
    assertFalse(pool.offer(Fixtures.task(UUID.randomUUID())));
    assertEquals(1, pool.capacity());
    assertEquals(1, pool.size());
    assertSame(first, pool.take());
    assertEquals(0, pool.size());
  }

  @Test
  void taskOrderIsFifo() throws InterruptedException {
    TaskPool pool = new TaskPool(2);
    VerifiedTask first = Fixtures.task(UUID.randomUUID());
    VerifiedTask second = Fixtures.task(UUID.randomUUID());
    pool.offer(first);
    pool.offer(second);
    assertSame(first, pool.take());
    assertSame(second, pool.take());
  }

  @Test
  void slowProcessingDoesNotHoldTheQueueSlot() throws Exception {
    TaskPool pool = new TaskPool(1);
    pool.offer(Fixtures.task(UUID.randomUUID()));
    CountDownLatch obtained = new CountDownLatch(1);
    CountDownLatch finish = new CountDownLatch(1);
    ExecutorService threads = Executors.newSingleThreadExecutor();
    try {
      Future<VerifiedTask> worker =
          threads.submit(
              () -> {
                VerifiedTask task = pool.take();
                obtained.countDown();
                if (!finish.await(5, TimeUnit.SECONDS)) {
                  throw new IllegalStateException("test deadline");
                }
                return task;
              });
      assertTrue(obtained.await(5, TimeUnit.SECONDS));
      assertTrue(pool.offer(Fixtures.task(UUID.randomUUID())));
      finish.countDown();
      worker.get(5, TimeUnit.SECONDS);
    } finally {
      finish.countDown();
      threads.shutdownNow();
      assertTrue(threads.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  @Test
  void waitingForATaskCanBeInterrupted() throws Exception {
    TaskPool pool = new TaskPool(1);
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch exited = new CountDownLatch(1);
    ExecutorService threads = Executors.newSingleThreadExecutor();
    try {
      Future<VerifiedTask> waiting =
          threads.submit(
              () -> {
                entered.countDown();
                try {
                  return pool.take();
                } finally {
                  exited.countDown();
                }
              });
      assertTrue(entered.await(5, TimeUnit.SECONDS));
      assertTrue(waiting.cancel(true));
      assertTrue(exited.await(5, TimeUnit.SECONDS));
    } finally {
      threads.shutdownNow();
      assertTrue(threads.awaitTermination(5, TimeUnit.SECONDS));
    }
  }
}
