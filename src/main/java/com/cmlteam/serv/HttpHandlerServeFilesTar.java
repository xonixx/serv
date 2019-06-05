package com.cmlteam.serv;

import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

class HttpHandlerServeFilesTar extends HttpHandlerBase {
  private final File[] files;
  private final boolean includeVcsFiles;

  HttpHandlerServeFilesTar(File[] files, boolean includeVcsFiles) {
    this.files = files;
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
            "attachment; filename=\"files.tar" + (isCompress ? ".gz" : "") + "\"");
    httpExchange.sendResponseHeaders(200, 0);

    OutputStream os = httpExchange.getResponseBody();

    TarUtil.compress(
        os, files, TarOptions.builder().compress(isCompress).excludeVcs(!includeVcsFiles).build());
    os.flush();
    os.close();
  }
}
