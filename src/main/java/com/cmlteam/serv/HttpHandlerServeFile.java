package com.cmlteam.serv;

import com.sun.net.httpserver.HttpExchange;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

class HttpHandlerServeFile extends HttpHandlerBase {
  private File file;
  private boolean isCompress;

  HttpHandlerServeFile(File file, boolean isCompress) {
    if (file.isDirectory())
      throw new IllegalArgumentException("Should be file, folder given: " + file);
    this.file = file;
    this.isCompress = isCompress;
  }

  @Override
  public void handle(HttpExchange httpExchange) throws IOException {
    log(httpExchange);
    httpExchange.sendResponseHeaders(200, 0);
    if (isCompress) {
      httpExchange.getResponseHeaders().add("Content-Encoding", "gzip");
    }
    OutputStream outputStream = httpExchange.getResponseBody();
    Path path = file.toPath();
    if (isCompress) {
      outputStream = new GzipCompressorOutputStream(outputStream);
    }
    Files.copy(path, outputStream);
    outputStream.flush();
    outputStream.close();
  }
}
