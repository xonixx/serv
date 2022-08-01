package com.cmlteam.serv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;

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

  static boolean isSingleFolder(File[] files) {
    return files.length == 1 && files[0].isDirectory();
  }

  File[] sortedFilesArray(File[] files) {
    List<File> fileList = Arrays.asList(files);
    fileList.sort(HttpHandlerListing::compareFiles);
    files = fileList.toArray(new File[0]);
    return files;
  }

  @RequiredArgsConstructor
  @ToString
  static class FileRef {
    static final int ROOT_IDX = -1;
    /**
     * The index of a file/folder in CLI args list, ex. `serv file1 dir2 dir3`.
     * In case of `serv dir` the files in dir are listed.
     */
    final int fIdx;
    /**
     * The file path relative to dir set by `fIdx`
     */
    final String name; // TODO rename to path

    final File[] files;

    @SneakyThrows
    @NonNull
    static FileRef of(URI uri, File[] files) {
      QueryParser queryParser = new QueryParser(uri);
      String name = queryParser.getParam("name");
      String f = queryParser.getParam("f");
      return new FileRef(f == null ? (!"".equals(name) && name != null ? 0 : ROOT_IDX) : Integer.parseInt(f), name, files);
    }

    boolean isRoot() {
      return ROOT_IDX == fIdx ||
              ("".equals(name) || name == null) && isSingleFolder(files);
    }

    /**
     * @return resolved file or null (if file path is invalid or outside the scope of shared
     *     folders)
     */
    File resolve() {
      File file = new File(files[fIdx], name);
      return isValidToAccess(file) ? file : null;
    }

    private boolean isValidToAccess(File file) {
      Path pathToCheck = file.toPath();
      if (!Files.exists(pathToCheck)) {
        return false;
      }
      return Arrays.stream(files).anyMatch(sharedFile -> !sharedFile.toPath().relativize(pathToCheck).toString().contains(".."));
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
