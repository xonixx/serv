package com.cmlteam.serv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;

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
    String requestURI = httpExchange.getRequestURI().toString();
    return requestURI.endsWith("?z");
  }

  static String escapeFileName(File file) {
    return file.getName().replace('"', '\'');
  }
}
