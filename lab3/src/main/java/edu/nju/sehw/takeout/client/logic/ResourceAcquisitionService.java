package edu.nju.sehw.takeout.client.logic;

import edu.nju.sehw.takeout.client.core.ClientParticipants;
import edu.nju.sehw.takeout.client.core.Core;
import edu.nju.sehw.takeout.client.core.MessageParticipant;
import edu.nju.sehw.takeout.client.resource.LocalResourceStore;
import edu.nju.sehw.takeout.client.security.ClientPermissionChecker;
import edu.nju.sehw.takeout.model.common.TargetDescriptor;
import edu.nju.sehw.takeout.model.message.ErrorPayload;
import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.message.OperationCode;
import edu.nju.sehw.takeout.model.resource.LocalResourceRef;
import edu.nju.sehw.takeout.model.resource.ResourceBundle;
import edu.nju.sehw.takeout.model.resource.ResourceBundlePayload;
import edu.nju.sehw.takeout.model.resource.ResourceDescriptor;
import edu.nju.sehw.takeout.model.resource.ResourceRequestPayload;
import edu.nju.sehw.takeout.model.ui.PageManifestPayload;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Resolves page resources locally and downloads missing remote assets. */
public final class ResourceAcquisitionService extends MessageParticipant {
  private final ClientPermissionChecker checker;
  private final LocalResourceStore store;
  private final Map<UUID, Download> waiting = new ConcurrentHashMap<>();

  /** Creates the service for the given local store and permission checker. */
  public ResourceAcquisitionService(
      int id, Core core, ClientPermissionChecker checker, LocalResourceStore store) {
    super(id, core);
    this.checker = Objects.requireNonNull(checker);
    this.store = Objects.requireNonNull(store);
  }

  @Override
  public void parse(Message message) {
    var payload = message.payload();
    if (message.senderId() == ClientParticipants.DOWNLOAD) {
      finishDownload(message);
      return;
    }
    Iterator<ResourceDescriptor> entries;
    if (payload instanceof PageManifestPayload manifest) {
      entries = manifest.manifest().entries().iterator();
    } else if (payload instanceof ResourceRequestPayload resource) {
      entries = java.util.List.of(resource.descriptor()).iterator();
    } else {
      reply(message, new ErrorPayload("INVALID_REQUEST", "Expected resource descriptors"));
      return;
    }
    collect(new Acquisition(message, entries, new HashMap<>()));
  }

  private void collect(Acquisition acquisition) {
    var original = acquisition.message();
    try {
      while (acquisition.entries().hasNext()) {
        var descriptor = acquisition.entries().next();
        var local = store.find(descriptor);
        if (local.isPresent()) {
          acquisition.resources().put(descriptor.elementId(), local.orElseThrow());
          continue;
        }
        String remote =
            descriptor
                .remoteResourceId()
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "Resource missing for element " + descriptor.elementId()));
        int code = OperationCode.GET_RESOURCE.code();
        var check = checker.check(code, new TargetDescriptor("resource", UUID.fromString(remote)));
        if (!check.allowed()) {
          reply(original, new ErrorPayload(check.code(), check.description()));
          return;
        }
        UUID child = UUID.randomUUID();
        waiting.put(child, new Download(acquisition, descriptor.elementId()));
        var result =
            dispatch(
                prepareMessage(
                    child,
                    original.correlationId(),
                    ClientParticipants.DOWNLOAD,
                    code,
                    new ResourceRequestPayload(descriptor)));
        if (!result.accepted() && waiting.remove(child) != null) {
          reply(original, new ErrorPayload(result.errorCode().name(), result.description()));
        }
        return;
      }
      reply(
          original,
          new ResourceBundlePayload(
              new ResourceBundle(original.requestId(), acquisition.resources())));
    } catch (IllegalArgumentException | java.io.UncheckedIOException ex) {
      reply(original, new ErrorPayload("RESOURCE_FAILED", ex.getMessage()));
    }
  }

  private void finishDownload(Message message) {
    var download = waiting.remove(message.requestId());
    if (download == null) {
      return;
    }
    var acquisition = download.acquisition();
    if (!acquisition.message().correlationId().equals(message.correlationId())) {
      reply(
          acquisition.message(),
          new ErrorPayload("INVALID_REPLY", "Resource correlation mismatch"));
    } else if (message.payload() instanceof ResourceBundlePayload bundle
        && bundle.resources().localResources().containsKey(download.element())) {
      acquisition
          .resources()
          .put(download.element(), bundle.resources().localResources().get(download.element()));
      collect(acquisition);
    } else if (message.payload() instanceof ErrorPayload error) {
      reply(acquisition.message(), error);
    } else {
      reply(
          acquisition.message(), new ErrorPayload("INVALID_REPLY", "Missing downloaded resource"));
    }
  }

  private record Acquisition(
      Message message,
      Iterator<ResourceDescriptor> entries,
      Map<Integer, LocalResourceRef> resources) {}

  private record Download(Acquisition acquisition, int element) {}
}
