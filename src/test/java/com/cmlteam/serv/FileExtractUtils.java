package com.cmlteam.serv;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import lombok.SneakyThrows;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

public class FileExtractUtils {
  private static final int BUFFER_SIZE = 4096;

  @SneakyThrows
  public static void extractTarOrTgz(File tarOrTgzFile, File outDir) {
    InputStream _inputStream = new FileInputStream(tarOrTgzFile);
    if (tarOrTgzFile.getName().toLowerCase().endsWith("gz")) {
      _inputStream = new GzipCompressorInputStream(new BufferedInputStream(_inputStream));
    }
    try (InputStream inputStream = _inputStream;
        TarArchiveInputStream tarIs = new TarArchiveInputStream(inputStream)) {
      TarArchiveEntry entry;
      while ((entry = tarIs.getNextEntry()) != null) {
        String name = entry.getName();
        if (entry.isDirectory()) {
          mkDirs(outDir, name);
        } else {
          String dir = directoryPart(name);
          if (dir != null) {
            mkDirs(outDir, dir);
          }
          extractFile(tarIs, outDir, name, TarUtil.modeToPermissions(entry.getMode()));
        }
      }
    }
  }

  private static void extractFile(
      InputStream inputStream,
      File outDir,
      String name,
      Set<PosixFilePermission> posixFilePermissions)
      throws IOException {
    int count;
    byte[] buffer = new byte[BUFFER_SIZE];
    File file = new File(outDir, name);
    try (BufferedOutputStream out =
        new BufferedOutputStream(new FileOutputStream(file), BUFFER_SIZE)) {
      while ((count = inputStream.read(buffer, 0, BUFFER_SIZE)) != -1) {
        out.write(buffer, 0, count);
      }
    }
    Files.setPosixFilePermissions(file.toPath(), posixFilePermissions);
  }

  private static void mkDirs(File outdir, String path) {
    File d = new File(outdir, path);
    if (!d.exists()) {
      d.mkdirs();
    }
  }

  private static String directoryPart(String name) {
    int s = name.lastIndexOf(File.separatorChar);
    return s == -1 ? null : name.substring(0, s);
  }
}
