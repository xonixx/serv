package com.cmlteam.serv;

import com.sun.net.httpserver.HttpServer;
import org.apache.commons.cli.*;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;

public class Serv {

  private static final String VERSION = "0.1";
  private static final int DEFAULT_PORT = 17777;
  private static final String UTILITY_NAME = "serv";
  private static final String UTILITY_HELP_LINE = UTILITY_NAME + " [...options] <file or folder>";

  public static void main(String[] args) throws Exception {

    Command command = parseCommandFromArgs(args);

    String serveIp = command.serveHost != null ? command.serveHost : IpUtil.getLocalNetworkIp();
    String url = "http://" + serveIp + ":" + command.servePort + "/dl";
    String urlZ = url + "?z";
    File file = command.file;
    boolean isFolder = file.isDirectory();

    if (isFolder) {
      System.out.println("To download the files please use one of the commands below. ");
      System.out.println("NB! All files will be placed into current folder!");
      System.out.println();

      String extractPart = " | tar -xvf -";
      System.out.println("curl " + url + extractPart);
      System.out.println("wget -O- " + url + extractPart);

      String extractPartZ = " | tar -xzvf -";
      System.out.println("curl " + urlZ + extractPartZ);
      System.out.println("wget -O- " + urlZ + extractPartZ);
    } else {
      System.out.println("To download the file please use one of the commands below: ");
      System.out.println();

      System.out.println("curl " + url + " > '" + file.getName() + "'");
      System.out.println("wget -O- " + url + " > '" + file.getName() + "'");

      System.out.println("curl " + urlZ + " --compressed > '" + file.getName() + "'");
      System.out.println("wget -O- " + urlZ + " | gunzip > '" + file.getName() + "'");
    }

    HttpServer server = HttpServer.create(new InetSocketAddress(serveIp, command.servePort), 0);
    server.createContext(
        "/dl", isFolder ? new HttpHandlerServeFolderTar(file) : new HttpHandlerServeFile(file));
    server.setExecutor(null); // creates a default executor
    server.start();
  }

  private static Command parseCommandFromArgs(String[] args) {
    Options options = new Options();

    Option host =
        new Option("H", "host", true, "host to serve on (default is determined automatically)");
    host.setRequired(false);
    options.addOption(host);

    Option port =
        new Option("p", "port", true, "port to serve on (default = " + DEFAULT_PORT + ")");
    port.setRequired(false);
    options.addOption(port);

    Option version = new Option("v", "version", false, "print version and exit");
    version.setRequired(false);
    options.addOption(version);

    Option help = new Option("h", "help", false, "print help and exit");
    help.setRequired(false);
    options.addOption(help);

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd;

    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      throw printHelpAndExit(e.getMessage(), options);
    }

    if (cmd.hasOption(help.getLongOpt())) {
      System.out.println(UTILITY_NAME + " ver. " + VERSION);
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(UTILITY_HELP_LINE, options);
      System.exit(0);
    }

    if (cmd.hasOption(version.getLongOpt())) {
      System.out.println(VERSION);
      System.exit(0);
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
        cmd.getOptionValue(host.getLongOpt()),
        Integer.parseInt(cmd.getOptionValue(port.getLongOpt(), "" + DEFAULT_PORT)));
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
