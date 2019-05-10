package com.cmlteam.serv;

import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

class ServeHandlerFile extends ServeHandlerBase {
  private File file;

  ServeHandlerFile(File file) {
    if (file.isDirectory())
      throw new IllegalArgumentException("Should be file, folder given: " + file);
    this.file = file;
  }

  @Override
  public void handle(HttpExchange httpExchange) throws IOException {
    log(httpExchange);
    httpExchange.sendResponseHeaders(200, 0);
    OutputStream os = httpExchange.getResponseBody();
    Path path = file.toPath();
    Files.copy(path, os);
    os.flush();
    os.close();
  }
}
