package edu.nju.sehw.takeout.app;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.nju.sehw.takeout.client.query.ClientSession;
import edu.nju.sehw.takeout.client.query.DefaultRequestSecurity;
import edu.nju.sehw.takeout.client.query.HttpsTransport;
import edu.nju.sehw.takeout.client.query.JdkHttpsTransport;
import edu.nju.sehw.takeout.client.resource.FileLocalResourceStore;
import edu.nju.sehw.takeout.client.resource.FileUiLibrary;
import edu.nju.sehw.takeout.model.comment.CommentDraftPayload;
import edu.nju.sehw.takeout.model.comment.CommentPagePayload;
import edu.nju.sehw.takeout.model.comment.CommentSort;
import edu.nju.sehw.takeout.model.comment.CommentSubject;
import edu.nju.sehw.takeout.model.comment.CommentViewPayload;
import edu.nju.sehw.takeout.model.comment.HideCommentPayload;
import edu.nju.sehw.takeout.model.comment.ListCommentPayload;
import edu.nju.sehw.takeout.model.comment.PublishCommentPayload;
import edu.nju.sehw.takeout.model.comment.PurchaseMark;
import edu.nju.sehw.takeout.model.comment.ReplyCommentPayload;
import edu.nju.sehw.takeout.model.comment.SubjectKind;
import edu.nju.sehw.takeout.model.comment.WithdrawCommentPayload;
import edu.nju.sehw.takeout.model.common.Permission;
import edu.nju.sehw.takeout.model.common.PermissionSet;
import edu.nju.sehw.takeout.model.common.ResultStatus;
import edu.nju.sehw.takeout.model.message.BinaryMessageCodec;
import edu.nju.sehw.takeout.model.message.EmptyPayload;
import edu.nju.sehw.takeout.model.message.ErrorPayload;
import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.message.MessagePayload;
import edu.nju.sehw.takeout.model.message.OperationCode;
import edu.nju.sehw.takeout.model.message.OperationReplyPayload;
import edu.nju.sehw.takeout.model.message.TargetPayload;
import edu.nju.sehw.takeout.model.network.AccessToken;
import edu.nju.sehw.takeout.model.network.SecureRequest;
import edu.nju.sehw.takeout.model.network.SecureResponse;
import edu.nju.sehw.takeout.model.order.OrderCommandPayload;
import edu.nju.sehw.takeout.model.order.PlaceOrderPayload;
import edu.nju.sehw.takeout.model.order.ResolveCasePayload;
import edu.nju.sehw.takeout.model.resource.LocalResourceRef;
import edu.nju.sehw.takeout.model.resource.RemoteResourceRef;
import edu.nju.sehw.takeout.model.resource.ResourceBlob;
import edu.nju.sehw.takeout.model.resource.ResourceDescriptor;
import edu.nju.sehw.takeout.model.resource.ResourceKind;
import edu.nju.sehw.takeout.model.resource.ResourceRequestPayload;
import edu.nju.sehw.takeout.model.ui.DecimalValue;
import edu.nju.sehw.takeout.model.ui.InputValues;
import edu.nju.sehw.takeout.model.ui.TextValue;
import edu.nju.sehw.takeout.model.ui.UiInput;
import edu.nju.sehw.takeout.model.ui.UiPage;
import edu.nju.sehw.takeout.model.ui.UiValuesPayload;
import edu.nju.sehw.takeout.server.network.ResponseSender;
import edu.nju.sehw.takeout.server.network.ServerReceiver;
import edu.nju.sehw.takeout.server.persistence.Jdbc;
import edu.nju.sehw.takeout.server.persistence.JdbcDatabaseAdapter;
import edu.nju.sehw.takeout.server.security.ServerRequestContext;
import edu.nju.sehw.takeout.server.security.SessionPrincipal;
import edu.nju.sehw.takeout.server.task.TaskPool;
import edu.nju.sehw.takeout.server.task.Worker;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TakeoutIntegrationTest {
  @TempDir static Path tlsDirectory;
  @TempDir Path working;
  private static final String TEST_PASSWORD = "test-fixture-only";
  private static Path keyStore;
  private static final Path ASSETS = Path.of("src/main/resources/ui");
  private static final Clock CLOCK = Clock.systemUTC();
  private static final BinaryMessageCodec CODEC = new BinaryMessageCodec();
  private static final CommentSubject SHOP =
      new CommentSubject(SubjectKind.SHOP, DemoData.SHOP, Optional.empty(), Optional.empty());

  private final ClientSession customer =
      session(
          DemoData.Role.CUSTOMER,
          Permission.PLACE_ORDER,
          Permission.CANCEL_OWN_ORDER,
          Permission.PUBLISH_COMMENT,
          Permission.WITHDRAW_OWN_COMMENT,
          Permission.UPLOAD_RESOURCE);
  private final ClientSession merchant =
      session(
          DemoData.Role.MERCHANT,
          Permission.MANAGE_SHOP,
          Permission.REPLY_SHOP_COMMENT,
          Permission.PUBLISH_COMMENT);
  private final ClientSession rider = session(DemoData.Role.RIDER, Permission.DELIVER_ORDER);
  private final ClientSession moderator =
      session(DemoData.Role.MODERATOR, Permission.HIDE_COMMENT, Permission.RESOLVE_CASE);
  private final ClientSession outsider =
      sessionFor(
          new edu.nju.sehw.takeout.model.common.UserId(UUID.randomUUID()),
          Permission.MANAGE_SHOP,
          Permission.REPLY_SHOP_COMMENT,
          Permission.WITHDRAW_OWN_COMMENT,
          Permission.PUBLISH_COMMENT);

  private ServerApplication server;
  private JdkHttpsTransport transport;
  private FileLocalResourceStore local;
  private JdbcDatabaseAdapter db;

  @BeforeAll
  static void generateLoopbackCertificate() throws Exception {
    keyStore = tlsDirectory.resolve("localhost.p12");
    String exe = System.getProperty("os.name").startsWith("Windows") ? "keytool.exe" : "keytool";
    Path keytool = Path.of(System.getProperty("java.home"), "bin", exe);
    Path log = tlsDirectory.resolve("keytool.log");
    Process process =
        new ProcessBuilder(
                keytool.toString(),
                "-genkeypair",
                "-alias",
                "localhost",
                "-keyalg",
                "EC",
                "-storetype",
                "PKCS12",
                "-keystore",
                keyStore.toString(),
                "-storepass",
                TEST_PASSWORD,
                "-dname",
                "CN=localhost",
                "-ext",
                "SAN=DNS:localhost,IP:127.0.0.1",
                "-validity",
                "2",
                "-noprompt")
            .redirectErrorStream(true)
            .redirectOutput(log.toFile())
            .start();
    if (!process.waitFor(30, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      throw new IllegalStateException("Certificate generation timed out");
    }
    assertEquals(
        0,
        process.exitValue(),
        () -> {
          try {
            return Files.readString(log);
          } catch (java.io.IOException ex) {
            return ex.toString();
          }
        });
  }

  @BeforeEach
  void startServer() throws Exception {
    var source = new JdbcDataSource();
    source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
    source.setUser("sa");
    ServerApplication.initialize(source);
    DemoData.seed(source);
    db = new JdbcDatabaseAdapter(source);
    var sessions = List.of(customer, merchant, rider, moderator, outsider);
    var serverTls = TlsContexts.server(keyStore, TEST_PASSWORD.toCharArray());
    var clientTls = TlsContexts.client(keyStore, TEST_PASSWORD.toCharArray());
    server =
        ServerApplication.start(
            new InetSocketAddress(InetAddress.getLoopbackAddress(), 0),
            serverTls,
            source,
            working.resolve("server"),
            token ->
                sessions.stream()
                    .filter(session -> session.accessToken().value().equals(token.value()))
                    .findFirst()
                    .map(
                        session ->
                            new SessionPrincipal(
                                session.actorId(), session.permissions(), session.expiresAt())),
            CLOCK);
    local = new FileLocalResourceStore(ASSETS, working.resolve("local"));
    transport =
        new JdkHttpsTransport(
            URI.create("https://localhost:" + server.port()),
            HttpClient.newBuilder().sslContext(clientTls).build(),
            CODEC,
            local);
  }

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.close();
    }
  }

  @Test
  void competingOrderWritesAndReplaysUseTheDatabaseVersion() throws Exception {
    UUID order = place();
    assertStatus(
        ResultStatus.FORBIDDEN, submit(outsider, OperationCode.ACCEPT_ORDER, command(order, 0)));
    assertStatus(
        ResultStatus.SUCCESS, submit(merchant, OperationCode.ACCEPT_ORDER, command(order, 0)));

    var cancel = serverRequest(customer, OperationCode.CANCEL_ORDER, command(order, 1));
    var prepare = serverRequest(merchant, OperationCode.START_PREPARATION, command(order, 1));
    var first = transport.submit(cancel).toCompletableFuture();
    var second = transport.submit(prepare).toCompletableFuture();
    var cancelled = first.get(10, TimeUnit.SECONDS);
    var started = second.get(10, TimeUnit.SECONDS);
    assertEquals(
        Set.of(ResultStatus.SUCCESS, ResultStatus.CONFLICT),
        Set.of(cancelled.status(), started.status()));

    var successful = cancelled.status() == ResultStatus.SUCCESS ? cancel : prepare;
    assertEquals(
        cancelled.status() == ResultStatus.SUCCESS ? cancelled : started, exchange(successful));
    var message = successful.message();
    var changed =
        new Message(
            message.requestId(),
            message.correlationId(),
            message.senderId(),
            message.targetId(),
            message.operationCode(),
            command(order, 2));
    var actor = successful == cancel ? customer : merchant;
    assertStatus(ResultStatus.CONFLICT, exchange(security(actor).prepare(changed)));
    var detail =
        assertInstanceOf(
            UiValuesPayload.class,
            submit(customer, OperationCode.ORDER_DETAIL, new TargetPayload(order)).payload());
    assertEquals(2, ((DecimalValue) detail.values().get(23)).value().intValueExact());
  }

  @Test
  void completeOrderLifecycleAndCaseResolution() throws Exception {
    UUID received = place();
    deliver(received);
    assertStatus(
        ResultStatus.SUCCESS,
        submit(customer, OperationCode.CONFIRM_RECEIPT, command(received, 5)));
    var purchase = publish(customer, "Purchased after completion");
    assertEquals(PurchaseMark.PURCHASED, purchase.comment().purchaseMark());
    UUID missing = place();
    deliver(missing);
    var reported = submit(customer, OperationCode.REPORT_NOT_RECEIVED, command(missing, 5));
    assertStatus(ResultStatus.SUCCESS, reported);
    var values = assertInstanceOf(UiValuesPayload.class, reported.payload());
    UUID caseId = UUID.fromString(((TextValue) values.values().get(21)).value());
    assertStatus(
        ResultStatus.SUCCESS,
        submit(
            moderator,
            OperationCode.RESOLVE_CASE,
            new ResolveCasePayload(caseId, "Refund agreed")));
    assertStatus(
        ResultStatus.CONFLICT,
        submit(moderator, OperationCode.RESOLVE_CASE, new ResolveCasePayload(caseId, "Duplicate")));
    UUID rejected = place();
    assertStatus(
        ResultStatus.SUCCESS, submit(merchant, OperationCode.REJECT_ORDER, command(rejected, 0)));
  }

  @Test
  void paginatesModeratesAndShowsReplies() throws Exception {
    var longest = publish(customer, "longest!");
    publish(customer, "abc");
    publish(customer, "12345");
    assertStatus(
        ResultStatus.FORBIDDEN,
        submit(
            outsider,
            OperationCode.COMMENT_WITHDRAW,
            new WithdrawCommentPayload(longest.comment().commentId())));
    assertStatus(
        ResultStatus.FORBIDDEN,
        submit(
            outsider,
            OperationCode.COMMENT_REPLY,
            new ReplyCommentPayload(longest.comment().commentId(), "Unrelated shop")));
    assertStatus(
        ResultStatus.SUCCESS,
        submit(
            merchant,
            OperationCode.COMMENT_REPLY,
            new ReplyCommentPayload(longest.comment().commentId(), "Thank you")));
    var first = comments(CommentSort.LONG_FIRST, Optional.empty(), 2);
    assertEquals(
        List.of("longest!", "12345"), first.page().items().stream().map(c -> c.text()).toList());
    assertEquals("Thank you", first.page().items().get(0).replies().get(0).text());
    var next = comments(CommentSort.LONG_FIRST, first.page().nextCursor(), 2);
    assertEquals(List.of("abc"), next.page().items().stream().map(c -> c.text()).toList());
    assertTrue(next.page().nextCursor().isEmpty());
    assertStatus(
        ResultStatus.INVALID_REQUEST,
        submit(
            customer,
            OperationCode.COMMENT_LIST,
            new ListCommentPayload(SHOP, CommentSort.IMAGE_FIRST, first.page().nextCursor(), 2)));
    assertStatus(
        ResultStatus.SUCCESS,
        submit(
            moderator,
            OperationCode.COMMENT_HIDE,
            new HideCommentPayload(longest.comment().commentId(), "Moderation test")));
    assertStatus(
        ResultStatus.SUCCESS,
        submit(
            customer,
            OperationCode.COMMENT_WITHDRAW,
            new WithdrawCommentPayload(next.page().items().get(0).commentId())));
    assertEquals(1, comments(CommentSort.LONG_FIRST, Optional.empty(), 10).page().items().size());
  }

  @Test
  void publishesAndDownloadsAnImageThroughTheUiAndHttps() throws Exception {
    var rendered = new LinkedBlockingQueue<UiPage>();
    var results = new LinkedBlockingQueue<MessagePayload>();
    var ui =
        ClientApplication.connect(
            customer,
            CODEC,
            CLOCK,
            transport,
            new FileUiLibrary(ASSETS),
            local,
            Runnable::run,
            rendered::offer,
            results::offer);
    ui.receiveUiInput(new UiInput(1001, 0, 1001, new InputValues(Map.of())));
    assertEquals(1001, await(rendered).pageId());
    publish(customer, "A much longer text-only comment");

    var png = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", png);
    byte[] bytes = png.toByteArray();
    var localImage =
        local.save(
            new ResourceBlob(
                new RemoteResourceRef(UUID.randomUUID().toString(), "image/png", bytes.length),
                bytes));
    ui.submit(
        OperationCode.COMMENT_PUBLISH.code(),
        new CommentDraftPayload(
            new PublishCommentPayload(SHOP, "Image", List.of()), List.of(localImage)));
    var reply = assertInstanceOf(OperationReplyPayload.class, await(results));
    assertEquals(ResultStatus.SUCCESS, reply.status());
    var posted = assertInstanceOf(CommentViewPayload.class, reply.payload()).comment();
    assertEquals(1, posted.images().size());
    String imageId = posted.images().get(0).resourceId();
    assertStatus(
        ResultStatus.FORBIDDEN,
        submit(
            outsider,
            OperationCode.COMMENT_PUBLISH,
            new PublishCommentPayload(SHOP, "Someone else's image", List.of(imageId))));

    var correlation =
        ui.submit(
            OperationCode.COMMENT_LIST.code(),
            new ListCommentPayload(SHOP, CommentSort.IMAGE_FIRST, Optional.empty(), 1));
    var page = await(rendered);
    assertEquals(correlation, page.requestId());
    assertTrue(
        page.root().children().stream()
            .flatMap(node -> node.value().stream())
            .anyMatch(value -> value instanceof TextValue text && text.value().endsWith("Image")));
    var image =
        page.root().children().stream()
            .flatMap(node -> node.resource().stream())
            .filter(ref -> ref.kind() == ResourceKind.IMAGE)
            .findFirst()
            .orElseThrow();
    assertArrayEquals(bytes, Files.readAllBytes(local.resolve(image)));
    assertTrue(
        new FileLocalResourceStore(ASSETS, working.resolve("local"))
            .find(
                new ResourceDescriptor(
                    1, Optional.empty(), Optional.of(imageId), ResourceKind.IMAGE))
            .isPresent());
    assertStatus(
        ResultStatus.SUCCESS,
        submit(
            merchant,
            OperationCode.GET_RESOURCE,
            new ResourceRequestPayload(
                new ResourceDescriptor(
                    1, Optional.empty(), Optional.of(imageId), ResourceKind.IMAGE))));
    assertStatus(
        ResultStatus.SUCCESS,
        submit(
            moderator,
            OperationCode.COMMENT_HIDE,
            new HideCommentPayload(posted.commentId(), "Hide attached image")));
    assertStatus(
        ResultStatus.NOT_FOUND,
        submit(
            merchant,
            OperationCode.GET_RESOURCE,
            new ResourceRequestPayload(
                new ResourceDescriptor(
                    1, Optional.empty(), Optional.of(imageId), ResourceKind.IMAGE))));
  }

  @Test
  void rejectsInvalidSessionsAndRollsBackFailedWrites() throws Exception {
    var valid =
        serverRequest(
            customer,
            OperationCode.COMMENT_LIST,
            new ListCommentPayload(SHOP, CommentSort.LONG_FIRST, Optional.empty(), 10));
    var expired =
        new SecureRequest(
            valid.requestId(),
            valid.accessToken(),
            Instant.EPOCH,
            valid.bodyDigest(),
            valid.message());
    assertStatus(ResultStatus.UNAUTHENTICATED, exchange(expired));
    byte[] incorrect = valid.bodyDigest();
    incorrect[0] ^= 1;
    assertStatus(
        ResultStatus.UNAUTHENTICATED,
        exchange(
            new SecureRequest(
                valid.requestId(),
                valid.accessToken(),
                valid.sentAt(),
                incorrect,
                valid.message())));
    assertStatus(
        ResultStatus.UNAUTHENTICATED,
        exchange(
            new SecureRequest(
                valid.requestId(),
                new AccessToken(UUID.randomUUID().toString()),
                valid.sentAt(),
                valid.bodyDigest(),
                valid.message())));
    assertTrue(comments(CommentSort.LONG_FIRST, Optional.empty(), 10).page().items().isEmpty());

    UUID shop = UUID.randomUUID();
    assertThrows(
        IllegalStateException.class,
        () ->
            db.inTransaction(
                tx -> {
                  Jdbc.update(
                      tx, "INSERT INTO shops VALUES (?, ?)", shop, merchant.actorId().value());
                  Jdbc.update(
                      tx, "INSERT INTO shops VALUES (?, ?)", shop, outsider.actorId().value());
                  return null;
                }));
    boolean exists =
        db.inTransaction(
            tx ->
                Jdbc.first(tx, "SELECT 1 FROM shops WHERE shop_id=?", row -> row.getInt(1), shop)
                    .isPresent());
    assertFalse(exists);
  }

  @Test
  void rendersOnlyTheLatestPageAndRecoversAfterTransportFailure() throws Exception {
    record Waiting(SecureRequest request, CompletableFuture<SecureResponse> future) {}

    var calls = new LinkedBlockingQueue<Waiting>();
    HttpsTransport controlled =
        new HttpsTransport() {
          @Override
          public CompletionStage<SecureResponse> submit(SecureRequest request) {
            var future = new CompletableFuture<SecureResponse>();
            calls.add(new Waiting(request, future));
            return future;
          }

          @Override
          public CompletionStage<SecureResponse> upload(
              SecureRequest request, LocalResourceRef file) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException());
          }
        };
    var rendered = new LinkedBlockingQueue<UiPage>();
    var results = new LinkedBlockingQueue<MessagePayload>();
    var ui =
        ClientApplication.connect(
            customer,
            CODEC,
            CLOCK,
            controlled,
            new FileUiLibrary(ASSETS),
            local,
            Runnable::run,
            rendered::offer,
            results::offer);
    var order = new TargetPayload(UUID.randomUUID());
    ui.submit(OperationCode.ORDER_DETAIL.code(), order);
    var older = await(calls);
    UUID newest = ui.submit(OperationCode.ORDER_DETAIL.code(), order);
    var later = await(calls);
    later
        .future()
        .complete(
            new SecureResponse(
                later.request().requestId(),
                ResultStatus.SUCCESS,
                new UiValuesPayload(Map.of(20, new TextValue("Latest result")))));
    assertEquals(newest, await(rendered).requestId());
    older
        .future()
        .complete(
            new SecureResponse(
                older.request().requestId(),
                ResultStatus.SUCCESS,
                new UiValuesPayload(Map.of(20, new TextValue("Previous result")))));
    assertTrue(rendered.isEmpty());

    ui.submit(OperationCode.ORDER_DETAIL.code(), order);
    await(calls).future().completeExceptionally(new java.io.IOException("Connection closed"));
    var failure = assertInstanceOf(OperationReplyPayload.class, await(results));
    assertEquals(ResultStatus.FAILED, failure.status());
    assertInstanceOf(ErrorPayload.class, failure.payload());
    ui.receiveUiInput(new UiInput(1001, 0, 1001, new InputValues(Map.of())));
    assertEquals(1001, await(rendered).pageId());
  }

  @Test
  void rejectsBusyQueueAndWorkerSurvivesOperationFailure() throws Exception {
    var pool = new TaskPool(1);
    var sender = new ResponseSender();
    var receiver =
        new ServerReceiver(
            request ->
                new ServerRequestContext(
                    customer.actorId(), customer.permissions(), request.requestId()),
            pool,
            sender);
    var failedRequest =
        serverRequest(customer, OperationCode.ORDER_DETAIL, new TargetPayload(UUID.randomUUID()));
    var nextRequest =
        serverRequest(customer, OperationCode.ORDER_DETAIL, new TargetPayload(UUID.randomUUID()));
    var responses = new LinkedBlockingQueue<SecureResponse>();
    receiver.receive(failedRequest, responses::add);
    receiver.receive(nextRequest, responses::add);
    assertStatus(ResultStatus.BUSY, await(responses));

    var thread =
        new Thread(
            new Worker(
                pool,
                task -> {
                  if (task.context().requestId().equals(failedRequest.requestId())) {
                    throw new IllegalStateException("Injected operation failure");
                  }
                  return new SecureResponse(
                      task.context().requestId(), ResultStatus.SUCCESS, new EmptyPayload());
                },
                sender));
    thread.start();
    try {
      assertStatus(ResultStatus.FAILED, await(responses));
      receiver.receive(nextRequest, responses::add);
      assertStatus(ResultStatus.SUCCESS, await(responses));
    } finally {
      thread.interrupt();
      thread.join(5_000);
      assertFalse(thread.isAlive());
    }
  }

  private static ClientSession session(DemoData.Role role, Permission... permissions) {
    return sessionFor(role.user(), permissions);
  }

  private static ClientSession sessionFor(
      edu.nju.sehw.takeout.model.common.UserId user, Permission... permissions) {
    var allowed =
        EnumSet.of(Permission.READ_ORDER, Permission.READ_COMMENT, Permission.READ_RESOURCE);
    allowed.addAll(List.of(permissions));
    return new ClientSession(
        user,
        new PermissionSet(allowed),
        new AccessToken(UUID.randomUUID().toString()),
        CLOCK.instant().plusSeconds(3600));
  }

  private static DefaultRequestSecurity security(ClientSession session) {
    return new DefaultRequestSecurity(session, CODEC, CLOCK);
  }

  private static SecureRequest serverRequest(
      ClientSession session, OperationCode operation, MessagePayload payload) {
    return security(session)
        .prepare(new Message(UUID.randomUUID(), 6, 8, operation.code(), payload));
  }

  private SecureResponse exchange(SecureRequest request) throws Exception {
    return transport.submit(request).toCompletableFuture().get(10, TimeUnit.SECONDS);
  }

  private SecureResponse submit(
      ClientSession session, OperationCode operation, MessagePayload payload) throws Exception {
    return exchange(serverRequest(session, operation, payload));
  }

  private static void assertStatus(ResultStatus status, SecureResponse response) {
    assertEquals(status, response.status(), () -> response.payload().toString());
  }

  private static <T> T await(LinkedBlockingQueue<T> queue) throws InterruptedException {
    T result = queue.poll(10, TimeUnit.SECONDS);
    assertNotNull(result);
    return result;
  }

  private UUID place() throws Exception {
    var twoDumplings = new PlaceOrderPayload(DemoData.SHOP, Map.of(DemoData.DUMPLINGS, 2));
    var response = submit(customer, OperationCode.PLACE_ORDER, twoDumplings);
    assertStatus(ResultStatus.SUCCESS, response);
    var values = assertInstanceOf(UiValuesPayload.class, response.payload()).values();
    return UUID.fromString(((TextValue) values.get(21)).value());
  }

  private void deliver(UUID order) throws Exception {
    assertStatus(
        ResultStatus.SUCCESS, submit(merchant, OperationCode.ACCEPT_ORDER, command(order, 0)));
    assertStatus(
        ResultStatus.SUCCESS, submit(merchant, OperationCode.START_PREPARATION, command(order, 1)));
    assertStatus(
        ResultStatus.SUCCESS,
        submit(merchant, OperationCode.FINISH_PREPARATION, command(order, 2)));
    assertStatus(
        ResultStatus.SUCCESS, submit(rider, OperationCode.RIDER_PICKUP, command(order, 3)));
    assertStatus(
        ResultStatus.SUCCESS, submit(rider, OperationCode.CONFIRM_DELIVERY, command(order, 4)));
  }

  private static OrderCommandPayload command(UUID order, long version) {
    return new OrderCommandPayload(order, version);
  }

  private CommentViewPayload publish(ClientSession session, String text) throws Exception {
    var response =
        submit(
            session,
            OperationCode.COMMENT_PUBLISH,
            new PublishCommentPayload(SHOP, text, List.of()));
    assertStatus(ResultStatus.SUCCESS, response);
    return assertInstanceOf(CommentViewPayload.class, response.payload());
  }

  private CommentPagePayload comments(CommentSort sort, Optional<String> cursor, int size)
      throws Exception {
    var reply =
        submit(
            customer, OperationCode.COMMENT_LIST, new ListCommentPayload(SHOP, sort, cursor, size));
    assertStatus(ResultStatus.SUCCESS, reply);
    return assertInstanceOf(CommentPagePayload.class, reply.payload());
  }
}
