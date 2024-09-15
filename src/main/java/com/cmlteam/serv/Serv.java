package com.cmlteam.serv;

import com.sun.net.httpserver.HttpServer;
import lombok.SneakyThrows;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Set;

public class Serv {

  private final HttpServer server;

  public Serv(String... args) {
    this(getCommand(args));
  }

  public Serv(Command command) {
    server = createServerWithContext(command, command.files);
    server.setExecutor(null); // creates a default executor
    server.start();
  }

  private static Command getCommand(String[] args) {
    Command command = Command.fromArgs(args);
    System.out.println(command.getHelpString());
    return command;
  }

  void stop() {
    server.stop(0);
  }

  InetSocketAddress getAddress() {
    return server.getAddress();
  }

  public static void main(String[] args) {
    new Serv(args);
  }

  @SneakyThrows
  private HttpServer createServerWithContext(Command command, Set<File> files) {
    String serveIp = command.serveHost != null ? command.serveHost : IpUtil.getLocalNetworkIp();
    HttpServer server = HttpServer.create(new InetSocketAddress(serveIp, command.servePort), 0);
    server.createContext("/favicon.ico", new HttpHandlerFavicon());
    if (files.size() == 1) {
      File file = files.iterator().next();
      if (file.isDirectory()) {
        server.createContext(
            "/dl", new HttpHandlerServeFilesTar(file.getName(), file.listFiles(), command.includeVcsFiles));
        serveListingWebUi(server, files.toArray(new File[0]));
      } else {
        server.createContext("/dl", new HttpHandlerServeFile(file));
      }
    } else {
      File[] filesArr = files.toArray(new File[0]);
      server.createContext(
          "/dl", new HttpHandlerServeFilesTar("files", filesArr, command.includeVcsFiles));
      serveListingWebUi(server, filesArr);
    }
    return server;
  }

  private static void serveListingWebUi(HttpServer server, File[] filesArr) {
    server.createContext("/", new HttpHandlerListing(filesArr));
    server.createContext("/dlRef", new HttpHandlerServeFileByLink(filesArr));
  }
}
