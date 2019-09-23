package com.cmlteam.serv;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

class HttpHandler404 extends HttpHandlerBase {
  @Override
  public void doHandle(HttpExchange httpExchange) throws IOException {
    httpExchange.sendResponseHeaders(404, 0);
    httpExchange.close();
  }
}
