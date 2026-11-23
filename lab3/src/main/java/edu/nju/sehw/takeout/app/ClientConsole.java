package edu.nju.sehw.takeout.app;

import edu.nju.sehw.takeout.client.query.JdkHttpsTransport;
import edu.nju.sehw.takeout.client.resource.FileLocalResourceStore;
import edu.nju.sehw.takeout.client.resource.FileUiLibrary;
import edu.nju.sehw.takeout.client.ui.UiInteractionController;
import edu.nju.sehw.takeout.model.comment.CommentDraftPayload;
import edu.nju.sehw.takeout.model.comment.CommentSort;
import edu.nju.sehw.takeout.model.comment.CommentSubject;
import edu.nju.sehw.takeout.model.comment.HideCommentPayload;
import edu.nju.sehw.takeout.model.comment.ListCommentPayload;
import edu.nju.sehw.takeout.model.comment.PublishCommentPayload;
import edu.nju.sehw.takeout.model.comment.ReplyCommentPayload;
import edu.nju.sehw.takeout.model.comment.SubjectKind;
import edu.nju.sehw.takeout.model.comment.WithdrawCommentPayload;
import edu.nju.sehw.takeout.model.message.BinaryMessageCodec;
import edu.nju.sehw.takeout.model.message.EmptyPayload;
import edu.nju.sehw.takeout.model.message.MessagePayload;
import edu.nju.sehw.takeout.model.message.OperationCode;
import edu.nju.sehw.takeout.model.message.TargetPayload;
import edu.nju.sehw.takeout.model.order.OrderCommandPayload;
import edu.nju.sehw.takeout.model.order.PlaceOrderPayload;
import edu.nju.sehw.takeout.model.order.ResolveCasePayload;
import edu.nju.sehw.takeout.model.resource.ImageMedia;
import edu.nju.sehw.takeout.model.resource.LocalResourceRef;
import edu.nju.sehw.takeout.model.resource.RemoteResourceRef;
import edu.nju.sehw.takeout.model.resource.ResourceBlob;
import edu.nju.sehw.takeout.model.ui.BooleanValue;
import edu.nju.sehw.takeout.model.ui.DecimalValue;
import edu.nju.sehw.takeout.model.ui.InputValues;
import edu.nju.sehw.takeout.model.ui.TextValue;
import edu.nju.sehw.takeout.model.ui.UiInput;
import edu.nju.sehw.takeout.model.ui.UiPage;
import edu.nju.sehw.takeout.model.ui.UiResourceNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Runs the takeout application as a command-line client with an independent UI thread. */
public final class ClientConsole {
  private static final String HELP =
      """
      home
      list [LONG_FIRST|IMAGE_FIRST]    next
      publish <text> [| <image path>]    withdraw <comment ID>
      reply <comment ID> <text>    hide <comment ID> <reason>
      place [quantity]    detail <order ID>    turnover    tasks
      accept|reject|cancel|prepare|ready|pickup|deliver|confirm|missing <order ID> <version>
      resolve <case ID> <resolution>
      help    quit
      """;

  private final FileLocalResourceStore resources;
  private final ArrayBlockingQueue<Object> replies = new ArrayBlockingQueue<>(1);
  private final UiInteractionController ui;
  private CommentSort sort = CommentSort.LONG_FIRST;
  private Optional<String> cursor = Optional.empty();

  private ClientConsole(
      URI endpoint, Path trust, DemoData.Role role, Path assets, ExecutorService uiThread)
      throws Exception {
    var clock = Clock.systemUTC();
    var codec = new BinaryMessageCodec();
    var tls =
        TlsContexts.client(trust, DemoData.requiredEnvironment("SEHW3_TLS_PASSWORD").toCharArray());
    var http = HttpClient.newBuilder().sslContext(tls).build();
    resources =
        new FileLocalResourceStore(assets, Path.of("target", "takeout-client", role.name()));
    var transport = new JdkHttpsTransport(endpoint, http, codec, resources);
    ui =
        ClientApplication.connect(
            DemoData.clientSession(role, clock),
            codec,
            clock,
            transport,
            new FileUiLibrary(assets),
            resources,
            uiThread::execute,
            replies::offer,
            replies::offer);
  }

