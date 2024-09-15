package com.cmlteam.serv;

import static java.nio.file.attribute.PosixFilePermission.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
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
    if (Files.isSymbolicLink(file.toPath())) {
      TarArchiveEntry archiveEntry = new TarArchiveEntry(entry, TarConstants.LF_SYMLINK);
      archiveEntry.setLinkName(Files.readSymbolicLink(file.toPath()).toString());
      archiveEntry.setMode(calcPermissions(file));
      out.putArchiveEntry(archiveEntry);
      out.closeArchiveEntry();
    } else if (file.isFile()) {
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
      // TODO this could be due to absent permission
      System.err.println("warning: not supported: " + file);
    }
  }

  private static final Map<PosixFilePermission, Integer> permToShift =
      Map.ofEntries(
          Map.entry(OWNER_READ, 8),
          Map.entry(OWNER_WRITE, 7),
          Map.entry(OWNER_EXECUTE, 6),
          Map.entry(GROUP_READ, 5),
          Map.entry(GROUP_WRITE, 4),
          Map.entry(GROUP_EXECUTE, 3),
          Map.entry(OTHERS_READ, 2),
          Map.entry(OTHERS_WRITE, 1),
          Map.entry(OTHERS_EXECUTE, 0));

  static Set<PosixFilePermission> modeToPermissions(int mode) {
    Set<PosixFilePermission> perms = EnumSet.noneOf(PosixFilePermission.class);
    for (Map.Entry<PosixFilePermission, Integer> permEntry : permToShift.entrySet()) {
      if ((mode & (1 << permEntry.getValue())) > 0) {
        perms.add(permEntry.getKey());
      }
    }
    return perms;
  }

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
