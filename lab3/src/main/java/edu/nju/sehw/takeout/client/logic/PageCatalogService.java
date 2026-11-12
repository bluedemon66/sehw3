package edu.nju.sehw.takeout.client.logic;

import edu.nju.sehw.takeout.client.core.Core;
import edu.nju.sehw.takeout.client.core.MessageParticipant;
import edu.nju.sehw.takeout.client.resource.LocalUiLibrary;
import edu.nju.sehw.takeout.client.security.ClientPermissionChecker;
import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.ui.PageResourceManifest;
import edu.nju.sehw.takeout.scaffold.NotImplementedYetException;
import java.util.Objects;

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
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  @Override
  public void parse(Message message) {
    throw new NotImplementedYetException("PageCatalogService.parse");
  }

  /**
   * Loads the layout-first resource manifest for a local page identifier.
   *
   * @param pageId identifier of the page in the client's local page catalogue
   * @return the page layout and required resource descriptors
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  private PageResourceManifest lookupPage(int pageId) {
    throw new NotImplementedYetException("PageCatalogService.lookupPage");
  }
}
