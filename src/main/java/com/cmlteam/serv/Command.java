package com.cmlteam.serv;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.apache.commons.cli.*;

import static com.cmlteam.serv.HelpMessageGenerator.UTILITY_HELP_LINE;

@RequiredArgsConstructor
public class Command {
  final Set<File> files;
  final String serveHost;
  final int servePort;
  final boolean includeVcsFiles;

  private static final HelpMessageGenerator helpMessageGenerator = new HelpMessageGenerator();

  static Command fromArgs(String... args) {
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


  String getHelpString() {
    String serveIp = serveHost != null ? serveHost : IpUtil.getLocalNetworkIp();
    String urlRoot = "http://" + serveIp + ":" + servePort + "/";
    String outputString;

    File file = files.iterator().next();
    if (files.size() == 1 && !file.isDirectory()) {
      outputString =
          helpMessageGenerator.getOutputStringForOneFileDownload(urlRoot, file.getName());
    } else {
      outputString = helpMessageGenerator.getOutputStringForMultipleFilesDownload(urlRoot);
    }
    return outputString;
  }

  @Override
  public String toString() {
    return "Command{"
        + "file="
        + files
        + ", serveHost='"
        + serveHost
        + '\''
        + ", servePort="
        + servePort
        + ", includeVcsFiles="
        + includeVcsFiles
        + '}';
  }
}
