package com.cmlteam.serv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class HttpHandlerWebInfoPage extends HttpHandlerBase {
  private String outputString;

  private final HttpHandler httpHandlerFavicon = new HttpHandlerFavicon();

  HttpHandlerWebInfoPage(String outputString) {
    this.outputString = outputString;
  }

  @Override
  public void doHandle(HttpExchange httpExchange) throws IOException {
    String requestUri = httpExchange.getRequestURI().toString();
    if (!"/".equals(requestUri)) {
      if ("/favicon.ico".equals(requestUri)) {
        httpHandlerFavicon.handle(httpExchange);
      } else {
        HttpHandlerForStatus.NOT_FOUND.handle(httpExchange);
      }
      return;
    }

    log(httpExchange);

    httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
    httpExchange.sendResponseHeaders(200, 0);
    try (OutputStream responseBody = httpExchange.getResponseBody()) {
      String[] strings = {
        "<pre><code>",
        outputString,
        "</pre></code><hr><a target='_blank' href='",
        Constants.GITHUB + "'>",
        Constants.UTILITY_NAME + " " + Constants.VERSION + "</a>"
      };
      for (String string : strings) {
        responseBody.write(string.getBytes(StandardCharsets.UTF_8));
      }
      responseBody.flush();
    } finally {
      httpExchange.close();
    }
  }
}
