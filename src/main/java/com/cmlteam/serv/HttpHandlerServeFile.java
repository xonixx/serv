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

  HttpHandlerServeFile(File file) {
    if (file.isDirectory())
      throw new IllegalArgumentException("Should be file, folder given: " + file);
    this.file = file;
  }

  @Override
  public void handle(HttpExchange httpExchange) throws IOException {
    log(httpExchange);
    boolean isCompress = isCompressed(httpExchange);
    if (isCompress) {
      httpExchange.getResponseHeaders().add("Content-Encoding", "gzip");
    }
    httpExchange
        .getResponseHeaders()
        .add("Content-Disposition", "attachment; filename=\"" + escapeFileName(file) + "\"");
    httpExchange.sendResponseHeaders(200, 0);
    OutputStream _outputStream = httpExchange.getResponseBody();
    Path path = file.toPath();
    if (isCompress) {
      _outputStream = new GzipCompressorOutputStream(_outputStream);
    }
    try (OutputStream outputStream = _outputStream) {
      Files.copy(path, outputStream);
      outputStream.flush();
    } finally {
      httpExchange.close();
    }
  }
}
