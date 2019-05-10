package com.cmlteam.serv;

import java.io.File;

public class Task {
  final File file;
  final int servePort;
  final boolean compress;

  public Task(File file, int servePort, boolean compress) {
    this.file = file;
    this.servePort = servePort;
    this.compress = compress;
  }

  @Override
  public String toString() {
    return "Task{" + "file=" + file + ", servePort=" + servePort + ", compress=" + compress + '}';
  }
}
