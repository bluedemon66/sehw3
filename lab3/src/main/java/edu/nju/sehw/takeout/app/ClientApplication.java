package edu.nju.sehw.takeout.app;

import edu.nju.sehw.takeout.client.comment.CommentService;
import edu.nju.sehw.takeout.client.core.ClientParticipants;
import edu.nju.sehw.takeout.client.core.Core;
import edu.nju.sehw.takeout.client.core.RouteRule;
import edu.nju.sehw.takeout.client.logic.CrudOperationService;
import edu.nju.sehw.takeout.client.logic.PageCatalogService;
import edu.nju.sehw.takeout.client.logic.ResourceAcquisitionService;
import edu.nju.sehw.takeout.client.processing.EventProcessor;
import edu.nju.sehw.takeout.client.processing.PageProcessor;
import edu.nju.sehw.takeout.client.query.ClientSession;
import edu.nju.sehw.takeout.client.query.DatabaseGateway;
import edu.nju.sehw.takeout.client.query.DefaultRequestSecurity;
import edu.nju.sehw.takeout.client.query.HttpsTransport;
import edu.nju.sehw.takeout.client.query.ResourceGetTool;
import edu.nju.sehw.takeout.client.query.ResourceUploadTool;
import edu.nju.sehw.takeout.client.resource.LocalResourceStore;
import edu.nju.sehw.takeout.client.resource.LocalUiLibrary;
import edu.nju.sehw.takeout.client.security.SessionPermissionChecker;
import edu.nju.sehw.takeout.client.ui.UiInteractionController;
import edu.nju.sehw.takeout.client.ui.UiResourceForest;
import edu.nju.sehw.takeout.client.ui.UiThreadDispatcher;
import edu.nju.sehw.takeout.model.message.MessageCodec;
import edu.nju.sehw.takeout.model.message.MessagePayload;
import edu.nju.sehw.takeout.model.message.OperationCode;
import edu.nju.sehw.takeout.model.ui.UiPage;
import java.time.Clock;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Assembles the local participants and the operation routes for a client session. */
public final class ClientApplication {
  private ClientApplication() {}

