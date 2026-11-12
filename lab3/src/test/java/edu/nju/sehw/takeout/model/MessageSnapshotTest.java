package edu.nju.sehw.takeout.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.nju.sehw.takeout.client.core.Core;
import edu.nju.sehw.takeout.model.message.IntPayload;
import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.message.MessagePayload;
import edu.nju.sehw.takeout.testsupport.Fixtures.Probe;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class MessageSnapshotTest {
  @Test
  void integerPayloadCopiesTheOriginalList() {
    List<Integer> values = new ArrayList<>(List.of(1, 2));
    IntPayload payload = new IntPayload(values);
    values.set(0, 9);
    assertEquals(List.of(1, 2), payload.values());
    assertThrows(UnsupportedOperationException.class, () -> payload.values().add(3));
    assertNotSame(payload, payload.deepCopy());
  }

  @Test
  void mutableExtensionPayloadCannotMutateAnExistingMessage() {
    MutablePayload original = new MutablePayload(new ArrayList<>(List.of(7)));
    Message message = new Message(UUID.randomUUID(), 1, 2, 2101, original);
    original.values.add(8);
    MutablePayload returned = (MutablePayload) message.payload();
    returned.values.add(9);
    assertEquals(List.of(7), ((MutablePayload) message.payload()).values);
    assertNotSame(message, message.deepCopy());
  }

  @Test
  void preparingAnotherDraftKeepsTheFirstSnapshot() {
    Probe probe = new Probe(1, new Core());
    UUID requestId = UUID.randomUUID();
    Message first = probe.snapshot(requestId, 2, new IntPayload(List.of(1)));
    Message second = probe.snapshot(requestId, 2, new IntPayload(List.of(2)));
    assertNotSame(first, second);
    assertEquals(List.of(1), ((IntPayload) first.payload()).values());
    assertEquals(List.of(2), ((IntPayload) second.payload()).values());
    assertEquals(requestId, first.requestId());
    assertEquals(requestId, second.requestId());
  }

  @Test
  void concurrentDraftsKeepTheirOwnRequestAndArguments() throws Exception {
    Probe probe = new Probe(1, new Core());
    ExecutorService threads = Executors.newFixedThreadPool(4);
    try {
      List<Future<Message>> futures = new ArrayList<>();
      List<UUID> ids = new ArrayList<>();
      for (int i = 0; i < 80; i++) {
        int value = i;
        UUID requestId = UUID.randomUUID();
        ids.add(requestId);
        futures.add(
            threads.submit(() -> probe.snapshot(requestId, 2, new IntPayload(List.of(value)))));
      }
      Set<UUID> returned = new HashSet<>();
      for (int i = 0; i < futures.size(); i++) {
        Message message = futures.get(i).get(5, TimeUnit.SECONDS);
        assertEquals(ids.get(i), message.requestId());
        assertEquals(List.of(i), ((IntPayload) message.payload()).values());
        returned.add(message.requestId());
      }
      assertEquals(80, returned.size());
    } finally {
      threads.shutdownNow();
      threads.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void snapshotsPreserveBothChildRequestAndParentCorrelation() {
    UUID requestId = UUID.randomUUID();
    UUID rootId = UUID.randomUUID();
    Message message = new Message(requestId, rootId, 1, 2, 2101, new IntPayload(List.of(7)));
    Message snapshot = message.deepCopy();
    assertEquals(requestId, snapshot.requestId());
    assertEquals(rootId, snapshot.correlationId());
    assertNotSame(message, snapshot);
  }

  private static final class MutablePayload implements MessagePayload {
    private final List<Integer> values;

    private MutablePayload(List<Integer> values) {
      this.values = values;
    }

    @Override
    public MutablePayload deepCopy() {
      return new MutablePayload(new ArrayList<>(values));
    }
  }
}
