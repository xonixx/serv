package com.cmlteam.serv;

import com.cmlteam.util.Util;
import com.sun.net.httpserver.HttpExchange;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static lombok.AccessLevel.PACKAGE;

@RequiredArgsConstructor(access = PACKAGE)
public class HttpHandlerListing extends HttpHandlerBase {

  private final File[] files;

  private static final byte[] FOOTER = "</tbody></table>".getBytes(UTF_8);
  private static final byte[] UNABLE_TO_LIST_FILES = "Unable to list files".getBytes(UTF_8);
  private static final byte[] INVALID_FOLDER = "Invalid folder".getBytes(UTF_8);

  @Override
  public void handle(HttpExchange httpExchange) throws IOException {
    log(httpExchange);
    httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
    httpExchange.sendResponseHeaders(200, 0);
    try (OutputStream os = httpExchange.getResponseBody()) {
      String param = getRequestParamForFolderNameToList(httpExchange.getRequestURI().toString());
      if (param == null) { // no param = top level
        showList(files, os);
      } else {
        File nextFolder = new File(param);
        if (isValidToAccess(nextFolder)) {
          File[] filesInFolder = nextFolder.listFiles();
          if (filesInFolder != null) {
            showList(filesInFolder, os);
          } else {
            os.write(UNABLE_TO_LIST_FILES);
          }
        } else {
          os.write(INVALID_FOLDER);
        }
      }
      os.flush();
    } finally {
      httpExchange.close();
    }
  }

  private void showList(File[] files, OutputStream os) throws IOException {
    writeHeaderWithBackLink(os, files[0]);
    List<File> filesList = Arrays.asList(files);

    // TODO human sort
    filesList.sort(
        (f1, f2) ->
            f1.isFile() && f2.isDirectory()
                ? 1
                : f1.isDirectory() && f2.isFile() ? -1 : f1.getName().compareTo(f2.getName()));

    for (File file : filesList) {
      if (file.isDirectory()) {
        writeFolderLink(os, file);
      } else if (file.isFile()) {
        writeFileName(os, file);
      } else {
        System.err.println(file.getName() + " is not supported");
      }
    }
    os.write(FOOTER);
  }

  private void writeFileName(OutputStream os, File file) throws IOException {
    String name = file.getName();
    String size = Util.renderFileSize(file.length());
    writeStrings(os, new String[] {"<tr><td>", name, "</td><td>", size, "</td></tr>"});
  }

  private void writeFolderLink(OutputStream os, File file) throws IOException {
    String name = file.getName();
    String escapedName = fileNameForLink(file);
    writeStrings(
        os,
        new String[] {
          "<tr>",
          "<td><a href='/listing?name=",
          escapedName,
          "'>",
          name,
          "</a></td>",
          "<td></td>",
          "</tr>"
        });
  }

  @SneakyThrows
  private String fileNameForLink(File file) {
    return URLEncoder.encode(file.getAbsolutePath(), UTF_8.name());
  }

  private void writeHeaderWithBackLink(OutputStream os, File file) throws IOException {
    File indexedFolder = file.getParentFile();
    String parentUrl = fileNameForLink(indexedFolder.getParentFile());
    writeStrings(
        os,
        new String[] {
          "<style>table {",
          "  border-collapse: collapse;",
          "}",
          "table, table td, table th {",
          "  border: 1px solid #bbb;",
          "}</style>",
          "<h1>Index of ",
          indexedFolder.getAbsolutePath(),
          "</h1>",
          "<a href='/listing?name=",
          parentUrl,
          "'>↑ UP</a>",
          "<br></br>",
          "<table>",
          "<thead>",
          "<tr>",
          "<th>Name</th>",
          "<th>Size</th>",
          "</tr>",
          "</thead>",
          "<tbody>"
        });
  }

  private void writeStrings(OutputStream os, String[] content) throws IOException {
    for (String s : content) {
      os.write(s.getBytes(UTF_8));
    }
  }

  @SneakyThrows
  private String getRequestParamForFolderNameToList(String url) {
    if (url.contains("?")) {
      String param = url.substring(url.indexOf("=") + 1);
      return URLDecoder.decode(param, UTF_8.name());
    }
    return null;
  }

  private boolean isValidToAccess(File file) {
    Path pathToCheck = file.toPath();
    if (!Files.exists(pathToCheck)) {
      return false;
    }
    for (File f : files) {
      Path rootDir = f.toPath().getParent();
      while (pathToCheck != null) {
        if (rootDir.equals(pathToCheck)) {
          return true;
        } else {
          pathToCheck = pathToCheck.getParent();
        }
      }
    }
    return false;
  }
}
