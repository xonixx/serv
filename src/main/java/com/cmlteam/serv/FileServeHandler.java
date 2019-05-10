package com.cmlteam.serv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

class FileServeHandler implements HttpHandler {
  private File file;

  public FileServeHandler(File file) {
    this.file = file;
  }

  @Override
  public void handle(HttpExchange t) throws IOException {
    System.out.println(
        "["
            + t.getRemoteAddress().getAddress().getHostAddress()
            + "] "
            + t.getRequestMethod()
            + " "
            + t.getRequestURI());
    t.sendResponseHeaders(200, 0);
    OutputStream os = t.getResponseBody();
    Path path = file.toPath();
    Files.copy(path, os);
    os.flush();
    os.close();
  }
}
