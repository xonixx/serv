package com.cmlteam.serv;

import com.sun.net.httpserver.HttpServer;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Serv {

  private static final String UTILITY_HELP_LINE =
      Constants.UTILITY_NAME + " [...options] <file or folder>";

  public static void main(String[] args) throws Exception {
    String currentDirectory = System.getProperty("user.dir");
    System.out.println("The current working directory is " + currentDirectory);

    Command command = parseCommandFromArgs(args);

    String serveIp = command.serveHost != null ? command.serveHost : IpUtil.getLocalNetworkIp();
    String urlRoot = "http://" + serveIp + ":" + command.servePort + "/";
    Set<File> files = command.files;
    String outputString;

    File file = files.iterator().next();
    if (files.size() == 1 && !file.isDirectory()) {
      outputString = getOutputStringForOneFileDownload(urlRoot, file.getName());
    } else {
      outputString = getOutputStringForMultipleFilesDownload(urlRoot);
    }

    System.out.println(outputString + "\nOr just open in browser: " + urlRoot);
    HttpServer server = createServerWithContext(command, outputString, files);
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
      throw printHelpAndExit(e.getMessage(), options);
    }

    if (cmd.hasOption(help.getLongOpt())) {
      System.out.println(Constants.UTILITY_NAME + " ver. " + Constants.VERSION);
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(UTILITY_HELP_LINE, options);
      System.exit(0);
    }

    if (cmd.hasOption(version.getLongOpt())) {
      System.out.println(Constants.VERSION);
      System.exit(0);
    }

    List<String> argList = cmd.getArgList();

    if (argList.isEmpty()) {
      throw printHelpAndExit("Provide at least 1 file/folder to serve", options);
    }

    Set<File> files = argList.stream().map (
        argument -> {
          File file = new File(argument);
          if (!file.exists()) {
            throw printHelpAndExit("File/folder doesn't exist", options);
          }
          try {
            return file.getCanonicalFile();
          } catch (IOException e) {
            throw printHelpAndExit("File/folder path cannot be converted to canonical view", options);
          }
        }).collect(Collectors.toSet());

    return new Command(
        files,
        cmd.getOptionValue(host.getLongOpt()),
        Integer.parseInt(cmd.getOptionValue(port.getLongOpt(), "" + Constants.DEFAULT_PORT)),
        cmd.hasOption(includeVcsFiles.getLongOpt()));
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

  private static String getOutputStringForMultipleFilesDownload(String urlRoot) {
    String url = urlRoot + "dl";
    String urlZ = url + "?z";

    String extractPart = " | tar -xvf -";

    String extractPartZ = " | tar -xzvf -";

    return "To download the files please use one of the commands below.\n" +
        "NB! All files will be placed into current folder!\n\n" +
        getOutputStringByUrlAndExtractPart(url, extractPart) +
        getOutputStringByUrlAndExtractPart(urlZ, extractPartZ);
  }

  private static StringBuilder getOutputStringByUrlAndExtractPart(String url, String extractPart) {
    StringBuilder output = new StringBuilder();
    output.append("curl ").append(url).append(extractPart);
    output.append('\n');
    output.append("wget -O- ").append(url).append(extractPart);
    output.append('\n');
    return output;
  }

  private static String getOutputStringForOneFileDownload(String urlRoot, String fileName) {
    String url = urlRoot + "dl";
    String urlZ = url + "?z";

    return  "To download the file please use one of the commands below:\n\n" +
        "curl " + url + " > '" + fileName + "'" +
        '\n' +
        "wget -O- " + url + " > '" + fileName + "'" +
        '\n' +
        "curl " +
        urlZ +
        " --compressed > '" +
        fileName +
        "'" +
        '\n' +
        "wget -O- " +
        urlZ +
        " | gunzip > '" +
        fileName +
        "'" +
        '\n';
  }

  private static HttpServer createServerWithContext(Command command, String outputString, Set<File> files) throws Exception {
    String serveIp = command.serveHost != null ? command.serveHost : IpUtil.getLocalNetworkIp();
    HttpServer server = HttpServer.create(new InetSocketAddress(serveIp, command.servePort), 0);
    server.createContext("/", new HttpHandlerWebInfoPage(outputString));
    if (files.size() == 1) {
      File file = files.iterator().next();
      server.createContext(
          "/dl",
          file.isDirectory()
              ? new HttpHandlerServeFilesTar(file.listFiles(), command.includeVcsFiles)
              : new HttpHandlerServeFile(file));
    } else server.createContext("/dl", new HttpHandlerServeFilesTar(files.toArray(new File[0]), command.includeVcsFiles));
    return server;
  }
}
