package com.cmlteam.serv;

import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpHandlerFilesList extends HttpHandlerBase {

    private final File[] files;
    private Deque<String> referers = new ArrayDeque<>();

    private String[] HEADER = {"<h1>Files List</h1>",
            "<br></br>",
            "<a href='/listing?name=back'>Go to upper level</a>",
            "<br></br>",
            "<table>",
                "<thead>",
                    "<tr>",
                        "<th>File or Folder Name</th>",
                        "<th>Size</th>",
                    "</tr>",
                "</thead>",
            "<tbody>"};
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
        switch (param) {
            case "toplevel":
                showList(files, os);
                break;
            case "back":
                if (referers.size() < 2) {
                    referers.clear();
                    showList(files, os);
                } else {
                    referers.pop();
                    nextFolder = new File (referers.peek());
                    showList(nextFolder.listFiles(), os);
                }
                break;
            default:
                referers.push(param);
                nextFolder = new File(param);
                showList(nextFolder.listFiles(), os);
        }
        os.flush();
        os.close();
    }

    private void showList(File[] folder, OutputStream os) throws IOException{
        for (String s: HEADER) {
            os.write(s.getBytes(StandardCharsets.UTF_8));
        }
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
        double dsize = file.length()/1000;
        String size = String.format("%5.2f", dsize) + "KB";
        String[] content = {"<tr>",
                                "<td>" + name + "</td>",
                                "<td>" + size + "</td>",
                            "</tr>"
        };
        for (String s: content) {
            os.write(s.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void writeClickableFolderName (OutputStream os, File file) throws IOException{
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

    private String findRequestParam (String url) {
        StringTokenizer st = new StringTokenizer(url, "=");
        if (st.countTokens() >=2) {
            st.nextToken();
            return st.nextToken().replace("$", "\\").replace("%20", " ");
        }
        return "toplevel";
    }
}