  /** Creates a connected UI controller using the supplied adapters. */
  public static UiInteractionController connect(
      ClientSession session,
      MessageCodec codec,
      Clock clock,
      HttpsTransport transport,
      LocalUiLibrary library,
      LocalResourceStore resources,
      UiThreadDispatcher dispatcher,
      Consumer<UiPage> renderer,
      Consumer<MessagePayload> results) {
    var core = new Core();
    var security = new DefaultRequestSecurity(session, codec, clock);
    var checker = new SessionPermissionChecker(session);
    var ui =
        new UiInteractionController(
            ClientParticipants.UI, core, dispatcher, new UiResourceForest(), renderer, results);
    core.register(ClientParticipants.UI, ui);
    core.register(ClientParticipants.EVENTS, new EventProcessor(ClientParticipants.EVENTS, core));
    core.register(ClientParticipants.PAGES, new PageProcessor(ClientParticipants.PAGES, core));
    core.register(
        ClientParticipants.CATALOG,
        new PageCatalogService(ClientParticipants.CATALOG, core, checker, library));
    core.register(
        ClientParticipants.RESOURCES,
        new ResourceAcquisitionService(ClientParticipants.RESOURCES, core, checker, resources));
    core.register(
        ClientParticipants.CRUD, new CrudOperationService(ClientParticipants.CRUD, core, checker));
    core.register(
        ClientParticipants.COMMENTS,
        new CommentService(ClientParticipants.COMMENTS, core, checker));
    core.register(
        ClientParticipants.GATEWAY,
        new DatabaseGateway(ClientParticipants.GATEWAY, core, security, transport));
    core.register(
        ClientParticipants.DOWNLOAD,
        new ResourceGetTool(ClientParticipants.DOWNLOAD, core, security, transport, resources));
    core.register(
        ClientParticipants.UPLOAD,
        new ResourceUploadTool(ClientParticipants.UPLOAD, core, security, transport));

    Set<Integer> all =
        Arrays.stream(OperationCode.values())
            .map(OperationCode::code)
            .collect(Collectors.toUnmodifiableSet());
    Set<Integer> pageQueries =
        codes(
            OperationCode.ORDER_DETAIL,
            OperationCode.MERCHANT_TURNOVER,
            OperationCode.RIDER_TASKS,
            OperationCode.COMMENT_LIST);
    final Set<Integer> comments =
        codes(
            OperationCode.COMMENT_LIST,
            OperationCode.COMMENT_PUBLISH,
            OperationCode.COMMENT_WITHDRAW,
            OperationCode.COMMENT_REPLY,
            OperationCode.COMMENT_HIDE);
    final Set<Integer> crud =
        codes(
            OperationCode.PLACE_ORDER,
            OperationCode.CANCEL_ORDER,
            OperationCode.CONFIRM_RECEIPT,
            OperationCode.REPORT_NOT_RECEIVED,
            OperationCode.START_PREPARATION,
            OperationCode.RIDER_PICKUP,
            OperationCode.CONFIRM_DELIVERY,
            OperationCode.ACCEPT_ORDER,
            OperationCode.REJECT_ORDER,
            OperationCode.FINISH_PREPARATION,
            OperationCode.RESOLVE_CASE,
            OperationCode.ORDER_DETAIL,
            OperationCode.MERCHANT_TURNOVER,
            OperationCode.RIDER_TASKS);
    Set<Integer> open = codes(OperationCode.OPEN_PAGE);

    core.allow(new RouteRule(ClientParticipants.UI, ClientParticipants.EVENTS, all, false));
    core.allow(new RouteRule(ClientParticipants.EVENTS, ClientParticipants.UI, all, false));
    core.allow(
        new RouteRule(
            ClientParticipants.EVENTS,
            ClientParticipants.PAGES,
            codes(
                OperationCode.OPEN_PAGE,
                OperationCode.ORDER_DETAIL,
                OperationCode.MERCHANT_TURNOVER,
                OperationCode.RIDER_TASKS,
                OperationCode.COMMENT_LIST),
            false));
    core.allow(new RouteRule(ClientParticipants.PAGES, ClientParticipants.UI, pageQueries, true));
    core.allow(new RouteRule(ClientParticipants.PAGES, ClientParticipants.UI, open, true));
    pair(core, ClientParticipants.PAGES, ClientParticipants.CATALOG, open);
    pair(core, ClientParticipants.PAGES, ClientParticipants.RESOURCES, open);
    pair(
        core,
        ClientParticipants.PAGES,
        ClientParticipants.CRUD,
        pageQueries.stream().filter(crud::contains).collect(Collectors.toUnmodifiableSet()));
    pair(
        core,
        ClientParticipants.PAGES,
        ClientParticipants.COMMENTS,
        codes(OperationCode.COMMENT_LIST));
    pair(
        core,
        ClientParticipants.RESOURCES,
        ClientParticipants.DOWNLOAD,
        codes(OperationCode.GET_RESOURCE));
    pair(
        core,
        ClientParticipants.COMMENTS,
        ClientParticipants.UPLOAD,
        codes(OperationCode.UPLOAD_ATTACHMENT));
    pair(core, ClientParticipants.CRUD, ClientParticipants.GATEWAY, crud);
    pair(core, ClientParticipants.COMMENTS, ClientParticipants.GATEWAY, comments);
    core.allow(new RouteRule(ClientParticipants.EVENTS, ClientParticipants.CRUD, crud, false));
    core.allow(
        new RouteRule(ClientParticipants.EVENTS, ClientParticipants.COMMENTS, comments, false));
    core.allow(new RouteRule(ClientParticipants.CRUD, ClientParticipants.UI, crud, false));
    core.allow(new RouteRule(ClientParticipants.COMMENTS, ClientParticipants.UI, comments, false));
    return ui;
  }

  private static Set<Integer> codes(OperationCode... operations) {
    return Arrays.stream(operations)
        .map(OperationCode::code)
        .collect(Collectors.toUnmodifiableSet());
  }

  private static void pair(Core core, int left, int right, Set<Integer> operations) {
    core.allow(new RouteRule(left, right, operations, false));
    core.allow(new RouteRule(right, left, operations, false));
  }
}
