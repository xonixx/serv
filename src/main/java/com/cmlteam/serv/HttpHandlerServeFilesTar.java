package com.cmlteam.serv;

import com.sun.net.httpserver.HttpExchange;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

@RequiredArgsConstructor
class HttpHandlerServeFilesTar extends HttpHandlerBase {
  private final String folderName;
  private final File[] files;
  private final boolean includeVcsFiles;

  @Override
  public void doHandle(HttpExchange httpExchange) throws IOException {
    log(httpExchange);
    boolean isCompress = isCompressed(httpExchange);
    httpExchange
        .getResponseHeaders()
        .add(
            "Content-Disposition",
            "attachment; filename=\""
                + folderName
                + ".tar"
                + (isCompress ? ".gz" : "")
                + "\""); // TODO should we escape the folder name?
    httpExchange.sendResponseHeaders(200, 0);

    try (OutputStream os = httpExchange.getResponseBody()) {
      TarUtil.compress(
          os,
          files,
          TarOptions.builder().compress(isCompress).excludeVcs(!includeVcsFiles).build());
    }
  }
}
