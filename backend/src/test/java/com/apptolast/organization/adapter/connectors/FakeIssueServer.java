package com.apptolast.organization.adapter.connectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Servidor falso del gestor de issues sobre el HTTP del JDK, en loopback y puerto efímero. Ninguna
 * prueba de este carril habla con api.github.com ni con gitlab.com: la base apunta siempre aquí.
 */
final class FakeIssueServer implements AutoCloseable {
  /**
   * Lo que la petición traía, para poder auditarlo después. {@code path} es la ruta tal cual llegó,
   * sin descodificar, que es la única forma de comprobar que lo que sale codificado llega
   * codificado.
   */
  record Received(String method, String path, String query, Map<String, String> headers) {}

  private final HttpServer server;
  private final List<Received> received = new CopyOnWriteArrayList<>();
  private final Map<String, Reply> replies = new LinkedHashMap<>();
  private volatile long bodyDelayMillis;

  record Reply(int status, String body, Map<String, String> headers) {
    static Reply ok(String body) {
      return new Reply(200, body, Map.of());
    }

    static Reply status(int status, Map<String, String> headers) {
      return new Reply(status, "{}", headers);
    }
  }

  FakeIssueServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext("/", this::handle);
    server.start();
  }

  String base() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  void reply(String path, Reply reply) {
    replies.put(path, reply);
  }

  void delayBody(long millis) {
    bodyDelayMillis = millis;
  }

  List<Received> received() {
    return List.copyOf(received);
  }

  private void handle(HttpExchange exchange) throws IOException {
    var uri = exchange.getRequestURI();
    var headers = new LinkedHashMap<String, String>();
    exchange
        .getRequestHeaders()
        .forEach(
            (name, values) ->
                headers.put(name.toLowerCase(java.util.Locale.ROOT), values.getFirst()));
    received.add(
        new Received(exchange.getRequestMethod(), uri.getRawPath(), uri.getRawQuery(), headers));
    var reply = replies.getOrDefault(uri.getRawPath(), new Reply(404, "{}", Map.of()));
    reply.headers().forEach((name, value) -> exchange.getResponseHeaders().add(name, value));
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    var body = reply.body().getBytes(StandardCharsets.UTF_8);
    if (bodyDelayMillis > 0) {
      exchange.sendResponseHeaders(reply.status(), 0);
      try {
        Thread.sleep(bodyDelayMillis);
      } catch (InterruptedException error) {
        Thread.currentThread().interrupt();
      }
      exchange.close();
      return;
    }
    exchange.sendResponseHeaders(reply.status(), body.length);
    try (var out = exchange.getResponseBody()) {
      out.write(body);
    }
  }

  /** Array JSON de issues de GitHub con identificadores consecutivos. */
  static String githubIssues(int from, int count) {
    var items = new ArrayList<String>();
    for (int n = from; n < from + count; n++) items.add(githubIssue(n, "Issue " + n, null));
    return "[" + String.join(",", items) + "]";
  }

  static String githubIssue(int id, String title, String body) {
    return "{\"id\":"
        + id
        + ",\"title\":\""
        + title
        + "\",\"body\":"
        + (body == null ? "null" : "\"" + body + "\"")
        + ",\"html_url\":\"https://github.com/octocat/Hello-World/issues/"
        + id
        + "\"}";
  }

  @Override
  public void close() {
    server.stop(0);
  }
}
