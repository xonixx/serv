package com.cmlteam.serv;

import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.IOException;

class HttpHandlerServeFileByLink extends HttpHandlerBase {
  private final File[] files;

  HttpHandlerServeFileByLink(File[] files) {
    this.files = sortedFilesArray(files);
  }

  @Override
  public void doHandle(HttpExchange httpExchange) throws IOException {
    FileRef ref = FileRef.fromUri(httpExchange.getRequestURI());
    File file = ref.resolve(files);
    if (file != null) {
      if (file.isFile()) {
        new HttpHandlerServeFile(file).doHandle(httpExchange);
      } else if (file.isDirectory()) {
        new HttpHandlerServeFilesTar(file.getName(), file.listFiles(), true)
            .doHandle(httpExchange);
      } else {
        HttpHandlerForStatus.NOT_FOUND.handle(httpExchange);
      }
    } else {
      HttpHandlerForStatus.NOT_FOUND.handle(httpExchange);
    }
  }
}