  /** Opens the console as a demo role and connects it to the supplied server. */
  public static void main(String[] args) throws Exception {
    if (args.length < 3 || args.length > 4) {
      throw new IllegalArgumentException(
          "Usage: ClientConsole <https://localhost:8443> <truststore.p12> <role> [assets]");
    }
    if (args.length == 4) {
      runConsole(args, Path.of(args[3]));
      return;
    }
    URI bundled =
        Objects.requireNonNull(ClientConsole.class.getResource("/ui"), "UI assets").toURI();
    if ("jar".equals(bundled.getScheme())) {
      try (var archive = FileSystems.newFileSystem(bundled, Map.of())) {
        runConsole(args, archive.getPath("/ui"));
      }
    } else {
      runConsole(args, Path.of(bundled));
    }
  }

  private static void runConsole(String[] args, Path workspace) throws Exception {
    var role = DemoData.Role.valueOf(args[2].toUpperCase(Locale.ROOT));
    var thread = Executors.newSingleThreadExecutor();
    try (var input = new Scanner(System.in, StandardCharsets.UTF_8)) {
      var console =
          new ClientConsole(URI.create(args[0]), Path.of(args[1]), role, workspace, thread);
      console.home();
      System.out.println(HELP);
      while (input.hasNextLine()) {
        String line = input.nextLine().strip();
        if (line.equals("quit")) {
          break;
        }
        try {
          console.execute(line);
        } catch (IllegalArgumentException | IOException ex) {
          System.out.println(ex.getMessage());
        }
      }
    } finally {
      thread.shutdownNow();
    }
  }

  private void execute(String line) throws IOException, InterruptedException {
    String[] words = line.split("\\s+", 3);
    switch (words[0]) {
      case "" -> {}
      case "help" -> System.out.println(HELP);
      case "home" -> home();
      case "list" -> {
        sort = words.length == 1 ? CommentSort.LONG_FIRST : CommentSort.valueOf(words[1]);
        cursor = Optional.empty();
        list();
      }
      case "next" -> {
        if (cursor.isPresent()) {
          list();
        } else {
          System.out.println("No further comments");
        }
      }
      case "publish" -> publish(line.substring(words[0].length()).strip());
      case "withdraw" ->
          submit(OperationCode.COMMENT_WITHDRAW, new WithdrawCommentPayload(id(words)));
      case "reply" ->
          submit(OperationCode.COMMENT_REPLY, new ReplyCommentPayload(id(words), tail(words)));
      case "hide" ->
          submit(OperationCode.COMMENT_HIDE, new HideCommentPayload(id(words), tail(words)));
      case "place" ->
          submit(
              OperationCode.PLACE_ORDER,
              new PlaceOrderPayload(
                  DemoData.SHOP,
                  Map.of(DemoData.DUMPLINGS, words.length == 1 ? 1 : Integer.parseInt(words[1]))));
      case "detail" -> submit(OperationCode.ORDER_DETAIL, new TargetPayload(id(words)));
      case "turnover" -> submit(OperationCode.MERCHANT_TURNOVER, new TargetPayload(DemoData.SHOP));
      case "tasks" -> submit(OperationCode.RIDER_TASKS, new EmptyPayload());
      case "resolve" ->
          submit(OperationCode.RESOLVE_CASE, new ResolveCasePayload(id(words), tail(words)));
      default -> {
        OperationCode operation =
            switch (words[0]) {
              case "accept" -> OperationCode.ACCEPT_ORDER;
              case "reject" -> OperationCode.REJECT_ORDER;
              case "cancel" -> OperationCode.CANCEL_ORDER;
              case "prepare" -> OperationCode.START_PREPARATION;
              case "ready" -> OperationCode.FINISH_PREPARATION;
              case "pickup" -> OperationCode.RIDER_PICKUP;
              case "deliver" -> OperationCode.CONFIRM_DELIVERY;
              case "confirm" -> OperationCode.CONFIRM_RECEIPT;
              case "missing" -> OperationCode.REPORT_NOT_RECEIVED;
              default ->
                  throw new IllegalArgumentException(
                      "Unknown command. Enter help for the command list.");
            };
        submit(operation, new OrderCommandPayload(id(words), Long.parseLong(tail(words))));
      }
    }
  }

