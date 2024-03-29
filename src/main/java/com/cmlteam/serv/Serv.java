package com.cmlteam.serv;

import com.sun.net.httpserver.HttpServer;
import lombok.SneakyThrows;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.cmlteam.serv.HelpMessageGenerator.UTILITY_HELP_LINE;

public class Serv {

  private final HelpMessageGenerator helpMessageGenerator = new HelpMessageGenerator();
  private final HttpServer server;

  public Serv(String[] args) {
    Command command = parseCommandFromArgs(args);

    String serveIp = command.serveHost != null ? command.serveHost : IpUtil.getLocalNetworkIp();
    String urlRoot = "http://" + serveIp + ":" + command.servePort + "/";
    Set<File> files = command.files;
    String outputString;

    File file = files.iterator().next();
    if (files.size() == 1 && !file.isDirectory()) {
      outputString =
          helpMessageGenerator.getOutputStringForOneFileDownload(urlRoot, file.getName());
    } else {
      outputString = helpMessageGenerator.getOutputStringForMultipleFilesDownload(urlRoot);
    }

    System.out.println(outputString);
    server = createServerWithContext(command, files);
    server.setExecutor(null); // creates a default executor
    server.start();
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

  private Command parseCommandFromArgs(String[] args) {
    Options options = new Options();

    Option host =
        new Option("H", "host", true, "host to serve on (default is determined automatically)");
    host.setRequired(false);
    options.addOption(host);

    Option port =
        new Option(
            "p", "port", true, "port to serve on (default = " + Constants.DEFAULT_PORT + ")");
    port.setRequired(false);
    options.addOption(port);

    Option version = new Option("v", "version", false, "print version and exit");
    version.setRequired(false);
    options.addOption(version);

    Option help = new Option("h", "help", false, "print help and exit");
    help.setRequired(false);
    options.addOption(help);

    Option includeVcsFiles =
        new Option(null, "include-vcs", false, "include VCS files (default = false)");
    includeVcsFiles.setRequired(false);
    options.addOption(includeVcsFiles);

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd;

    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      throw helpMessageGenerator.printHelpAndExit(e.getMessage(), options);
    }

    if (cmd.hasOption(help.getLongOpt())) {
      System.out.println(Constants.UTILITY_NAME + " ver. " + Constants.getFullVersion());
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(UTILITY_HELP_LINE, options);
      System.exit(0);
    }

    if (cmd.hasOption(version.getLongOpt())) {
      System.out.println(Constants.getFullVersion());
      System.exit(0);
    }

    List<String> argList = cmd.getArgList();

    if (argList.isEmpty()) {
      throw helpMessageGenerator.printHelpAndExit(
          "Provide at least 1 file/folder to serve", options);
    }

    Set<File> files =
        argList.stream()
            .map(
                argument -> {
                  File file = new File(argument);
                  if (!file.exists()) {
                    throw helpMessageGenerator.printHelpAndExit(
                        "File/folder doesn't exist", options);
                  }
                  try {
                    return file.getCanonicalFile();
                  } catch (IOException e) {
                    throw helpMessageGenerator.printHelpAndExit(
                        "File/folder path cannot be converted to canonical view", options);
                  }
                })
            .collect(Collectors.toSet());

    return new Command(
        files,
        cmd.getOptionValue(host.getLongOpt()),
        Integer.parseInt(cmd.getOptionValue(port.getLongOpt(), "" + Constants.DEFAULT_PORT)),
        cmd.hasOption(includeVcsFiles.getLongOpt()));
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
