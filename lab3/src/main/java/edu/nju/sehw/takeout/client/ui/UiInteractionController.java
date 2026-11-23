package edu.nju.sehw.takeout.client.ui;

import edu.nju.sehw.takeout.client.core.ClientParticipants;
import edu.nju.sehw.takeout.client.core.Core;
import edu.nju.sehw.takeout.client.core.MessageParticipant;
import edu.nju.sehw.takeout.model.message.ErrorPayload;
import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.message.MessagePayload;
import edu.nju.sehw.takeout.model.message.OperationCode;
import edu.nju.sehw.takeout.model.message.OperationReplyPayload;
import edu.nju.sehw.takeout.model.ui.UiDataPort;
import edu.nju.sehw.takeout.model.ui.UiInput;
import edu.nju.sehw.takeout.model.ui.UiInputPayload;
import edu.nju.sehw.takeout.model.ui.UiPage;
import edu.nju.sehw.takeout.model.ui.UiPagePayload;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Owns the active UI state, accepts UI input, and receives assembled pages from application logic.
 */
public final class UiInteractionController extends MessageParticipant implements UiDataPort {
  /** Adapter that schedules rendering work on the UI toolkit's thread. */
  private final UiThreadDispatcher dispatcher;

  /** Controller-owned collection of page resource trees and element bindings. */
  private final UiResourceForest resourceForest;

  /** Identifier of the page in the client's local page catalogue. */
  private int pageId;

  /** Optional root UI request identifier used to match an arriving page to the active operation. */
  private Optional<UUID> activeRequestId = Optional.empty();

  /** Local resource references currently bound to the active page's element identifiers. */
  private final Consumer<UiPage> renderer;

  private final Consumer<MessagePayload> results;
  private UUID displayedRequestId;

  /**
   * Creates this participant with the dependencies documented below.
   *
   * @param participantId positive identifier assigned to this participant in its local Core
   * @param core local Core that owns this participant and routes its messages
   * @param dispatcher adapter that schedules rendering work on the UI toolkit's thread
   * @param resourceForest controller-owned collection of page resource trees and element bindings
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public UiInteractionController(
      int participantId,
      Core core,
      UiThreadDispatcher dispatcher,
      UiResourceForest resourceForest) {
    this(participantId, core, dispatcher, resourceForest, System.out::println, System.out::println);
  }

  /** Creates a controller with callbacks for assembled pages and operation results. */
  public UiInteractionController(
      int participantId,
      Core core,
      UiThreadDispatcher dispatcher,
      UiResourceForest resourceForest,
      Consumer<UiPage> renderer,
      Consumer<MessagePayload> results) {
    super(participantId, core);
    this.renderer = Objects.requireNonNull(renderer);
    this.results = Objects.requireNonNull(results);
    this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
    this.resourceForest = Objects.requireNonNull(resourceForest, "resourceForest");
  }

  /**
   * Accepts display-related messages and selects the controller's page or state update handling.
   *
   * @param message message envelope containing request identity, routing fields, operation code,
   *     and arguments
   */
  @Override
  public void parse(Message message) {
    var payload = message.payload();
    if (payload instanceof UiPagePayload page) {
      receivePage(page.page());
      return;
    }
    if (!(payload instanceof OperationReplyPayload || payload instanceof ErrorPayload)) {
      return;
    }
    dispatcher.dispatch(
        () -> {
          synchronized (this) {
            if (activeRequestId.filter(message.correlationId()::equals).isPresent()) {
              activeRequestId = Optional.empty();
              results.accept(payload);
            }
          }
        });
  }

  /** Renders the selected page from the controller's stored resource tree and element values. */
  public synchronized void render() {
    resourceForest
        .findPage(pageId)
        .ifPresent(
            root -> {
              if (displayedRequestId != null) {
                renderer.accept(new UiPage(pageId, displayedRequestId, root));
              }
            });
  }

  /**
   * Turns the UI interaction into a correlated message for the event-processing participant.
   *
   * @param input page, element, action, and typed input values produced by the UI
   */
  public void receiveUiInput(UiInput input) {
    submit(OperationCode.UI_INPUT.code(), new UiInputPayload(input));
  }

  /**
   * Receives an assembled page so the controller can match its active request and install its
   * state.
   *
   * @param page assembled page associated with the UI request being completed
   */
  @Override
  public void receivePage(UiPage page) {
    dispatcher.dispatch(
        () -> {
          synchronized (this) {
            if (activeRequestId.filter(page.requestId()::equals).isEmpty()) {
              return;
            }
            activeRequestId = Optional.empty();
            pageId = page.pageId();
            displayedRequestId = page.requestId();
            resourceForest.install(page);
            render();
          }
        });
  }

  /** Sends a typed UI action and returns its correlation identifier. */
  public UUID submit(int operation, MessagePayload payload) {
    UUID id = UUID.randomUUID();
    synchronized (this) {
      activeRequestId = Optional.of(id);
    }
    var result = dispatch(prepareMessage(id, ClientParticipants.EVENTS, operation, payload));
    if (!result.accepted()) {
      parse(
          new Message(
              id,
              ClientParticipants.EVENTS,
              participantId(),
              operation,
              new ErrorPayload(result.errorCode().name(), result.description())));
    }
    return id;
  }
}
