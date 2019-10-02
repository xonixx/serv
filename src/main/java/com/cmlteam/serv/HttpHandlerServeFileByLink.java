package com.cmlteam.serv;

import com.sun.net.httpserver.HttpExchange;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

class HttpHandlerServeFileByLink extends HttpHandlerBase {
  private final File[] files;

  HttpHandlerServeFileByLink(File[] files) {
    this.files = sortedFilesArray(files);
  }

  @Override
  public void doHandle(HttpExchange httpExchange) throws IOException {
    log(httpExchange);
    boolean isCompress = isCompressed(httpExchange);
    //    Map<String, String> params = splitQuery(httpExchange.getRequestURI());
    //    int f = Integer.parseInt(params.get("f"));
    //    String f = params.get("f");

    FileRef ref = getRequestedFileRef(httpExchange.getRequestURI());
    if (ref != null) {
      File file = ref.resolve(files);
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
      }
    } else {
      HttpHandlerForStatus.NOT_FOUND.handle(httpExchange);
    }
  }
}
