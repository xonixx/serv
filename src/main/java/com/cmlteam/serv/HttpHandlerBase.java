package com.cmlteam.serv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

abstract class HttpHandlerBase implements HttpHandler {
  static void log(HttpExchange httpExchange) {
    System.out.println(
        "["
            + httpExchange.getRemoteAddress().getAddress().getHostAddress()
            + "] "
            + httpExchange.getRequestMethod()
            + " "
            + httpExchange.getRequestURI());
  }

  static boolean isCompressed(HttpExchange httpExchange) {
    return splitQuery(httpExchange.getRequestURI()).containsKey("z");
  }

  static String escapeFileName(File file) {
    return file.getName().replace('"', '\'');
  }

  @SneakyThrows
  static Map<String, String> splitQuery(URI uri) {
    Map<String, String> query_pairs = new LinkedHashMap<>();
    String query = uri.getQuery();
    if (query != null) {
      String[] pairs = query.split("&");
      if (pairs.length == 1) {
        query_pairs.put(pairs[0], null);
      } else {
        for (String pair : pairs) {
          int idx = pair.indexOf("=");
          query_pairs.put(
              URLDecoder.decode(pair.substring(0, idx), UTF_8.name()),
              URLDecoder.decode(pair.substring(idx + 1), UTF_8.name()));
        }
      }
    }
    return query_pairs;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    try {
      doHandle(exchange);
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      exchange.close();
    }
  }

  protected abstract void doHandle(HttpExchange exchange) throws IOException;
}
