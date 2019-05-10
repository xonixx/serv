package com.cmlteam.serv;

import com.sun.net.httpserver.HttpServer;
import org.apache.commons.cli.*;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;

public class Serv {

  private static final int DEFAULT_PORT = 17777;
  private static final String UTILITY_NAME = "serv";
  private static final String OPTION_PORT = "port";
  private static final String OPTION_COMPRESS = "compress";

  public static void main(String[] args) throws Exception {

    Task task = parseTaskFromArgs(args);

    //    System.exit(0);

    String serveIp = IpUtil.getLocalNetworkIp();

    //    System.out.println("Starting " + task);

    String url = "http://" + serveIp + ":" + task.servePort + "/dl";
    File file = task.file;
    boolean isFolder = file.isDirectory();

    if (isFolder) {
      System.out.println("To download the files please use commands below. ");
      System.out.println("NB! All files will be created in current folder.");
      System.out.println();
      System.out.println("curl " + url + " | tar -xvf -");
      System.out.println(" -or-");
      System.out.println("wget -O- " + url + " | tar -xvf -");
    } else {
      System.out.println("To download the file please use: ");
      System.out.println();
      System.out.println("curl " + url + " > '" + file.getName() + "'");
      System.out.println(" -or-");
      System.out.println("wget -O- " + url + " > '" + file.getName() + "'");
    }

    HttpServer server = HttpServer.create(new InetSocketAddress(serveIp, task.servePort), 0);
    server.createContext(
        "/dl", isFolder ? new ServeHandlerFolder(file) : new ServeHandlerFile(file));
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
}
