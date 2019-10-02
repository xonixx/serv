package com.cmlteam.serv;

import com.cmlteam.util.Util;
import com.sun.net.httpserver.HttpExchange;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HttpHandlerListing extends HttpHandlerBase {

  private static final byte[] FOOTER = "</tbody></table>".getBytes(UTF_8);
  private static final byte[] UNABLE_TO_LIST_FILES = "Unable to list files".getBytes(UTF_8);
  private static final byte[] INVALID_FOLDER = "Invalid folder".getBytes(UTF_8);

  private final File[] files;

  HttpHandlerListing(File[] files) {
    List<File> fileList = Arrays.asList(files);
    fileList.sort(HttpHandlerListing::compareFiles);
    this.files = fileList.toArray(new File[0]);
  }

  @Override
  public void doHandle(HttpExchange httpExchange) throws IOException {
    log(httpExchange);
    httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
    try (OutputStream os = httpExchange.getResponseBody()) {
      FileRef ref = getRequestedFolderToList(httpExchange.getRequestURI());
      if (ref == null) { // no ref = top level
        showList(httpExchange, -1, null, files, os);
      } else {
        File nextFolder = ref.resolve();
        if (isValidToAccess(nextFolder)) {
          File[] filesInFolder = nextFolder.listFiles();
          if (filesInFolder != null) {
            showList(httpExchange, ref.fIdx, nextFolder, filesInFolder, os);
          } else {
            showNotFoundErr(httpExchange, os, UNABLE_TO_LIST_FILES);
          }
        } else {
          showNotFoundErr(httpExchange, os, INVALID_FOLDER);
        }
      }
      os.flush();
    }
  }

  private void showNotFoundErr(HttpExchange httpExchange, OutputStream os, byte[] err)
      throws IOException {
    httpExchange.sendResponseHeaders(404, 0);
    os.write(err);
  }

  // TODO human sort
  private static int compareFiles(File f1, File f2) {
    return f1.isFile() && f2.isDirectory()
        ? 1
        : f1.isDirectory() && f2.isFile() ? -1 : f1.getName().compareTo(f2.getName());
  }

  private void showList(
      HttpExchange httpExchange, int fIdx, File indexedFolder, File[] files, OutputStream os)
      throws IOException {

    httpExchange.sendResponseHeaders(200, 0);

    writeHeaderWithBackLink(os, fIdx, indexedFolder);
    List<File> filesList = Arrays.asList(files);

    filesList.sort(HttpHandlerListing::compareFiles);

    for (File file : filesList) {
      if (file.isDirectory()) {
        writeFolderRow(os, fIdx == -1 ? Arrays.asList(files).indexOf(file) : fIdx, file);
      } else if (file.isFile()) {
        writeFileRow(os, file);
      } else {
        System.err.println(file.getName() + " is not supported");
      }
    }
    os.write(FOOTER);
  }

  private void writeFileRow(OutputStream os, File file) throws IOException {
    String name = file.getName();
    String size = Util.renderFileSize(file.length());
    writeStrings(os, new String[] {"<tr><td>", name, "</td><td>", size, "</td></tr>"});
  }

  private void writeFolderRow(OutputStream os, int fIdx, File file) throws IOException {
    String name = file.getName();
    String escapedName = fileNameForLink(fIdx, file);
    writeStrings(
        os,
        new String[] {
          "<tr>",
          "<td><a href='/listing?f=" + fIdx + "&name=",
          escapedName,
          "'>",
          name,
          "</a></td>",
          "<td></td>",
          "</tr>"
        });
  }

  @SneakyThrows
  private String fileNameForLink(int fIdx, File file) {
    String relativePath = relativePath(fIdx, file);
    return URLEncoder.encode(relativePath, UTF_8.name());
  }

  @SneakyThrows
  private String relativePath(int fIdx, File file) {
    if (fIdx == -1) {
      return "/";
    }

    File root = files[fIdx];
    return root.toURI().relativize(file.toURI()).getPath();
  }

  private void writeHeaderWithBackLink(OutputStream os, int fIdx, File indexedFolder)
      throws IOException {
    boolean isRoot = indexedFolder == null;
    String indexedFolderDisplayed =
        "/" + (isRoot ? "" : files[fIdx].getName() + "/" + relativePath(fIdx, indexedFolder));

    String parentUrl = "";
    if (!isRoot) {
      File upFile = indexedFolder.getParentFile();
      parentUrl = fileNameForLink(fIdx, upFile);
    }
    boolean upIsRoot = Arrays.asList(files).contains(indexedFolder);
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
          indexedFolderDisplayed,
          "</h1>",
          isRoot
              ? "" /* no up link */
              : "<a class=\"up\" href='/listing"
                  + (upIsRoot ? "" : "?f=" + fIdx + "&name=" + parentUrl)
                  + "'>↑ UP</a><br><br>",
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
  private FileRef getRequestedFolderToList(URI uri) {
    Map<String, String> paramsMap = splitQuery(uri);
    String name = paramsMap.get("name");
    return name == null ? null : new FileRef(Integer.parseInt(paramsMap.get("f")), name);
  }

  @RequiredArgsConstructor
  private class FileRef {
    private final int fIdx;
    private final String name;

    File resolve() {
      return new File(files[fIdx], name);
    }
  }

  private boolean isValidToAccess(File file) {
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
