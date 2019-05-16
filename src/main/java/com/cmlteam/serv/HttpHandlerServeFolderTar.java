package com.cmlteam.serv;

import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

class HttpHandlerServeFolderTar extends HttpHandlerBase {
  private final File folder;
  private final boolean includeVcsFiles;

  HttpHandlerServeFolderTar(File folder, boolean includeVcsFiles) {
    if (!folder.isDirectory())
      throw new IllegalArgumentException("Should be folder, file given: " + folder);
    this.folder = folder;
    this.includeVcsFiles = includeVcsFiles;
  }

  @Override
  public void handle(HttpExchange httpExchange) throws IOException {
    log(httpExchange);
    boolean isCompress = isCompressed(httpExchange);
    httpExchange
        .getResponseHeaders()
        .add(
            "Content-Disposition",
            "attachment; filename=\"folder.tar" + (isCompress ? ".gz" : "") + "\"");
    httpExchange.sendResponseHeaders(200, 0);
    OutputStream os = httpExchange.getResponseBody();
    TarUtil.compress(
        os, folder, TarOptions.builder().compress(isCompress).excludeVcs(!includeVcsFiles).build());
    os.flush();
    os.close();
  }
}
