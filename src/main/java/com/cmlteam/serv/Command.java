package com.cmlteam.serv;

import java.io.File;

public class Command {
  final File file;
  final String serveHost;
  final int servePort;
  final boolean includeVcsFiles;

  Command(File file, String serveHost, int servePort, boolean includeVcsFiles) {
    this.file = file;
    this.serveHost = serveHost;
    this.servePort = servePort;
    this.includeVcsFiles = includeVcsFiles;
  }

  @Override
  public String toString() {
    return "Command{"
        + "file="
        + file
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
