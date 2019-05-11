package com.cmlteam.serv;

import java.io.File;

public class Command {
  final File file;
  final int servePort;

  Command(File file, int servePort) {
    this.file = file;
    this.servePort = servePort;
  }

  @Override
  public String toString() {
    return "Command{" + "file=" + file + ", servePort=" + servePort + '}';
  }
}
