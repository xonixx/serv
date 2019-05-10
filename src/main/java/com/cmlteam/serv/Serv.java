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

public class Serv {
  public static void main(String[] args) throws Exception {

    Options options = new Options();

    Option input = new Option("i", "input", true, "input file path");
    input.setRequired(true);
    options.addOption(input);

    Option output = new Option("o", "output", true, "output file");
    output.setRequired(true);
    options.addOption(output);

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();
    CommandLine cmd = null;

    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      formatter.printHelp("utility-name", options);

      System.exit(1);
    }

    String inputFilePath = cmd.getOptionValue("input");
    String outputFilePath = cmd.getOptionValue("output");

    System.out.println(inputFilePath);
    System.out.println(outputFilePath);

    System.out.println("Starting");
//    System.exit(0);
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
