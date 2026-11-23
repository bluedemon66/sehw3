package edu.nju.sehw.takeout.app;

import edu.nju.sehw.takeout.model.message.BinaryMessageCodec;
import edu.nju.sehw.takeout.model.message.EnvelopeCodec;
import edu.nju.sehw.takeout.server.comment.CommentOperationHandler;
import edu.nju.sehw.takeout.server.comment.DefaultPurchaseEvidenceResolver;
import edu.nju.sehw.takeout.server.comment.JdbcCommentRepository;
import edu.nju.sehw.takeout.server.network.HttpsEndpoint;
import edu.nju.sehw.takeout.server.network.ResponseSender;
import edu.nju.sehw.takeout.server.network.ServerReceiver;
import edu.nju.sehw.takeout.server.operation.ServerOperationExecutor;
import edu.nju.sehw.takeout.server.operation.StandardOperations;
import edu.nju.sehw.takeout.server.order.JdbcExceptionCaseRepository;
import edu.nju.sehw.takeout.server.order.JdbcOrderRepository;
import edu.nju.sehw.takeout.server.order.OrderOperationHandler;
import edu.nju.sehw.takeout.server.persistence.JdbcDatabaseAdapter;
import edu.nju.sehw.takeout.server.persistence.JdbcIdempotencyStore;
import edu.nju.sehw.takeout.server.resource.FileServerResourceStore;
import edu.nju.sehw.takeout.server.resource.ServerUploadHandler;
import edu.nju.sehw.takeout.server.security.DefaultServerPermissionChecker;
import edu.nju.sehw.takeout.server.security.DefaultServerRequestVerifier;
import edu.nju.sehw.takeout.server.security.ServerSessionStore;
import edu.nju.sehw.takeout.server.task.TaskPool;
import edu.nju.sehw.takeout.server.task.Worker;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.SSLContext;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;

/** Runs the lab's JDBC-backed HTTPS endpoint and worker pool. */
public final class ServerApplication implements AutoCloseable {
  private final HttpsEndpoint endpoint;
  private final ExecutorService workers;
  private final TaskPool pool;

  private ServerApplication(HttpsEndpoint endpoint, ExecutorService workers, TaskPool pool) {
    this.endpoint = endpoint;
    this.workers = workers;
    this.pool = pool;
  }

  /** Creates the schema in the configured H2 database. */
  public static void initialize(DataSource dataSource) throws IOException, SQLException {
    try (var schema =
            Objects.requireNonNull(ServerApplication.class.getResourceAsStream("/schema.sql"));
        var reader = new InputStreamReader(schema, StandardCharsets.UTF_8);
        var connection = dataSource.getConnection()) {
      RunScript.execute(connection, reader);
    }
  }

  /** Starts the endpoint with the supplied adapters and its own worker threads. */
  public static ServerApplication start(
      InetSocketAddress address,
      SSLContext tls,
      DataSource dataSource,
      Path files,
      ServerSessionStore sessions,
      Clock clock)
      throws IOException {
    var codec = new BinaryMessageCodec();
    var db = new JdbcDatabaseAdapter(dataSource);
    var resources = new FileServerResourceStore(files, db);
    var comments =
        new CommentOperationHandler(
            new JdbcCommentRepository(db),
            new DefaultPurchaseEvidenceResolver(db),
            resources,
            text -> text.codePointCount(0, text.length()));
    var orders =
        new OrderOperationHandler(
            new JdbcOrderRepository(clock), new JdbcExceptionCaseRepository(clock));
    var executor =
        new ServerOperationExecutor(
            new DefaultServerPermissionChecker(),
            StandardOperations.catalog(),
            db,
            db,
            new JdbcIdempotencyStore(),
            comments,
            orders,
            new ServerUploadHandler(resources));
    var pool = new TaskPool(32);
    var sender = new ResponseSender();
    var receiver =
        new ServerReceiver(new DefaultServerRequestVerifier(sessions, codec, clock), pool, sender);
    var endpoint = new HttpsEndpoint(address, tls, new EnvelopeCodec(codec), receiver);
    var workers = Executors.newFixedThreadPool(4);
    for (int i = 0; i < 4; i++) {
      workers.submit(new Worker(pool, executor, sender));
    }
    return new ServerApplication(endpoint, workers, pool);
  }

  /** Returns the port used by the running server. */
  public int port() {
    return endpoint.address().getPort();
  }

  @Override
  public synchronized void close() {
    if (workers.isShutdown()) {
      return;
    }
    endpoint.close();
    workers.shutdownNow();
    pool.drain().forEach(task -> task.upload().ifPresent(upload -> upload.discard()));
  }

  /** Runs the localhost demo using a PKCS12 key store and the configured demo sessions. */
  public static void main(String[] args) throws Exception {
    if (args.length < 1 || args.length > 2) {
      throw new IllegalArgumentException("Usage: ServerApplication <keystore.p12> [port]");
    }
    int port = args.length == 2 ? Integer.parseInt(args[1]) : 8443;
    var clock = Clock.systemUTC();
    var data = Path.of("target", "takeout-server").toAbsolutePath();
    Files.createDirectories(data);
    var source = new JdbcDataSource();
    source.setURL("jdbc:h2:" + data.resolve("takeout").toString().replace('\\', '/'));
    source.setUser("sa");
    initialize(source);
    DemoData.seed(source);
    var tls =
        TlsContexts.server(
            Path.of(args[0]), DemoData.requiredEnvironment("SEHW3_TLS_PASSWORD").toCharArray());
    try (var server =
        start(
            new InetSocketAddress(InetAddress.getLoopbackAddress(), port),
            tls,
            source,
            data.resolve("images"),
            DemoData.serverSessions(clock),
            clock)) {
      Runtime.getRuntime().addShutdownHook(new Thread(server::close, "takeout-shutdown"));
      System.out.println("Takeout server ready on localhost:" + server.port());
      new CountDownLatch(1).await();
    }
  }
}
