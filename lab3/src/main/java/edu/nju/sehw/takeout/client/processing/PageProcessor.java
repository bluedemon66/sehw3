package edu.nju.sehw.takeout.client.processing;

import edu.nju.sehw.takeout.client.core.ClientParticipants;
import edu.nju.sehw.takeout.client.core.Core;
import edu.nju.sehw.takeout.client.core.MessageParticipant;
import edu.nju.sehw.takeout.model.comment.CommentPagePayload;
import edu.nju.sehw.takeout.model.common.ResultStatus;
import edu.nju.sehw.takeout.model.message.EmptyPayload;
import edu.nju.sehw.takeout.model.message.ErrorPayload;
import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.message.MessagePayload;
import edu.nju.sehw.takeout.model.message.OperationCode;
import edu.nju.sehw.takeout.model.message.OperationReplyPayload;
import edu.nju.sehw.takeout.model.resource.ResourceBundle;
import edu.nju.sehw.takeout.model.resource.ResourceBundlePayload;
import edu.nju.sehw.takeout.model.resource.ResourceDescriptor;
import edu.nju.sehw.takeout.model.resource.ResourceKind;
import edu.nju.sehw.takeout.model.ui.PageActionPayload;
import edu.nju.sehw.takeout.model.ui.PageManifestPayload;
import edu.nju.sehw.takeout.model.ui.PageRequestPayload;
import edu.nju.sehw.takeout.model.ui.PageResourceManifest;
import edu.nju.sehw.takeout.model.ui.TextValue;
import edu.nju.sehw.takeout.model.ui.UiPage;
import edu.nju.sehw.takeout.model.ui.UiResourceNode;
import edu.nju.sehw.takeout.model.ui.UiValue;
import edu.nju.sehw.takeout.model.ui.UiValuesPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Collects the display data and resources for each page before returning it to the UI. */
public final class PageProcessor extends MessageParticipant {
  private final Map<UUID, Stage> pending = new ConcurrentHashMap<>();

  /** Creates a page processor registered through the supplied Core. */
  public PageProcessor(int participantId, Core core) {
    super(participantId, core);
  }

  @Override
  public void parse(Message message) {
    if (message.senderId() == ClientParticipants.EVENTS) {
      var payload = message.payload();
      if (payload instanceof PageRequestPayload request) {
        begin(message, new PageActionPayload(request.pageId(), 0, new EmptyPayload()));
      } else if (payload instanceof PageActionPayload action) {
        begin(message, action);
      } else {
        fail(message, new ErrorPayload("INVALID_REQUEST", "Expected page action"));
      }
      return;
    }
    Stage stage = pending.remove(message.requestId());
    if (stage == null) {
      return;
    }
    var progress = stage.progress();
    if (message.senderId() != stage.sender()
        || !message.correlationId().equals(progress.request.correlationId())) {
      fail(
          progress.request,
          new ErrorPayload("INVALID_REPLY", "Page reply does not match its request"));
      return;
    }
    try {
      advance(progress, message.payload());
    } catch (IllegalArgumentException | IllegalStateException ex) {
      fail(progress.request, new ErrorPayload("PAGE_FAILED", ex.getMessage()));
    }
  }

  private void begin(Message request, PageActionPayload action) {
    var progress = new Progress(request, action);
    send(
        progress,
        ClientParticipants.CATALOG,
        OperationCode.OPEN_PAGE.code(),
        new PageRequestPayload(action.pageId()));
  }

  private void advance(Progress progress, MessagePayload payload) {
    if (payload instanceof ErrorPayload) {
      fail(progress.request, payload);
    } else if (payload instanceof PageManifestPayload manifest) {
      progress.manifest = manifest.manifest();
      int operation = progress.action.operationCode();
      if (operation == 0) {
        acquire(progress);
      } else {
        int target =
            operation == OperationCode.COMMENT_LIST.code()
                ? ClientParticipants.COMMENTS
                : ClientParticipants.CRUD;
        send(progress, target, operation, progress.action.arguments());
      }
    } else if (payload instanceof OperationReplyPayload result) {
      if (result.status() != ResultStatus.SUCCESS) {
        fail(progress.request, result);
        return;
      }
      populate(progress, result.payload());
      acquire(progress);
    } else if (payload instanceof ResourceBundlePayload bundle) {
      core()
          .getTargetRef(this, ClientParticipants.UI)
          .receivePage(assemblePage(progress, bundle.resources()));
    } else {
      fail(progress.request, new ErrorPayload("INVALID_REPLY", "Unexpected page result"));
    }
  }

