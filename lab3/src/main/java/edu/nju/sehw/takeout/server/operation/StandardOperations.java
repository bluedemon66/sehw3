package edu.nju.sehw.takeout.server.operation;

import edu.nju.sehw.takeout.model.comment.HideCommentPayload;
import edu.nju.sehw.takeout.model.comment.ListCommentPayload;
import edu.nju.sehw.takeout.model.comment.PublishCommentPayload;
import edu.nju.sehw.takeout.model.comment.ReplyCommentPayload;
import edu.nju.sehw.takeout.model.comment.WithdrawCommentPayload;
import edu.nju.sehw.takeout.model.common.Permission;
import edu.nju.sehw.takeout.model.common.TargetDescriptor;
import edu.nju.sehw.takeout.model.message.EmptyPayload;
import edu.nju.sehw.takeout.model.message.MessagePayload;
import edu.nju.sehw.takeout.model.message.OperationCode;
import edu.nju.sehw.takeout.model.message.TargetPayload;
import edu.nju.sehw.takeout.model.order.OrderCommandPayload;
import edu.nju.sehw.takeout.model.order.PlaceOrderPayload;
import edu.nju.sehw.takeout.model.order.ResolveCasePayload;
import edu.nju.sehw.takeout.model.resource.AttachmentUploadPayload;
import edu.nju.sehw.takeout.model.resource.ResourceRequestPayload;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Defines built-in remote operations and maps their typed payloads to schema fields. */
public final class StandardOperations {
  private static final UUID NO_TARGET = new UUID(0, 0);

  private StandardOperations() {}

  /** Creates the default catalog for the server. */
  public static ServerOperationCatalog catalog() {
    return new ServerOperationCatalog(
        Arrays.stream(OperationCode.values())
            .filter(code -> code != OperationCode.OPEN_PAGE && code != OperationCode.UI_INPUT)
            .map(StandardOperations::definition)
            .toList());
  }

  private static PreparedOperation definition(OperationCode operation) {
    Permission permission =
        switch (operation) {
          case PLACE_ORDER, CONFIRM_RECEIPT, REPORT_NOT_RECEIVED -> Permission.PLACE_ORDER;
          case CANCEL_ORDER -> Permission.CANCEL_OWN_ORDER;
          case ACCEPT_ORDER,
              REJECT_ORDER,
              START_PREPARATION,
              FINISH_PREPARATION,
              MERCHANT_TURNOVER ->
              Permission.MANAGE_SHOP;
          case RIDER_PICKUP, CONFIRM_DELIVERY, RIDER_TASKS -> Permission.DELIVER_ORDER;
          case RESOLVE_CASE -> Permission.RESOLVE_CASE;
          case ORDER_DETAIL -> Permission.READ_ORDER;
          case COMMENT_LIST -> Permission.READ_COMMENT;
          case COMMENT_PUBLISH -> Permission.PUBLISH_COMMENT;
          case COMMENT_WITHDRAW -> Permission.WITHDRAW_OWN_COMMENT;
          case COMMENT_REPLY -> Permission.REPLY_SHOP_COMMENT;
          case COMMENT_HIDE -> Permission.HIDE_COMMENT;
          case GET_RESOURCE -> Permission.READ_RESOURCE;
          case UPLOAD_ATTACHMENT -> Permission.UPLOAD_RESOURCE;
          default -> throw new IllegalArgumentException("Internal operation");
        };
    Map<String, ValueType> fields =
        switch (operation) {
          case COMMENT_LIST ->
              Map.of(
                  "targetId",
                  ValueType.UUID,
                  "subjectKind",
                  ValueType.STRING,
                  "sort",
                  ValueType.STRING,
                  "cursor",
                  ValueType.STRING,
                  "limit",
                  ValueType.INTEGER);
          case COMMENT_PUBLISH ->
              Map.of(
                  "targetId",
                  ValueType.UUID,
                  "subjectKind",
                  ValueType.STRING,
                  "text",
                  ValueType.STRING);
          case COMMENT_REPLY, COMMENT_HIDE, RESOLVE_CASE ->
              Map.of("targetId", ValueType.UUID, "text", ValueType.STRING);
          case RIDER_TASKS -> Map.of();
          case UPLOAD_ATTACHMENT ->
              Map.of("mediaType", ValueType.STRING, "length", ValueType.INTEGER);
          case CANCEL_ORDER,
              ACCEPT_ORDER,
              REJECT_ORDER,
              START_PREPARATION,
              FINISH_PREPARATION,
              RIDER_PICKUP,
              CONFIRM_DELIVERY,
              CONFIRM_RECEIPT,
              REPORT_NOT_RECEIVED ->
              Map.of("targetId", ValueType.UUID, "version", ValueType.INTEGER);
          default -> Map.of("targetId", ValueType.UUID);
        };
    boolean read =
        switch (operation) {
          case COMMENT_LIST, ORDER_DETAIL, MERCHANT_TURNOVER, RIDER_TASKS, GET_RESOURCE -> true;
          default -> false;
        };
    Set<String> required =
        operation == OperationCode.COMMENT_LIST
            ? Set.of("targetId", "subjectKind", "sort", "limit")
            : fields.keySet();
    return new PreparedOperation(
        operation.code(),
        permission,
        new ParameterSchema(fields, required),
        read ? TransactionPolicy.READ_ONLY : TransactionPolicy.ATOMIC_WRITE);
  }

