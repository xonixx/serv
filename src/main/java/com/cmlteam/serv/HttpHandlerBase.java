package com.cmlteam.serv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

abstract class HttpHandlerBase implements HttpHandler {
  void log(HttpExchange httpExchange) {
    System.out.println(
        "["
            + httpExchange.getRemoteAddress().getAddress().getHostAddress()
            + "] "
            + httpExchange.getRequestMethod()
            + " "
            + httpExchange.getRequestURI());
  }

  boolean isCompressed(HttpExchange httpExchange) {
    String requestURI = httpExchange.getRequestURI().toString();
    return requestURI.endsWith("?z");
  }
}
