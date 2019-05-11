package com.cmlteam.serv;

import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

class HttpHandlerServeFolderTar extends HttpHandlerBase {
  private File folder;

  HttpHandlerServeFolderTar(File folder) {
    if (!folder.isDirectory())
      throw new IllegalArgumentException("Should be folder, file given: " + folder);
    this.folder = folder;
  }

  @Override
  public void handle(HttpExchange httpExchange) throws IOException {
    log(httpExchange);
    boolean isCompress = isCompressed(httpExchange);
    httpExchange.sendResponseHeaders(200, 0);
    OutputStream os = httpExchange.getResponseBody();
    TarUtil.compress(os, folder, isCompress);
    os.flush();
    os.close();
  }
}