  static TargetDescriptor validate(PreparedOperation definition, MessagePayload payload) {
    OperationCode operation = OperationCode.fromCode(definition.operationCode());
    Map<String, Object> fields = new HashMap<>();
    String kind =
        switch (operation) {
          case COMMENT_LIST -> {
            var query = require(payload, ListCommentPayload.class);
            fields.put("targetId", query.subject().targetId());
            fields.put("subjectKind", query.subject().kind().name());
            fields.put("sort", query.sort().name());
            fields.put("limit", query.limit());
            query.cursor().ifPresent(cursor -> fields.put("cursor", cursor));
            yield query.subject().kind().name();
          }
          case COMMENT_PUBLISH -> {
            var comment = require(payload, PublishCommentPayload.class);
            fields.put("targetId", comment.subject().targetId());
            fields.put("subjectKind", comment.subject().kind().name());
            fields.put("text", comment.text());
            yield comment.subject().kind().name();
          }
          case COMMENT_WITHDRAW -> {
            fields.put("targetId", require(payload, WithdrawCommentPayload.class).commentId());
            yield "comment";
          }
          case COMMENT_REPLY -> {
            var comment = require(payload, ReplyCommentPayload.class);
            fields.put("targetId", comment.commentId());
            fields.put("text", comment.text());
            yield "comment";
          }
          case COMMENT_HIDE -> {
            var comment = require(payload, HideCommentPayload.class);
            fields.put("targetId", comment.commentId());
            fields.put("text", comment.reason());
            yield "comment";
          }
          case PLACE_ORDER -> {
            fields.put("targetId", require(payload, PlaceOrderPayload.class).shopId());
            yield "shop";
          }
          case RESOLVE_CASE -> {
            var command = require(payload, ResolveCasePayload.class);
            fields.put("targetId", command.caseId());
            fields.put("text", command.resolution());
            yield "case";
          }
          case MERCHANT_TURNOVER, ORDER_DETAIL -> {
            fields.put("targetId", require(payload, TargetPayload.class).targetId());
            yield operation == OperationCode.ORDER_DETAIL ? "order" : "shop";
          }
          case RIDER_TASKS -> {
            require(payload, EmptyPayload.class);
            yield "rider";
          }
          case GET_RESOURCE -> {
            var descriptor = require(payload, ResourceRequestPayload.class).descriptor();
            fields.put(
                "targetId",
                UUID.fromString(
                    descriptor
                        .remoteResourceId()
                        .orElseThrow(
                            () -> new IllegalArgumentException("Missing remote resource ID"))));
            yield "resource";
          }
          case UPLOAD_ATTACHMENT -> {
            var upload = require(payload, AttachmentUploadPayload.class);
            fields.put("mediaType", upload.mediaType());
            fields.put("length", upload.contentLength());
            yield "resource";
          }
          case CANCEL_ORDER,
              ACCEPT_ORDER,
              REJECT_ORDER,
              START_PREPARATION,
              FINISH_PREPARATION,
              RIDER_PICKUP,
              CONFIRM_DELIVERY,
              CONFIRM_RECEIPT,
              REPORT_NOT_RECEIVED -> {
            var command = require(payload, OrderCommandPayload.class);
            if (command.expectedVersion() < 0) {
              throw new IllegalArgumentException("Order version must be nonnegative");
            }
            fields.put("targetId", command.orderId());
            fields.put("version", command.expectedVersion());
            yield "order";
          }
          default -> throw new IllegalArgumentException("Internal operation");
        };

    var schema = definition.parameterSchema();
    if (!fields.keySet().containsAll(schema.requiredFields())) {
      throw new IllegalArgumentException("Missing required operation parameters");
    }
    schema
        .fields()
        .forEach(
            (key, type) -> {
              Object value = fields.get(key);
              if (value != null && !validType(type, value)) {
                throw new IllegalArgumentException("Invalid operation field " + key);
              }
            });
    return new TargetDescriptor(kind, (UUID) fields.getOrDefault("targetId", NO_TARGET));
  }

  private static boolean validType(ValueType type, Object value) {
    return switch (type) {
      case STRING -> value instanceof String;
      case INTEGER -> value instanceof Integer || value instanceof Long;
      case DECIMAL -> value instanceof java.math.BigDecimal;
      case BOOLEAN -> value instanceof Boolean;
      case UUID -> value instanceof UUID;
    };
  }

  private static <T extends MessagePayload> T require(MessagePayload payload, Class<T> kind) {
    if (!kind.isInstance(payload)) {
      throw new IllegalArgumentException("Expected " + kind.getSimpleName());
    }
    return kind.cast(payload);
  }
}
