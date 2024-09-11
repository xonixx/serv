package com.cmlteam.serv;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Map;
import java.util.Set;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;

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
      TarArchiveEntry archiveEntry = new TarArchiveEntry(file, entry);
      archiveEntry.setMode(calcPermissions(file));
      out.putArchiveEntry(archiveEntry);
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

  private static final Map<PosixFilePermission, Integer> permToShift =
      Map.ofEntries(
          Map.entry(PosixFilePermission.OWNER_READ, 8),
          Map.entry(PosixFilePermission.OWNER_WRITE, 7),
          Map.entry(PosixFilePermission.OWNER_EXECUTE, 6),
          Map.entry(PosixFilePermission.GROUP_READ, 5),
          Map.entry(PosixFilePermission.GROUP_WRITE, 4),
          Map.entry(PosixFilePermission.GROUP_EXECUTE, 3),
          Map.entry(PosixFilePermission.OTHERS_READ, 2),
          Map.entry(PosixFilePermission.OTHERS_WRITE, 1),
          Map.entry(PosixFilePermission.OTHERS_EXECUTE, 0));

  static int calcPermissions(File file) throws IOException {
    Set<String> availableAttributeViews =
        file.toPath().getFileSystem().supportedFileAttributeViews();
    int res = TarArchiveEntry.DEFAULT_FILE_MODE;
    if (availableAttributeViews.contains("posix")) {
      Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(file.toPath());
      res = 0100000;
      for (PosixFilePermission permission : permissions) {
        res |= 1 << permToShift.get(permission);
      }
    }
    return res;
  }
}
