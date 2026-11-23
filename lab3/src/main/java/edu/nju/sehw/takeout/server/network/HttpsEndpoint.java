package edu.nju.sehw.takeout.server.network;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import edu.nju.sehw.takeout.model.message.BinaryMessageCodec;
import edu.nju.sehw.takeout.model.message.EnvelopeCodec;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;

/** Accepts versioned HTTPS requests and hands their reply channels to the task queue. */
public final class HttpsEndpoint implements AutoCloseable {
  private final HttpsServer server;
  private final ExecutorService requests = Executors.newFixedThreadPool(8);

  /** Creates and starts an HTTPS endpoint with the supplied server TLS context. */
  public HttpsEndpoint(
      InetSocketAddress address, SSLContext tls, EnvelopeCodec codec, ServerReceiver receiver)
      throws IOException {
    server = HttpsServer.create(address, 32);
    server.setHttpsConfigurator(new HttpsConfigurator(tls));
    server.setExecutor(requests);
    server.createContext("/api/operations", exchange -> handle(exchange, codec, receiver, false));
    server.createContext("/api/uploads", exchange -> handle(exchange, codec, receiver, true));
    server.start();
  }

  /** Returns the bound address, including the assigned port. */
  public InetSocketAddress address() {
    return server.getAddress();
  }

  private static void handle(
      HttpExchange exchange, EnvelopeCodec codec, ServerReceiver receiver, boolean upload)
      throws IOException {
    if (!exchange.getRequestMethod().equals("POST")) {
      exchange.getResponseHeaders().set("Allow", "POST");
      reject(exchange, 405);
      return;
    }
    try (var in = new DataInputStream(exchange.getRequestBody())) {
      int size = in.readInt();
      if (size < 1 || size > BinaryMessageCodec.MAX_BYTES) {
        reject(exchange, 413);
        return;
      }
      byte[] body = in.readNBytes(size);
      if (body.length != size || (!upload && in.read() != -1)) {
        reject(exchange, 400);
        return;
      }
      var request = codec.decodeRequest(body);
      if (upload) {
        receiver.receiveUpload(
            request, in, response -> send(exchange, codec.encodeResponse(response)));
      } else {
        receiver.receive(request, response -> send(exchange, codec.encodeResponse(response)));
      }
    } catch (IllegalArgumentException | IOException ex) {
      reject(exchange, 400);
    }
  }

  private static void send(HttpExchange exchange, byte[] bytes) throws IOException {
    try (exchange) {
      exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
      exchange.sendResponseHeaders(200, bytes.length);
      exchange.getResponseBody().write(bytes);
    }
  }

  private static void reject(HttpExchange exchange, int status) throws IOException {
    try (exchange) {
      exchange.sendResponseHeaders(status, -1);
    }
  }

  @Override
  public void close() {
    server.stop(1);
    requests.shutdownNow();
    try {
      requests.awaitTermination(1, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }
}