  private void home() throws InterruptedException {
    ui.receiveUiInput(new UiInput(1001, 0, 1001, new InputValues(Map.of())));
    showReply();
  }

  private static UUID id(String[] words) {
    if (words.length < 2) {
      throw new IllegalArgumentException("An identifier is required");
    }
    return UUID.fromString(words[1]);
  }

  private static String tail(String[] words) {
    if (words.length < 3 || words[2].isBlank()) {
      throw new IllegalArgumentException("A version or text value is required");
    }
    return words[2];
  }

  private static CommentSubject shop() {
    return new CommentSubject(SubjectKind.SHOP, DemoData.SHOP, Optional.empty(), Optional.empty());
  }

  private void list() throws InterruptedException {
    submit(OperationCode.COMMENT_LIST, new ListCommentPayload(shop(), sort, cursor, 10));
  }

  private void publish(String argument) throws IOException, InterruptedException {
    String[] parts = argument.split("[|]", 2);
    List<LocalResourceRef> files = List.of();
    if (parts.length == 2) {
      byte[] bytes;
      try (var input = Files.newInputStream(Path.of(parts[1].strip()))) {
        bytes = input.readNBytes(5 * 1024 * 1024 + 1);
      }
      if (bytes.length > 5 * 1024 * 1024) {
        throw new IllegalArgumentException("Image exceeds 5 MiB");
      }
      var ref =
          new RemoteResourceRef(
              UUID.randomUUID().toString(), ImageMedia.detect(bytes), bytes.length);
      files = List.of(resources.save(new ResourceBlob(ref, bytes)));
    }
    submit(
        OperationCode.COMMENT_PUBLISH,
        new CommentDraftPayload(
            new PublishCommentPayload(shop(), parts[0].strip(), List.of()), files));
  }

  private void submit(OperationCode operation, MessagePayload payload) throws InterruptedException {
    ui.submit(operation.code(), payload);
    showReply();
  }

  private void showReply() throws InterruptedException {
    Object result = replies.poll(35, TimeUnit.SECONDS);
    if (result instanceof UiPage page) {
      if (page.pageId() == 1004) {
        cursor =
            page.root().children().stream()
                .filter(node -> node.elementId() == 19)
                .flatMap(node -> node.value().stream())
                .filter(TextValue.class::isInstance)
                .map(TextValue.class::cast)
                .map(TextValue::value)
                .findFirst();
      }
      printNode(page.root());
    } else {
      System.out.println(result == null ? "Request timed out" : result);
    }
  }

  private void printNode(UiResourceNode node) {
    if (node.elementId() != 19) {
      node.value()
          .ifPresent(
              value -> {
                if (value instanceof TextValue text) {
                  System.out.println(text.value());
                } else if (value instanceof DecimalValue decimal) {
                  System.out.println(decimal.value().toPlainString());
                } else if (value instanceof BooleanValue bool) {
                  System.out.println(bool.value());
                }
              });
    }
    node.resource()
        .ifPresent(
            ref -> {
              Path path = resources.resolve(ref);
              try {
                switch (ref.kind()) {
                  case TEXT, UI_LAYOUT -> System.out.println(Files.readString(path).strip());
                  default -> System.out.println("Image cached at " + path);
                }
              } catch (IOException ex) {
                System.out.println("Resource unavailable at " + path);
              }
            });
    node.children().forEach(this::printNode);
  }
}
