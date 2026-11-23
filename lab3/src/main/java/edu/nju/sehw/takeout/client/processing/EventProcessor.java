package edu.nju.sehw.takeout.client.processing;

import edu.nju.sehw.takeout.client.core.ClientParticipants;
import edu.nju.sehw.takeout.client.core.Core;
import edu.nju.sehw.takeout.client.core.MessageParticipant;
import edu.nju.sehw.takeout.model.comment.CommentSort;
import edu.nju.sehw.takeout.model.comment.CommentSubject;
import edu.nju.sehw.takeout.model.comment.ListCommentPayload;
import edu.nju.sehw.takeout.model.comment.SubjectKind;
import edu.nju.sehw.takeout.model.message.EmptyPayload;
import edu.nju.sehw.takeout.model.message.ErrorPayload;
import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.message.MessagePayload;
import edu.nju.sehw.takeout.model.message.OperationCode;
import edu.nju.sehw.takeout.model.message.TargetPayload;
import edu.nju.sehw.takeout.model.order.OrderCommandPayload;
import edu.nju.sehw.takeout.model.ui.DecimalValue;
import edu.nju.sehw.takeout.model.ui.PageActionPayload;
import edu.nju.sehw.takeout.model.ui.PageRequestPayload;
import edu.nju.sehw.takeout.model.ui.TextValue;
import edu.nju.sehw.takeout.model.ui.UiInput;
import edu.nju.sehw.takeout.model.ui.UiInputPayload;
import java.util.Optional;
import java.util.UUID;

/** Translates UI actions into requests for page, order, and comment processing. */
public final class EventProcessor extends MessageParticipant {
  /** Registers the processor with the client Core. */
  public EventProcessor(int participantId, Core core) {
    super(participantId, core);
  }

  @Override
  public void parse(Message message) {
    try {
      if (message.payload() instanceof UiInputPayload input) {
        handleInput(message, input.input());
      } else {
        handleAction(message, OperationCode.fromCode(message.operationCode()), message.payload());
      }
    } catch (IllegalArgumentException | ArithmeticException ex) {
      reply(message, new ErrorPayload("INVALID_INPUT", ex.getMessage()));
    }
  }

  private void handleInput(Message message, UiInput input) {
    int action = input.actionCode();
    if (action == OperationCode.OPEN_PAGE.code()) {
      openPage(message, input.elementId());
      return;
    }
    if (action >= 1001 && action <= 1005) {
      openPage(message, action);
      return;
    }
    var operation = OperationCode.fromCode(action);
    MessagePayload arguments =
        switch (operation) {
          case CANCEL_ORDER,
              CONFIRM_RECEIPT,
              REPORT_NOT_RECEIVED,
              START_PREPARATION,
              RIDER_PICKUP,
              CONFIRM_DELIVERY,
              ACCEPT_ORDER,
              REJECT_ORDER,
              FINISH_PREPARATION ->
              new OrderCommandPayload(UUID.fromString(text(input, 1)), version(input));
          case ORDER_DETAIL, MERCHANT_TURNOVER ->
              new TargetPayload(UUID.fromString(text(input, 1)));
          case RIDER_TASKS -> new EmptyPayload();
          case COMMENT_LIST ->
              new ListCommentPayload(
                  new CommentSubject(
                      SubjectKind.valueOf(text(input, 1)),
                      UUID.fromString(text(input, 2)),
                      Optional.empty(),
                      Optional.empty()),
                  CommentSort.valueOf(text(input, 3)),
                  Optional.ofNullable(input.values().values().get(4))
                      .filter(TextValue.class::isInstance)
                      .map(TextValue.class::cast)
                      .map(TextValue::value)
                      .filter(value -> !value.isEmpty()),
                  10);
          default -> throw new IllegalArgumentException("Use a typed payload for this action");
        };
    handleAction(message, operation, arguments);
  }

  private void openPage(Message message, int pageId) {
    if (pageId <= 0) {
      throw new IllegalArgumentException("Page identifier must be positive");
    }
    handleAction(message, OperationCode.OPEN_PAGE, new PageRequestPayload(pageId));
  }

  private void handleAction(Message request, OperationCode operation, MessagePayload payload) {
    int target;
    MessagePayload arguments = payload;
    switch (operation) {
      case OPEN_PAGE -> target = ClientParticipants.PAGES;
      case ORDER_DETAIL, MERCHANT_TURNOVER, COMMENT_LIST, RIDER_TASKS -> {
        target = ClientParticipants.PAGES;
        int page =
            switch (operation) {
              case ORDER_DETAIL -> 1002;
              case MERCHANT_TURNOVER -> 1003;
              case COMMENT_LIST -> 1004;
              default -> 1005;
            };
        arguments = new PageActionPayload(page, operation.code(), payload);
      }
      case COMMENT_PUBLISH, COMMENT_WITHDRAW, COMMENT_REPLY, COMMENT_HIDE ->
          target = ClientParticipants.COMMENTS;
      case UI_INPUT, GET_RESOURCE, UPLOAD_ATTACHMENT ->
          throw new IllegalArgumentException("Unsupported UI action");
      default -> target = ClientParticipants.CRUD;
    }
    var result =
        dispatch(
            prepareMessage(
                request.requestId(), request.correlationId(), target, operation.code(), arguments));
    if (!result.accepted()) {
      reply(request, new ErrorPayload(result.errorCode().name(), result.description()));
    }
  }

  private static long version(UiInput input) {
    if (input.values().values().get(2) instanceof DecimalValue version) {
      return version.value().longValueExact();
    }
    throw new IllegalArgumentException("Missing order version");
  }

  private static String text(UiInput input, int key) {
    if (input.values().values().get(key) instanceof TextValue text) {
      return text.value();
    }
    throw new IllegalArgumentException("Missing text input " + key);
  }
}
