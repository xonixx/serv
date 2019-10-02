package com.cmlteam.serv;

import com.sun.net.httpserver.HttpExchange;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
class HttpHandlerForStatus extends HttpHandlerBase {

  static HttpHandlerForStatus NOT_FOUND = new HttpHandlerForStatus(404);
  static HttpHandlerForStatus SERVER_ERROR = new HttpHandlerForStatus(500);

  final int code;

  @Override
  public void doHandle(HttpExchange httpExchange) throws IOException {
    httpExchange.sendResponseHeaders(code, 0);
  }
}
