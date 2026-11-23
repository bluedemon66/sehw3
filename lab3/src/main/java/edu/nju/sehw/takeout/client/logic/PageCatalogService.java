package edu.nju.sehw.takeout.client.logic;

import edu.nju.sehw.takeout.client.core.Core;
import edu.nju.sehw.takeout.client.core.MessageParticipant;
import edu.nju.sehw.takeout.client.resource.LocalUiLibrary;
import edu.nju.sehw.takeout.client.security.ClientPermissionChecker;
import edu.nju.sehw.takeout.model.common.TargetDescriptor;
import edu.nju.sehw.takeout.model.message.ErrorPayload;
import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.message.OperationCode;
import edu.nju.sehw.takeout.model.ui.PageManifestPayload;
import edu.nju.sehw.takeout.model.ui.PageRequestPayload;
import edu.nju.sehw.takeout.model.ui.PageResourceManifest;
import java.io.UncheckedIOException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

/** Looks up the local layout and resource manifest required by a requested page. */
public final class PageCatalogService extends MessageParticipant {
  /** Permission preflight or server authorization service used by this component. */
  private final ClientPermissionChecker checker;

  /** Local page catalogue used to obtain layout and resource manifests. */
  private final LocalUiLibrary library;

  /**
   * Creates this participant with the dependencies documented below.
   *
   * @param participantId positive identifier assigned to this participant in its local Core
   * @param core local Core that owns this participant and routes its messages
   * @param checker permission preflight or server authorization service used by this component
   * @param library local page catalogue used to obtain layout and resource manifests
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public PageCatalogService(
      int participantId, Core core, ClientPermissionChecker checker, LocalUiLibrary library) {
    super(participantId, core);
    this.checker = Objects.requireNonNull(checker, "checker");
    this.library = Objects.requireNonNull(library, "library");
  }

  /**
   * Processes a page-manifest request and routes its manifest result back through Core.
   *
   * @param message message envelope containing request identity, routing fields, operation code,
   *     and arguments
   */
  @Override
  public void parse(Message message) {
    if (!(message.payload() instanceof PageRequestPayload request)) {
      reply(message, new ErrorPayload("INVALID_REQUEST", "Expected page ID"));
      return;
    }
    try {
      reply(message, new PageManifestPayload(lookupPage(request.pageId())));
    } catch (SecurityException ex) {
      reply(message, new ErrorPayload("FORBIDDEN", ex.getMessage()));
    } catch (NoSuchElementException ex) {
      reply(message, new ErrorPayload("NOT_FOUND", ex.getMessage()));
    } catch (IllegalArgumentException | UncheckedIOException ex) {
      reply(message, new ErrorPayload("PAGE_FAILED", ex.getMessage()));
    }
  }

  /**
   * Loads the layout-first resource manifest for a local page identifier.
   *
   * @param pageId identifier of the page in the client's local page catalogue
   * @return the page layout and required resource descriptors
   */
  private PageResourceManifest lookupPage(int pageId) {
    String kind =
        switch (pageId) {
          case 1003 -> "turnover";
          case 1004 -> "comment";
          case 1005 -> "rider";
          default -> "page";
        };
    var access =
        checker.check(
            OperationCode.OPEN_PAGE.code(), new TargetDescriptor(kind, new UUID(0, pageId)));
    if (!access.allowed()) {
      throw new SecurityException(access.description());
    }
    return library
        .findPage(pageId)
        .orElseThrow(() -> new NoSuchElementException("Page not found: " + pageId));
  }
}
