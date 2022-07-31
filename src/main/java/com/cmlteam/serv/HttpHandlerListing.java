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
      FileRef ref = FileRef.of(httpExchange.getRequestURI(), files);
      if (ref.isRoot()) {
        if (files.length == 1 && files[0].isDirectory()) {
          showList(httpExchange, ref, null, files[0].listFiles(), os);
        } else {
          showList(httpExchange, ref, null, files, os);
        }
      } else {
        File refFolder = ref.resolve();
        if (refFolder != null) {
          File[] filesInFolder = refFolder.listFiles();
          if (filesInFolder != null) {
            showList(httpExchange, ref, refFolder, filesInFolder, os);
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
          HttpExchange httpExchange, FileRef ref, File indexedFolder, File[] files, OutputStream os)
      throws IOException {

    httpExchange.sendResponseHeaders(200, 0);

    writeHeaderWithBackLink(os, ref, indexedFolder);
    List<File> filesList = Arrays.asList(files);

    filesList.sort(HttpHandlerListing::compareFiles);

    for (File file : filesList) {
//      FileRef ref1 = ref.isRoot() ? new FileRef(filesList.indexOf(file),"",files) : ref;

      if (file.isDirectory()) {
        writeFolderRow(os, ref, file);
      } else if (file.isFile()) {
        writeFileRow(os, ref, file);
      } else {
        System.err.println(file.getName() + " is not supported");
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

  private void writeFileRow(OutputStream os, FileRef ref, File file) throws IOException {
    String name = file.getName();
    String size = Util.renderFileSize(file.length());
    String escapedName = fileNameForLink(ref, file);
    String download =
        String.format(
            "<a href=\"/dlRef?f=%d&name=%s\">↓ download</a> | <a href=\"/dlRef?f=%d&name=%s&z\">↓ compressed</a>",
            ref.fIdx, escapedName, ref.fIdx, escapedName);
    writeStrings(
        os,
        new String[] {
          "<tr><td>", name, "</td><td>", size, "</td><td class=center>", download, "</td></tr>"
        });
  }

  private void writeFolderRow(OutputStream os, FileRef ref, File file) throws IOException {
    String name = file.getName();
    String escapedName = fileNameForLink(ref, file);
    String download =
        String.format(
            "<a href=\"/dlRef?f=%d&name=%s\">↓ tar</a> | <a href=\"/dlRef?f=%d&name=%s&z\">↓ tar.gz</a>",
            ref.fIdx, escapedName, ref.fIdx, escapedName);
    writeStrings(
        os,
        new String[] {
          "<tr>",
          "<td><a href='/?f=" + ref.fIdx + "&name=",
          escapedName,
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
  private String fileNameForLink(FileRef ref, File file) {
    String relativePath = relativePath(ref, file);
    return URLEncoder.encode(relativePath, UTF_8);
  }

  @SneakyThrows
  private String relativePath(FileRef ref, File file) {
    if (ref.isRoot()) {
      return "/";
    }

    File root = files[ref.fIdx];
    return root.toURI().relativize(file.toURI()).getPath();
  }

  private void writeHeaderWithBackLink(OutputStream os, FileRef ref, File indexedFolder)
      throws IOException {
    boolean isRoot = indexedFolder == null;
    String indexedFolderDisplayed =
        "/" + (isRoot ? "" : files[ref.fIdx].getName() + "/" + relativePath(ref, indexedFolder));

    String parentUrl = "";
    if (!isRoot) {
      File upFile = indexedFolder.getParentFile();
      parentUrl = fileNameForLink(ref, upFile);
    }

    String download;
    String downloadZ;

    if (!isRoot) {
      String escapedName = fileNameForLink(ref, indexedFolder);
      download = String.format("<a href=\"/dlRef?f=%d&name=%s\">↓ tar</a> | ", ref.fIdx, escapedName);
      downloadZ =
          String.format("<a href=\"/dlRef?f=%d&name=%s&z\">↓ tar.gz</a>", ref.fIdx, escapedName);
    } else {
      download = "<a href=\"/dl\">↓ tar</a> | ";
      downloadZ = "<a href=\"/dl?z\">↓ tar.gz</a>";
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
                  + (upIsRoot ? "" : "?f=" + ref.fIdx + "&name=" + parentUrl)
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

  private void writeStrings(OutputStream os, String[] content) throws IOException {
    for (String s : content) {
      os.write(s.getBytes(UTF_8));
    }
  }
}
