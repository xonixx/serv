package com.cmlteam.serv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

abstract class HttpHandlerBase implements HttpHandler {
  static void log(HttpExchange httpExchange) {
    System.out.println(
        "["
            + httpExchange.getRemoteAddress().getAddress().getHostAddress()
            + "] "
            + httpExchange.getRequestMethod()
            + " "
            + httpExchange.getRequestURI());
  }

  static boolean isCompressed(HttpExchange httpExchange) {
    return splitQuery(httpExchange.getRequestURI()).containsKey("z");
  }

  static String escapeFileName(File file) {
    return file.getName().replace('"', '\'');
  }

  @SneakyThrows
  static Map<String, String> splitQuery(URI uri) {
    Map<String, String> query_pairs = new LinkedHashMap<>();
    String query = uri.getQuery();
    if (query != null) {
      String[] pairs = query.split("&");
      if (pairs.length == 1) {
        query_pairs.put(pairs[0], null);
      } else {
        for (String pair : pairs) {
          int idx = pair.indexOf("=");
          query_pairs.put(
              URLDecoder.decode(pair.substring(0, idx), UTF_8.name()),
              URLDecoder.decode(pair.substring(idx + 1), UTF_8.name()));
        }
      }
    }
    return query_pairs;
  }

  // TODO human sort
  static int compareFiles(File f1, File f2) {
    return f1.isFile() && f2.isDirectory()
        ? 1
        : f1.isDirectory() && f2.isFile() ? -1 : f1.getName().compareTo(f2.getName());
  }

  File[] sortedFilesArray(File[] files) {
    List<File> fileList = Arrays.asList(files);
    fileList.sort(HttpHandlerListing::compareFiles);
    files = fileList.toArray(new File[0]);
    return files;
  }

  @SneakyThrows
  static FileRef getRequestedFileRef(URI uri) {
    Map<String, String> paramsMap = splitQuery(uri);
    String name = paramsMap.get("name");
    return name == null ? null : new FileRef(Integer.parseInt(paramsMap.get("f")), name);
  }

  @RequiredArgsConstructor
  static class FileRef {
    final int fIdx;
    final String name;

    /**
     * @return resolved file or null (if file path is invalid or outside the scope of shared
     *     folders)
     */
    File resolve(File[] files) {
      File file = new File(files[fIdx], name);
      return isValidToAccess(files, file) ? file : null;
    }

    private boolean isValidToAccess(File[] files, File file) {
      Path pathToCheck = file.toPath();
      if (!Files.exists(pathToCheck)) {
        return false;
      }
      for (File sharedFile : files) {
        Path sharedDir = sharedFile.toPath();
        Path pathToCheckCurrent = pathToCheck;
        while (pathToCheckCurrent != null) {
          if (sharedDir.equals(pathToCheckCurrent)) {
            return true;
          } else {
            pathToCheckCurrent = pathToCheckCurrent.getParent();
          }
        }
      }
      return false;
    }
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    try {
      doHandle(exchange);
    } catch (Exception ex) {
      ex.printStackTrace();
      HttpHandlerForStatus.SERVER_ERROR.doHandle(exchange);
    } finally {
      exchange.close();
    }
  }

  protected abstract void doHandle(HttpExchange exchange) throws IOException;
}
