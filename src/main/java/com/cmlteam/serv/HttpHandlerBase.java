package com.cmlteam.serv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

abstract class HttpHandlerBase implements HttpHandler {
  void log(HttpExchange httpExchange) {
    System.out.println(
        "["
            + httpExchange.getRemoteAddress().getAddress().getHostAddress()
            + "] "
            + httpExchange.getRequestMethod()
            + " "
            + httpExchange.getRequestURI()
            + " ("
            + getClass().getSimpleName()
            + ")");
  }

  static boolean isCompressed(HttpExchange httpExchange) {
    return new QueryParser(httpExchange.getRequestURI()).hasParam("z");
  }

  static String escapeFileName(File file) {
    return file.getName().replace('"', '\'');
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

  @RequiredArgsConstructor
  static class FileRef {
    /**
     * The index of a file/folder in CLI args list, ex. `serv file1 dir2 dir3`.
     * In case of `serv dir` the files in dir are listed.
     */
    final int fIdx;
    /**
     * The file path relative to dir set by `fIdx`
     */
    final String name;

    @SneakyThrows
    static FileRef fromUri(URI uri) {
      QueryParser queryParser = new QueryParser(uri);
      String name = queryParser.getParam("name");
      return name == null ? null : new FileRef(Integer.parseInt(queryParser.getParam("f")), name);
    }

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
