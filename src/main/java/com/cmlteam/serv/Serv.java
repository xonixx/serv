package com.cmlteam.serv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

public class Serv {
  public static void main(String[] args) throws Exception {
    System.out.println("Starting");
    System.exit(0);
    HttpServer server = HttpServer.create(new InetSocketAddress(17777), 0);
    server.createContext("/dl", new MyHandler());
    server.setExecutor(null); // creates a default executor
    server.start();
  }

  static class MyHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
      System.out.println("new req");
      t.sendResponseHeaders(200, 0);
      OutputStream os = t.getResponseBody();
      //      os.write(response.getBytes());
      Path path = new File("/home/xonix/Downloads/Site (2).pdf").toPath();
      Files.copy(path, os);
      os.flush();
      os.close();
    }
  }
}
