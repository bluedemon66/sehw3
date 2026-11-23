package edu.nju.sehw.takeout.client.comment;

import edu.nju.sehw.takeout.client.core.ClientParticipants;
import edu.nju.sehw.takeout.client.core.Core;
import edu.nju.sehw.takeout.client.core.MessageParticipant;
import edu.nju.sehw.takeout.client.security.ClientPermissionChecker;
import edu.nju.sehw.takeout.model.comment.CommentDraftPayload;
import edu.nju.sehw.takeout.model.comment.HideCommentPayload;
import edu.nju.sehw.takeout.model.comment.ListCommentPayload;
import edu.nju.sehw.takeout.model.comment.PublishCommentPayload;
import edu.nju.sehw.takeout.model.comment.ReplyCommentPayload;
import edu.nju.sehw.takeout.model.comment.WithdrawCommentPayload;
import edu.nju.sehw.takeout.model.common.ResultStatus;
import edu.nju.sehw.takeout.model.common.TargetDescriptor;
import edu.nju.sehw.takeout.model.message.ErrorPayload;
import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.message.MessagePayload;
import edu.nju.sehw.takeout.model.message.OperationCode;
import edu.nju.sehw.takeout.model.message.OperationReplyPayload;
import edu.nju.sehw.takeout.model.resource.LocalResourceRef;
import edu.nju.sehw.takeout.model.resource.ResourceRefPayload;
import edu.nju.sehw.takeout.model.resource.UploadResourcePayload;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Routes comment operations, uploading each draft image before sending publication. */
public final class CommentService extends MessageParticipant {
  private final ClientPermissionChecker checker;
  private final Map<UUID, Message> pending = new ConcurrentHashMap<>();
  private final Map<UUID, Draft> uploads = new ConcurrentHashMap<>();

  /** Creates a comment service with local permission checks. */
  public CommentService(int participantId, Core core, ClientPermissionChecker checker) {
    super(participantId, core);
    this.checker = Objects.requireNonNull(checker);
  }

  @Override
  public void parse(Message message) {
    if (message.senderId() == ClientParticipants.GATEWAY) {
      var request = pending.remove(message.requestId());
      if (request != null) {
        finish(
            request,
            message.correlationId().equals(request.correlationId())
                    && message.payload() instanceof OperationReplyPayload
                ? message.payload()
                : new ErrorPayload("INVALID_REPLY", "Unexpected comment reply"));
      }
    } else if (message.senderId() == ClientParticipants.UPLOAD) {
      completeUpload(message);
    } else {
      handle(message);
    }
  }

  private void handle(Message message) {
    MessagePayload payload = message.payload();
    try {
      var target = targetOf(payload);
      var access = checker.check(message.operationCode(), target);
      if (!access.allowed()) {
        finish(message, new ErrorPayload(access.code(), access.description()));
        return;
      }
      if (payload instanceof CommentDraftPayload draft && !draft.files().isEmpty()) {
        int uploadCode = OperationCode.UPLOAD_ATTACHMENT.code();
        var uploadAccess =
            checker.check(uploadCode, new TargetDescriptor("resource", target.targetId()));
        if (!uploadAccess.allowed()) {
          finish(message, new ErrorPayload(uploadAccess.code(), uploadAccess.description()));
        } else if (draft.files().size() + draft.comment().attachmentIds().size() > 9) {
          finish(
              message,
              new ErrorPayload("INVALID_REQUEST", "A comment can include up to nine images"));
        } else {
          uploadAttachments(
              new Draft(
                  message,
                  draft.comment(),
                  draft.files().iterator(),
                  new ArrayList<>(draft.comment().attachmentIds())));
        }
      } else {
        submitOperation(
            message, payload instanceof CommentDraftPayload draft ? draft.comment() : payload);
      }
    } catch (IllegalArgumentException ex) {
      finish(message, new ErrorPayload("INVALID_REQUEST", ex.getMessage()));
    }
  }

  private void uploadAttachments(Draft draft) {
    if (!draft.files().hasNext()) {
      submitOperation(
          draft.request(),
          new PublishCommentPayload(
              draft.comment().subject(), draft.comment().text(), draft.ids()));
      return;
    }
    UUID child = UUID.randomUUID();
    uploads.put(child, draft);
    var request = draft.request();
    var sent =
        dispatch(
            prepareMessage(
                child,
                request.correlationId(),
                ClientParticipants.UPLOAD,
                OperationCode.UPLOAD_ATTACHMENT.code(),
                new UploadResourcePayload(draft.files().next())));
    if (!sent.accepted() && uploads.remove(child) != null) {
      finish(request, new ErrorPayload(sent.errorCode().name(), sent.description()));
    }
  }

  private void completeUpload(Message message) {
    Draft draft = uploads.remove(message.requestId());
    if (draft == null) {
      return;
    }
    var payload = message.payload();
    if (draft.request().correlationId().equals(message.correlationId())
        && payload instanceof OperationReplyPayload result
        && result.status() == ResultStatus.SUCCESS
        && result.payload() instanceof ResourceRefPayload ref) {
      draft.ids().add(ref.reference().resourceId());
      uploadAttachments(draft);
    } else {
      finish(
          draft.request(),
          payload instanceof OperationReplyPayload || payload instanceof ErrorPayload
              ? payload
              : new ErrorPayload("UPLOAD_FAILED", "Attachment upload did not complete"));
    }
  }

  private void submitOperation(Message request, MessagePayload payload) {
    if (pending.putIfAbsent(request.requestId(), request) != null) {
      return;
    }
    var sent =
        dispatch(
            prepareMessage(
                request.requestId(),
                request.correlationId(),
                ClientParticipants.GATEWAY,
                request.operationCode(),
                payload));
    if (!sent.accepted() && pending.remove(request.requestId(), request)) {
      finish(request, new ErrorPayload(sent.errorCode().name(), sent.description()));
    }
  }

  private void finish(Message request, MessagePayload payload) {
    int target =
        request.senderId() == ClientParticipants.EVENTS
            ? ClientParticipants.UI
            : request.senderId();
    dispatch(
        prepareMessage(
            request.requestId(),
            request.correlationId(),
            target,
            request.operationCode(),
            payload));
  }

  private static TargetDescriptor targetOf(MessagePayload payload) {
    if (payload instanceof CommentDraftPayload draft) {
      return targetOf(draft.comment());
    }
    if (payload instanceof ListCommentPayload list) {
      return new TargetDescriptor(list.subject().kind().name(), list.subject().targetId());
    }
    if (payload instanceof PublishCommentPayload publish) {
      return new TargetDescriptor(publish.subject().kind().name(), publish.subject().targetId());
    }
    if (payload instanceof WithdrawCommentPayload withdraw) {
      return new TargetDescriptor("comment", withdraw.commentId());
    }
    if (payload instanceof ReplyCommentPayload reply) {
      return new TargetDescriptor("comment", reply.commentId());
    }
    if (payload instanceof HideCommentPayload hide) {
      return new TargetDescriptor("comment", hide.commentId());
    }
    throw new IllegalArgumentException("Expected comment parameters");
  }

  private record Draft(
      Message request,
      PublishCommentPayload comment,
      Iterator<LocalResourceRef> files,
      List<String> ids) {}
}