  private void populate(Progress progress, MessagePayload payload) {
    if (payload instanceof UiValuesPayload fields) {
      progress.values.putAll(fields.values());
      return;
    }
    if (!(payload instanceof CommentPagePayload comments)) {
      throw new IllegalArgumentException("Expected page data");
    }
    var entries = new ArrayList<>(progress.manifest.entries());
    int element =
        Math.max(100, 1 + entries.stream().mapToInt(ResourceDescriptor::elementId).max().orElse(0));
    if (comments.page().items().isEmpty()) {
      progress.values.put(element, new TextValue("No comments"));
    }
    for (var comment : comments.page().items()) {
      progress.values.put(
          element++,
          new TextValue(
              comment.commentId()
                  + " | "
                  + comment.authorLabel()
                  + " ["
                  + comment.purchaseMark()
                  + "] "
                  + comment.text()));
      for (var image : comment.images()) {
        entries.add(
            new ResourceDescriptor(
                element++, Optional.empty(), Optional.of(image.resourceId()), ResourceKind.IMAGE));
      }
      for (var reply : comment.replies()) {
        progress.values.put(element++, new TextValue(reply.merchantLabel() + " " + reply.text()));
      }
    }
    comments
        .page()
        .nextCursor()
        .ifPresent(cursor -> progress.values.put(19, new TextValue(cursor)));
    progress.manifest = new PageResourceManifest(entries);
  }

  private void acquire(Progress progress) {
    send(
        progress,
        ClientParticipants.RESOURCES,
        OperationCode.OPEN_PAGE.code(),
        new PageManifestPayload(progress.manifest));
  }

  private void send(Progress progress, int target, int operation, MessagePayload payload) {
    UUID child = UUID.randomUUID();
    pending.put(child, new Stage(progress, target));
    var sent =
        dispatch(
            prepareMessage(child, progress.request.correlationId(), target, operation, payload));
    if (!sent.accepted() && pending.remove(child) != null) {
      fail(progress.request, new ErrorPayload(sent.errorCode().name(), sent.description()));
    }
  }

  private void fail(Message request, MessagePayload error) {
    dispatch(
        prepareMessage(
            request.requestId(),
            request.correlationId(),
            ClientParticipants.UI,
            request.operationCode(),
            error));
  }

  private UiPage assemblePage(Progress progress, ResourceBundle resources) {
    int layout = progress.manifest.uiLayout().elementId();
    if (!resources.localResources().containsKey(layout)) {
      throw new IllegalStateException("Page layout is missing");
    }
    var elements = new LinkedHashSet<Integer>();
    progress.manifest.entries().forEach(entry -> elements.add(entry.elementId()));
    progress.values.keySet().stream().sorted().forEach(elements::add);
    elements.remove(layout);
    var children =
        elements.stream()
            .map(
                element ->
                    new UiResourceNode(
                        element,
                        Optional.ofNullable(resources.localResources().get(element)),
                        Optional.ofNullable(progress.values.get(element)),
                        List.of()))
            .toList();
    var root =
        new UiResourceNode(
            layout,
            Optional.of(resources.localResources().get(layout)),
            Optional.ofNullable(progress.values.get(layout)),
            children);
    return new UiPage(progress.action.pageId(), progress.request.correlationId(), root);
  }

  private record Stage(Progress progress, int sender) {}

  private static final class Progress {
    private final Message request;
    private final PageActionPayload action;
    private final Map<Integer, UiValue> values = new HashMap<>();
    private PageResourceManifest manifest;

    private Progress(Message request, PageActionPayload action) {
      this.request = request;
      this.action = action;
    }
  }
}
