package com.cmlteam.serv;

import java.io.File;

public class Command {
  final File file;
  final int servePort;
  final boolean isCompress;
  final boolean isVersion;

  Command(File file, int servePort, boolean isCompress, boolean isVersion) {
    this.file = file;
    this.servePort = servePort;
    this.isCompress = isCompress;
    this.isVersion = isVersion;
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
        + ", isVersion="
        + isVersion
        + '}';
  }
}
