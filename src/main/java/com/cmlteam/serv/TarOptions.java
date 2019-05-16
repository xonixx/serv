package com.cmlteam.serv;

import lombok.Builder;
import lombok.Getter;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Getter
@Builder
class TarOptions {
  /** apply gzip compression */
  private boolean compress;

  private Set<String> filesToExclude;
  private Set<String> foldersToExclude;

  boolean shouldExclude(File file) {
    return filesToExclude != null && file.isFile() && filesToExclude.contains(file.getName())
        || foldersToExclude != null
            && file.isDirectory()
            && foldersToExclude.contains(file.getName());
  }

  /**
   * List of files and directories used by following version control systems:
   *
   * <p>`CVS', `RCS', `SCCS', `SVN', `Arch', `Bazaar', `Mercurial', and `Darcs'.
   *
   * <p>List was taken from tar docs.
   */
  private static final Set<String> vcsFolders =
      new HashSet<>(Arrays.asList("CVS", "RCS", "SCCS", ".git", ".svn", ".arch-ids", "{arch}"));

  private static final Set<String> vcsFiles =
      new HashSet<>(
          Arrays.asList(
              ".gitignore",
              ".gitmodules",
              ".gitattributes",
              ".cvsignore",
              "=RELEASE-ID",
              "=meta-update",
              "=update",
              ".bzr",
              ".bzrignore",
              ".bzrtags",
              ".hg",
              ".hgignore",
              ".hgrags",
              "_darcs"));

  @SuppressWarnings("unused")
  static class TarOptionsBuilder {
    TarOptionsBuilder excludeVcs(boolean excludeVcs) {
      if (excludeVcs) {
        filesToExclude(vcsFiles);
        foldersToExclude(vcsFolders);
      }
      return this;
    }
  }
}
