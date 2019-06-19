package com.cmlteam.serv;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

class HelpMessageGenerator {

  static final String UTILITY_HELP_LINE =
      Constants.UTILITY_NAME + " [...options] <file or folder> [...<file or folder>]";

  static IllegalStateException printHelpAndExit(String message, Options options) {
    if (message != null) {
      System.err.println(message);
    }
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(UTILITY_HELP_LINE, options);
    System.exit(1);
    return new IllegalStateException();
  }

  static String getOutputStringForMultipleFilesDownload(String urlRoot) {
    String url = urlRoot + "dl";
    String urlZ = url + "?z";
    String listUrl = urlRoot + "listing";

    String extractPart = " | tar -xvf -";

    String extractPartZ = " | tar -xzvf -";

    return "To download the files please use one of the commands below.\n" +
        "NB! All files will be placed into current folder!\n\n" +
        getOutputStringByUrlAndExtractPart(url, extractPart) +
        getOutputStringByUrlAndExtractPart(urlZ, extractPartZ) +
            "To see the list of files use the URL below.\n" + listUrl;
  }

  private static StringBuilder getOutputStringByUrlAndExtractPart(String url, String extractPart) {
    StringBuilder output = new StringBuilder();
    output.append("curl ").append(url).append(extractPart);
    output.append('\n');
    output.append("wget -O- ").append(url).append(extractPart);
    output.append('\n');
    return output;
  }

  static String getOutputStringForOneFileDownload(String urlRoot, String fileName) {
    String url = urlRoot + "dl";
    String urlZ = url + "?z";

    return "To download the file please use one of the commands below:\n\n" +
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
}
