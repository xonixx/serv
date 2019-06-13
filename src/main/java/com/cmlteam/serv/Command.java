package com.cmlteam.serv;

import java.io.File;
import java.util.Set;

public class Command {
  final Set<File> files;
  final String serveHost;
  final int servePort;
  final boolean includeVcsFiles;

  Command(Set<File> files, String serveHost, int servePort, boolean includeVcsFiles) {
    this.files = files;
    this.serveHost = serveHost;
    this.servePort = servePort;
    this.includeVcsFiles = includeVcsFiles;
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
