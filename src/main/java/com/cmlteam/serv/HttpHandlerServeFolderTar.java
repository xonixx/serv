package com.cmlteam.serv;

import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

class HttpHandlerServeFolderTar extends HttpHandlerBase {
  private File folder;
  private boolean compress;

  HttpHandlerServeFolderTar(File folder, boolean compress) {
    if (!folder.isDirectory())
      throw new IllegalArgumentException("Should be folder, file given: " + folder);
    this.compress = compress;
    this.folder = folder;
  }

  @Override
  public void handle(HttpExchange httpExchange) throws IOException {
    log(httpExchange);
    httpExchange.sendResponseHeaders(200, 0);
    OutputStream os = httpExchange.getResponseBody();
    TarUtil.compress(os, folder, compress);
    os.flush();
    os.close();
  }
}
