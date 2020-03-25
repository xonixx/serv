package com.cmlteam.serv;

import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Set;

@RequiredArgsConstructor
public class Command {
  final Set<File> files;
  final String serveHost;
  final int servePort;
  final boolean includeVcsFiles;

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
