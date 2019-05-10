package com.cmlteam.serv;

import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

class ServeHandlerFolder extends ServeHandlerBase {
  private File folder;

  ServeHandlerFolder(File folder) {
    if (!folder.isDirectory())
      throw new IllegalArgumentException("Should be folder, file given: " + folder);
    this.folder = folder;
  }

  @Override
  public void handle(HttpExchange httpExchange) throws IOException {
    log(httpExchange);
    httpExchange.sendResponseHeaders(200, 0);
    OutputStream os = httpExchange.getResponseBody();
    TarUtil.compress(os, folder);
    os.flush();
    os.close();
  }
}
