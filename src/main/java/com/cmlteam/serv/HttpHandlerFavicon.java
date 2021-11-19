package com.cmlteam.serv;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;

class HttpHandlerFavicon extends HttpHandlerBase {
  @Override
  public void doHandle(HttpExchange httpExchange) throws IOException {
    httpExchange.getResponseHeaders().add("Content-Type", "image/x-icon");
    httpExchange.sendResponseHeaders(200, 0);
    InputStream faviconInputStream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("./favicon.ico");
    faviconInputStream.transferTo(httpExchange.getResponseBody());
    httpExchange.getResponseBody().flush();
  }
}
