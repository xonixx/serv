package com.cmlteam.serv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Serv {

  private static final int DEFAULT_PORT = 17777;
  private static final String UTILITY_NAME = "serv";
  private static final String OPTION_PORT = "port";
  private static final String OPTION_COMPRESS = "compress";

  public static void main(String[] args) throws Exception {

    Task task = parseTaskFromArgs(args);

    System.out.println("Starting " + task);
    //    System.exit(0);
    HttpServer server = HttpServer.create(new InetSocketAddress(task.servePort), 0);
    server.createContext("/dl", new FileServeHandler(task.file));
    server.setExecutor(null); // creates a default executor
    server.start();
  }

  private static Task parseTaskFromArgs(String[] args) {
    Options options = new Options();

    Option port =
        new Option("p", OPTION_PORT, true, "port to serve on (default = " + DEFAULT_PORT + ")");
    port.setRequired(false);
    options.addOption(port);

    Option compress =
        new Option("C", OPTION_COMPRESS, false, "enable compression (default = false)");
    compress.setRequired(false);
    options.addOption(compress);

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd;

    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      throw printHelpAndExit(e.getMessage(), options);
    }

    List<String> argList = cmd.getArgList();

    if (argList.isEmpty()) {
      throw printHelpAndExit("Provide at least 1 file/folder to serve", options);
    }

    File file = new File(argList.get(0));
    if (!file.exists()) {
      throw printHelpAndExit("File/folder doesn't exist", options);
    }

    return new Task(
        file,
        Integer.parseInt(cmd.getOptionValue(OPTION_PORT, "" + DEFAULT_PORT)),
        cmd.hasOption(OPTION_COMPRESS));
  }

  private static IllegalStateException printHelpAndExit(String message, Options options) {
    if (message != null) System.err.println(message);
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(UTILITY_NAME, options);
    System.exit(1);
    return new IllegalStateException();
  }

  static class FileServeHandler implements HttpHandler {
    private File file;

    public FileServeHandler(File file) {
      this.file = file;
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
      System.out.println(
          "["
              + t.getRemoteAddress().getAddress()
              + "] "
              + t.getRequestMethod()
              + " "
              + t.getRequestURI());
      t.sendResponseHeaders(200, 0);
      OutputStream os = t.getResponseBody();
      Path path = file.toPath();
      Files.copy(path, os);
      os.flush();
      os.close();
    }
  }
}
