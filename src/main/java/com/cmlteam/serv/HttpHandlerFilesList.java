package com.cmlteam.serv;

import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class HttpHandlerFilesList extends HttpHandlerBase {

    private final File[] files;

    private final String[] FOOTER = {"</tbody>",
            "</table>"
    };

    public HttpHandlerFilesList(File[] files) {
        this.files = files;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        log(httpExchange);
        httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        httpExchange.sendResponseHeaders(200, 0);
        OutputStream os = httpExchange.getResponseBody();
        String param = findRequestParam(httpExchange.getRequestURI().toString());
        File nextFolder;
        if ("toplevel".equals(param)) {
            showList(files, os);
        } else {
            nextFolder = new File(param);
            if (isValid(nextFolder)) {
                showList(nextFolder.listFiles(), os);
            } else {
                showList(files, os);
            }
        }
        os.flush();
        os.close();
    }

    private void showList(File[] folder, OutputStream os) throws IOException{
        writeHeaderWithBackLink(os, folder[0]);
        for (File file: folder) {
            if (file.isDirectory()) {
                writeClickableFolderName(os, file);
            }
            else if (file.isFile()) {
                writeNonClickableFileName(os, file);
            } else {
                System.out.println(file.getName() + " is not supported");
            }
        }
        for (String str: FOOTER) {
            os.write(str.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void writeNonClickableFileName (OutputStream os, File file) throws IOException{
        String name = file.getName();
        String size = "";
        if (file.length() < 1000) {
            size = file.length() + " B";
        } else if (file.length() < 1000000) {
            double dsize = file.length() / 1000;
            size = String.format("%5.2f", dsize) + " KB";
        } else if (file.length() < 1000000000) {
            double dsize = file.length() / 1000000;
            size = String.format("%5.2f", dsize) + " MB";
        } else {
            double dsize = file.length() / 1000000000;
            size = String.format("%5.2f", dsize) + " GB";
        }
        String[] content = {"<tr>",
                "<td>" + name + "</td>",
                "<td>" + size + "</td>",
                "</tr>"
        };
        for (String s: content) {
            os.write(s.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void writeClickableFolderName (OutputStream os, File file) throws IOException {
        String name = file.getName();
        String convertedName = file.getAbsolutePath().replace("\\", "$" );
        String[] content = {"<tr>",
                "<td><a href='/listing?name=" + convertedName + "'>" + name + "</a></td>",
                "<td>---</td>",
                "</tr>"
        };
        for (String s: content) {
            os.write(s.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void writeHeaderWithBackLink (OutputStream os, File file) throws IOException {
        String url = file.getParentFile().getParentFile().getAbsolutePath().replace("\\", "$");
        String content[] = {"<h1>Files List</h1>",
                "<br></br>",
                "<a href='/listing?name=" + url + "'>Go to upper level</a>",
                "<br></br>",
                "<table>",
                "<thead>",
                "<tr>",
                "<th>File or Folder Name</th>",
                "<th>Size</th>",
                "</tr>",
                "</thead>",
                "<tbody>"};
        for (String s: content) {
            os.write(s.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String findRequestParam (String url) {
        if  (url.contains("?")) {
            String param = url.substring(url.indexOf("=") + 1);
            return param.replace("$", "\\").replace("%20", " ");
        }
        return "toplevel";
    }

    private boolean isValid (File file) {
        Path pathToCheck = file.toPath();
        if (!Files.exists(pathToCheck)) {
            return false;
        }
        // startsWith?
        for (File f: files) {
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

