package edu.nju.sehw.takeout.client.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.nju.sehw.takeout.model.message.EmptyPayload;
import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.message.OperationCode;
import edu.nju.sehw.takeout.model.ui.UiDataPort;
import edu.nju.sehw.takeout.model.ui.UiPage;
import edu.nju.sehw.takeout.testsupport.Fixtures;
import edu.nju.sehw.takeout.testsupport.Fixtures.Probe;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CoreRoutingTest {
  private Core core;
  private Probe sender;
  private Probe target;

  @BeforeEach
  void setUp() {
    core = new Core();
    sender = new Probe(1, core);
    target = new Probe(2, core);
    core.register(1, sender);
    core.register(2, target);
  }

  private void allow() {
    core.allow(new RouteRule(1, 2, Set.of(OperationCode.OPEN_PAGE.code()), false));
  }

  @Test
  void permittedRouteDeliversIndependentSnapshotWithSameRequestId() {
    allow();
    Message message = Fixtures.message(UUID.randomUUID(), 1, 2);
    DispatchResult result = sender.send(1, 2, core, message);
    assertTrue(result.accepted());
    assertEquals(message, target.received().get(0));
    assertNotSame(message, target.received().get(0));
  }

  @Test
  void defaultRoutePolicyDeniesDelivery() {
    assertEquals(
        DispatchError.FORBIDDEN_ROUTE,
        sender.send(1, 2, core, Fixtures.message(UUID.randomUUID(), 1, 2)).errorCode());
    assertTrue(target.received().isEmpty());
  }

  @Test
  void anUnregisteredObjectCannotImpersonateARegisteredSender() {
    allow();
    Probe impostor = new Probe(1, core);
    assertEquals(
        DispatchError.UNREGISTERED_SENDER,
        impostor.send(1, 2, core, Fixtures.message(UUID.randomUUID(), 1, 2)).errorCode());
    assertTrue(target.received().isEmpty());
  }

  @Test
  void senderCannotChangeItsOwnIdentity() {
    allow();
    assertEquals(
        DispatchError.INVALID_ENVELOPE,
        sender.send(2, 2, core, Fixtures.message(UUID.randomUUID(), 2, 2)).errorCode());
  }

  @Test
  void envelopeTargetMustMatchTheCall() {
    allow();
    assertEquals(
        DispatchError.INVALID_ENVELOPE,
        sender.send(1, 2, core, Fixtures.message(UUID.randomUUID(), 1, 3)).errorCode());
  }

  @Test
  void routingPermissionIncludesOperationCode() {
    allow();
    Message message =
        new Message(UUID.randomUUID(), 1, 2, OperationCode.COMMENT_HIDE.code(), new EmptyPayload());
    assertFalse(sender.send(1, 2, core, message).accepted());
    assertTrue(target.received().isEmpty());
  }

  @Test
  void aDifferentCoreIsRejected() {
    assertEquals(
        DispatchError.WRONG_CORE,
        sender.send(1, 2, new Core(), Fixtures.message(UUID.randomUUID(), 1, 2)).errorCode());
  }

  @Test
  void anUnknownTargetReturnsAnExplicitRoutingFailure() {
    assertEquals(
        DispatchError.UNKNOWN_TARGET,
        sender.send(1, 9, core, Fixtures.message(UUID.randomUUID(), 1, 9)).errorCode());
  }

  @Test
  void registrationPreservesUniqueIdsAndInstanceIdentity() {
    assertThrows(IllegalArgumentException.class, () -> core.register(1, new Probe(1, core)));
    assertThrows(IllegalArgumentException.class, () -> core.register(3, sender));
    assertThrows(IllegalArgumentException.class, () -> core.register(3, new Probe(3, new Core())));
  }

  @Test
  void nullEnvelopeProducesNoDelivery() {
    assertEquals(DispatchError.INVALID_ENVELOPE, core.dispatch(sender, 1, 2, null).errorCode());
    assertTrue(target.received().isEmpty());
  }

  @Test
  void uiReferenceRequiresSeparatePermission() {
    UiProbe ui = new UiProbe(3, core);
    core.register(3, ui);
    core.allow(new RouteRule(1, 3, Set.of(OperationCode.OPEN_PAGE.code()), false));
    assertThrows(SecurityException.class, () -> core.getTargetRef(sender, 3));
    core.allow(new RouteRule(1, 3, Set.of(), true));
    assertSame(ui, core.getTargetRef(sender, 3));
    assertThrows(SecurityException.class, () -> core.getTargetRef(new Probe(1, core), 3));
  }

  @Test
  void uiReferenceExposesTheUiPortContract() {
    core.allow(new RouteRule(1, 2, Set.of(), true));
    assertThrows(IllegalArgumentException.class, () -> core.getTargetRef(sender, 2));
  }

  @Test
  void handlerFailureIsReportedAsFailure() {
    Probe broken =
        new Probe(3, core) {
          @Override
          public void parse(Message message) {
            throw new IllegalStateException("broken");
          }
        };
    core.register(3, broken);
    core.allow(new RouteRule(1, 3, Set.of(OperationCode.OPEN_PAGE.code()), false));
    assertEquals(
        DispatchError.TARGET_FAILED,
        sender.send(1, 3, core, Fixtures.message(UUID.randomUUID(), 1, 3)).errorCode());
  }

  private static final class UiProbe extends Probe implements UiDataPort {
    private UiProbe(int id, Core core) {
      super(id, core);
    }

    @Override
    public void receivePage(UiPage page) {}
  }
}
