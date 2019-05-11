package com.cmlteam.serv;

import com.sun.net.httpserver.HttpServer;
import org.apache.commons.cli.*;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;

public class Serv {

  private static final String VERSION = "0.1";
  private static final int DEFAULT_PORT = 17777;
  private static final String UTILITY_HELP_LINE = "serv [...options] <file or folder>";
  private static final String OPTION_PORT = "port";
  private static final String OPTION_COMPRESS = "compress";
  private static final String OPTION_VERSION = "version";

  public static void main(String[] args) throws Exception {

    Command command = parseCommandFromArgs(args);

    if (command.isVersion) {
      System.out.println(VERSION);
      System.exit(0);
    }

    String serveIp = IpUtil.getLocalNetworkIp();
    String url = "http://" + serveIp + ":" + command.servePort + "/dl";
    File file = command.file;
    boolean isFolder = file.isDirectory();
    boolean isCompress = command.isCompress;

    if (isFolder) {
      System.out.println("To download the files please use one of the commands below. ");
      System.out.println("NB! All files will be placed into current folder!");
      System.out.println();
      String extractPart = " | tar -x" + (isCompress ? "z" : "") + "vf -";
      System.out.println("curl " + url + extractPart);
      System.out.println("-or-");
      System.out.println("wget -O- " + url + extractPart);
    } else {
      System.out.println("To download the file please use: ");
      System.out.println();
      System.out.println("curl " + url + " > '" + file.getName() + "'");
      System.out.println("-or-");
      System.out.println("wget -O- " + url + " > '" + file.getName() + "'");
    }

    HttpServer server = HttpServer.create(new InetSocketAddress(serveIp, command.servePort), 0);
    server.createContext(
        "/dl",
        isFolder
            ? new HttpHandlerServeFolderTar(file, isCompress)
            : new HttpHandlerServeFile(file));
    server.setExecutor(null); // creates a default executor
    server.start();
  }

  private static Command parseCommandFromArgs(String[] args) {
    Options options = new Options();

    Option port =
        new Option("p", OPTION_PORT, true, "port to serve on (default = " + DEFAULT_PORT + ")");
    port.setRequired(false);
    options.addOption(port);

    Option compress =
        new Option("C", OPTION_COMPRESS, false, "enable compression (default = false)");
    compress.setRequired(false);
    options.addOption(compress);

    Option version = new Option("v", OPTION_VERSION, false, "show version and exit");
    compress.setRequired(false);
    options.addOption(version);

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

    return new Command(
        file,
        Integer.parseInt(cmd.getOptionValue(OPTION_PORT, "" + DEFAULT_PORT)),
        cmd.hasOption(OPTION_COMPRESS),
        cmd.hasOption(OPTION_VERSION));
  }

  private static IllegalStateException printHelpAndExit(String message, Options options) {
    if (message != null) {
      System.err.println(message);
    }
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(UTILITY_HELP_LINE, options);
    System.exit(1);
    return new IllegalStateException();
  }
}
