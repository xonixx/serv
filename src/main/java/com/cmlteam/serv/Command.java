package com.cmlteam.serv;

import java.io.File;

public class Command {
  final File file;
  final String serveHost;
  final int servePort;

  Command(File file, String serveHost, int servePort) {
    this.file = file;
    this.serveHost = serveHost;
    this.servePort = servePort;
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
        + '}';
  }
}
