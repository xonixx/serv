package com.cmlteam.serv;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

class TarUtil {

  private TarUtil() {}

  /**
   * @param outputStream output stream to write to
   * @param files files to compress
   * @param tarOptions tar compression options
   * @throws IOException in case of IO exception
   */
  static void compress(OutputStream outputStream, File[] files, TarOptions tarOptions)
      throws IOException {
    try (TarArchiveOutputStream out =
        getTarArchiveOutputStream(outputStream, tarOptions.isCompress())) {
      if (files != null) {
        for (File file : files) {
          addToArchiveCompression(out, file, "", tarOptions);
        }
      }
    }
  }

  private static TarArchiveOutputStream getTarArchiveOutputStream(
      OutputStream outputStream, boolean compress) throws IOException {
    if (compress) {
      outputStream = new GzipCompressorOutputStream(outputStream);
    }
    TarArchiveOutputStream taos = new TarArchiveOutputStream(outputStream);
    // TAR has an 8 gig file limit by default, this gets around that
    taos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
    // TAR originally didn't support long file names, so enable the support for it
    taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
    taos.setAddPaxHeadersForNonAsciiNames(true);
    return taos;
  }

  private static void addToArchiveCompression(
      TarArchiveOutputStream out, File file, String dir, TarOptions tarOptions) throws IOException {

    if (!file.canRead()) {
      System.err.println("warning: can't read " + file);
      return;
    }

    if (tarOptions.shouldExclude(file)) {
      return;
    }

    String entry = dir + File.separator + file.getName();
    if (file.isFile()) {
      out.putArchiveEntry(new TarArchiveEntry(file, entry));
      try (FileInputStream in = new FileInputStream(file)) {
        IOUtils.copy(in, out);
      }
      out.closeArchiveEntry();
    } else if (file.isDirectory()) {
      File[] children = file.listFiles();
      if (children != null) {
        for (File child : children) {
          addToArchiveCompression(out, child, entry, tarOptions);
        }
      }
    } else {
      System.err.println("warning: not supported (symlink?): " + file);
    }
  }
}
