package com.cmlteam.serv;

import com.cmlteam.util.Util;
import com.sun.net.httpserver.HttpExchange;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HttpHandlerListing extends HttpHandlerBase {

  private static final byte[] FOOTER = "</tbody></table>".getBytes(UTF_8);
  private static final byte[] UNABLE_TO_LIST_FILES = "Unable to list files".getBytes(UTF_8);
  private static final byte[] INVALID_FOLDER = "Invalid folder".getBytes(UTF_8);

  private final File[] files;

  HttpHandlerListing(File[] files) {
    this.files = sortedFilesArray(files);
  }

  @Override
  public void doHandle(HttpExchange httpExchange) throws IOException {
    log(httpExchange);
    httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
    try (OutputStream os = httpExchange.getResponseBody()) {
      FileRef ref = FileRef.of(httpExchange.getRequestURI(),files);
      if (ref.isRoot()) {
        if (isSingleFolder(files)) {
          showList(httpExchange, 0, null, files[0].listFiles(), os);
        } else {
          showList(httpExchange, -1, null, files, os);
        }
      } else {
        File refFolder = ref.resolve();
        if (refFolder != null) {
          File[] filesInFolder = refFolder.listFiles();
          if (filesInFolder != null) {
            showList(httpExchange, ref.fIdx, refFolder, filesInFolder, os);
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

  private void showList(
      HttpExchange httpExchange, int fIdx, File indexedFolder, File[] files, OutputStream os)
      throws IOException {

    httpExchange.sendResponseHeaders(200, 0);

    writeHeaderWithBackLink(os, fIdx, indexedFolder);
    List<File> filesList = Arrays.asList(files);

    filesList.sort(HttpHandlerListing::compareFiles);

    for (File file : filesList) {
      int fIdxFixed = fIdx == -1 ? filesList.indexOf(file) : fIdx;

      if (file.isDirectory()) {
        writeFolderRow(os, fIdxFixed, file);
      } else if (file.isFile()) {
        writeFileRow(os, fIdxFixed, file);
      } else {
        System.err.println("warning: not supported: " + file);
      }
    }
    writeFooter(os);
  }

  private void writeFooter(OutputStream os) throws IOException {
    os.write(FOOTER);
    writeStrings(
        os,
        new String[] {
          "<br><a target='_blank' href='",
          Constants.GITHUB + "'>",
          Constants.UTILITY_NAME + " " + Constants.getVersion() + "</a>"
        });
  }

  private void writeFileRow(OutputStream os, int fIdx, File file) throws IOException {
    String name = file.getName();
    String size = Util.renderFileSize(file.length());
    String dlParams = dlParams(fIdx, file);
    String download =
        String.format(
            "<a href=\"/dlRef%s\">↓ download</a> | <a href=\"/dlRef%s&z\">↓ compressed</a>",
            dlParams, dlParams);
    writeStrings(
        os,
        new String[] {
          "<tr><td>", name, "</td><td>", size, "</td><td class=center>", download, "</td></tr>"
        });
  }

  private void writeFolderRow(OutputStream os, int fIdx, File file) throws IOException {
    String name = file.getName();
    String dlParams = dlParams(fIdx, file);
    String download =
        String.format(
            "<a href=\"/dlRef%s\">↓ tar</a> | <a href=\"/dlRef%s&z\">↓ tar.gz</a>",
            dlParams, dlParams);
    writeStrings(
        os,
        new String[] {
          "<tr>",
          "<td><a href='/",
          dlParams,
          "'>",
          name,
          "</a></td>",
          "<td></td>",
          "<td class=center>",
          download,
          "</td>",
          "</tr>"
        });
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
        "/" + (isRoot ? "" : (isSingleFolder(files) ? "" : files[fIdx].getName() + "/") + relativePath(fIdx, indexedFolder));

    boolean upIsRoot = Arrays.asList(files).contains(indexedFolder);

    String download;
    String downloadZ;

    if (!isRoot) {
      String dlParams = dlParams(fIdx, indexedFolder);
      download = String.format("<a href='/dlRef%s'>↓ tar</a> | ", dlParams);
      downloadZ =
          String.format("<a href='/dlRef%s&z'>↓ tar.gz</a>", dlParams);
    } else {
      download = "<a href='/dl'>↓ tar</a> | ";
      downloadZ = "<a href='/dl?z'>↓ tar.gz</a>";
    }

    writeStrings(
        os,
        new String[] {
          "<style>table {",
          "  border-collapse: collapse;",
          "}",
          "table, table td, table th {",
          "  border: 1px solid #bbb;",
          "}",
          ".center { text-align:center }</style>",
          "<h1>Index of ",
          indexedFolderDisplayed,
          "&nbsp;<span style='font-size:.5em;font-weight:normal'>",
          download,
          downloadZ,
          "</span></h1>",
          isRoot
              ? "" /* no up link */
              : "<a class=\"up\" href='/"
                  + (upIsRoot ? "" : dlParams(fIdx, indexedFolder.getParentFile()))
                  + "'>↑ UP</a><br><br>",
          "<table>",
          "<thead>",
          "<tr>",
          "<th>Name</th>",
          "<th>Size</th>",
          "<th>Download</th>",
          "</tr>",
          "</thead>",
          "<tbody>"
        });
  }

  private String dlParams(int fIdx, File file) {
    String relativePath = relativePath(fIdx, file);
    boolean skipF0 = fIdx == 0 && isSingleFolder(files);
    return  skipF0 && "".equals(relativePath) ? "" :
            "?" + (skipF0 ? "" : "f=" + fIdx + "&") + "name=" + URLEncoder.encode(relativePath, UTF_8);
  }

  private void writeStrings(OutputStream os, String[] content) throws IOException {
    for (String s : content) {
      os.write(s.getBytes(UTF_8));
    }
  }
}
