package com.cmlteam.serv;

import java.io.File;

public class Command {
  final File file;
  final int servePort;
  final boolean isCompress;

  Command(File file, int servePort, boolean isCompress) {
    this.file = file;
    this.servePort = servePort;
    this.isCompress = isCompress;
  }

  @Override
  public String toString() {
    return "Command{"
        + "file="
        + file
        + ", servePort="
        + servePort
        + ", isCompress="
        + isCompress
        + '}';
  }
}
