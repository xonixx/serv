package com.cmlteam.serv;

import static com.cmlteam.serv.TestsUtil.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SharingTests {
  private static final String testPort = "18888";
  private Serv serv;

  @AfterEach
  void afterEach() {
    serv.stop();
  }

  @Test
  void testServeSingleFileUncompressed(@TempDir Path tempDir) throws IOException {
    // GIVEN
    Path file = createTestFile(tempDir, "file.txt", "hello world 123");

    File inputFile = file.toFile();
    serv = new Serv("-p", testPort, inputFile.getAbsolutePath());

    InetSocketAddress address = serv.getAddress();

    // WHEN
    File resultFile = tempDir.resolve("file_result.txt").toFile();
    getUrlToFile("http://" + address.getHostName() + ":" + address.getPort() + "/dl", resultFile);

    // THEN
    assertFilesEqual(inputFile, resultFile);
  }

  @Test
  void testServeSingleFileCompressed(@TempDir Path tempDir) throws IOException {
    // GIVEN
    Path file = createTestFile(tempDir, "file.txt", "hello world 123");

    File inputFile = file.toFile();
    serv = new Serv("-p", testPort, inputFile.getAbsolutePath());

    InetSocketAddress address = serv.getAddress();

    // WHEN
    File resultFileGz = tempDir.resolve("file_result.txt.gz").toFile();

    getUrlToFile(
        "http://" + address.getHostName() + ":" + address.getPort() + "/dl?z", resultFileGz);

    // THEN
    GzipCompressorInputStream gzipCompressorInputStream =
        new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(resultFileGz)));

    File resultFile = tempDir.resolve("file_result.txt").toFile();

    Files.copy(gzipCompressorInputStream, resultFile.toPath());

    assertFilesEqual(inputFile, resultFile);
  }

  @Test
  void testServeFolderUncompressed(@TempDir Path tempDir) throws IOException {
    testServeFolder(false, tempDir);
  }

  @Test
  void testServeFolderCompressed(@TempDir Path tempDir) throws IOException {
    testServeFolder(true, tempDir);
  }

  void testServeFolder(boolean isGz, Path tempDir) throws IOException {
    // GIVEN
    Path inputFolder = createTestFolder(tempDir, "input_folder");

    String fname1 = "file1.txt";
    String fname2 = "file2.txt";
    String fname3 = "file3";

    Path file1 = createTestFile(inputFolder, fname1, "hello world 123");
    Path file2 = createTestFile(inputFolder, fname2, "");
    Path file3 =
        createTestFile(inputFolder, fname3, "123\n456\n789000000000000000000000000000000000");

    serv = new Serv("-p", testPort, inputFolder.toFile().getAbsolutePath());

    InetSocketAddress address = serv.getAddress();

    // WHEN
    File resultFile = tempDir.resolve("input_folder_result.tar" + (isGz ? ".gz" : "")).toFile();

    getUrlToFile(
        "http://" + address.getHostName() + ":" + address.getPort() + "/dl" + (isGz ? "?z" : ""),
        resultFile);

    // THEN
    Path resultExtractedFolder = createTestFolder(tempDir, "result_folder");

    FileExtractUtils.extractTarOrTgz(resultFile, resultExtractedFolder.toFile());

    assertFilesEqual(file1, resultExtractedFolder.resolve(fname1));
    assertFilesEqual(file2, resultExtractedFolder.resolve(fname2));
    assertFilesEqual(file3, resultExtractedFolder.resolve(fname3));
    String[] list = resultExtractedFolder.toFile().list();
    assertNotNull(list);
    assertEquals(3, list.length, "number of files should be same");
  }

  @Test
  void testServeFileSetUncompressed(@TempDir Path tempDir) throws IOException {
    testServeFileSet(false, tempDir);
  }

  @Test
  void testServeFileSetCompressed(@TempDir Path tempDir) throws IOException {
    testServeFileSet(true, tempDir);
  }

  void testServeFileSet(boolean isGz, Path tempDir) throws IOException {
    // GIVEN
    Path inputFolder = createTestFolder(tempDir, "input_folder");

    String fname1 = "file1.txt";
    String fname2 = "file2.txt";
    String fname3 = "file3";
    String fname4 = "FiLe4.html";

    Path file1 = createTestFile(inputFolder, fname1, "hello world 123");
    Path file2 = createTestFile(inputFolder, fname2, "");
    Path file3 =
        createTestFile(inputFolder, fname3, "123\n456\n789000000000000000000000000000000000");
    Path file4 = createTestFile(inputFolder, fname4, "<h1>Hello</h1>");

    serv =
        new Serv(
            "-p",
            testPort,
            file1.toFile().getAbsolutePath(),
            file2.toFile().getAbsolutePath(),
            file3.toFile().getAbsolutePath());

    InetSocketAddress address = serv.getAddress();

    // WHEN
    File resultFile = tempDir.resolve("input_folder_result.tar" + (isGz ? ".gz" : "")).toFile();
    getUrlToFile(
        "http://" + address.getHostName() + ":" + address.getPort() + "/dl" + (isGz ? "?z" : ""),
        resultFile);

    //    System.out.println("resultFile: " + resultFile);
    //    System.out.println("    size=" + resultFile.length());

    // THEN
    Path resultExtractedFolder = createTestFolder(tempDir, "result_folder");

    FileExtractUtils.extractTarOrTgz(resultFile, resultExtractedFolder.toFile());

    assertFilesEqual(file1, resultExtractedFolder.resolve(fname1));
    assertFilesEqual(file2, resultExtractedFolder.resolve(fname2));
    assertFilesEqual(file3, resultExtractedFolder.resolve(fname3));
    String[] list = resultExtractedFolder.toFile().list();
    assertNotNull(list);
    assertEquals(3, list.length, "number of files should be the same");
  }

  @Test
  void testPreserveScriptsPermissions_extractViaTar_issue48(@TempDir Path tempDir)
      throws IOException {
    // GIVEN
    Path inputFolder = createTestFolder(tempDir, "input_folder");

    String fname1 = "script.sh";

    Path file1 = createTestFile(inputFolder, fname1, "#!/bin/sh\necho 123\n");

    assertEquals(0, exec("chmod", "+x", file1.toFile().getAbsolutePath()));
    assertEquals(0, exec("ls", "-l", file1.toFile().getAbsolutePath())); // show perms
    //    assertEquals(0, TestsUtil.exec(file1.toFile().getAbsolutePath()));

    Command command = Command.fromArgs("-p", testPort, inputFolder.toFile().getAbsolutePath());
    String helpString = command.getHelpString();
    String[] lines = helpString.split("[\r\n]+");
    String cmdTar = "";
    for (String line : lines) {
      if (line.contains(" tar ")) {
        cmdTar = line;
        break;
      }
    }
//    System.out.println(helpString);
//    System.out.println("cmdTar: " + cmdTar);

    serv = new Serv(command);

    InetSocketAddress address = serv.getAddress();

    // WHEN
    File resultFile = tempDir.resolve("input_folder_result.tar").toFile();
    getUrlToFile("http://" + address.getHostName() + ":" + address.getPort() + "/dl", resultFile);

    System.out.println("resultFile: " + resultFile);
    System.out.println("    size=" + resultFile.length());

    // THEN
    Path resultExtractedFolder = createTestFolder(tempDir, "result_folder");

    assertEquals(0, system("cd '" + resultExtractedFolder.toFile().getAbsolutePath() + "'; " + cmdTar));


    Path file1Res = resultExtractedFolder.resolve(fname1);
    assertFilesEqual(file1, file1Res);
    assertEquals(0, exec("tar", "-tvf", resultFile.getAbsolutePath())); // show perms in tar
    assertEquals(0, exec("ls", "-l", file1Res.toFile().getAbsolutePath())); // show perms
    assertEquals(0, exec(file1Res.toFile().getAbsolutePath())); // is still executable
  }

  @Test
  void testPreserveScriptsPermissions_issue48(@TempDir Path tempDir) throws IOException {
    // GIVEN
    Path inputFolder = createTestFolder(tempDir, "input_folder");

    String fname1 = "script.sh";

    Path file1 = createTestFile(inputFolder, fname1, "#!/bin/sh\necho 123\n");

    assertEquals(0, exec("chmod", "+x", file1.toFile().getAbsolutePath()));
    assertEquals(0, exec("ls", "-l", file1.toFile().getAbsolutePath())); // show perms
    //    assertEquals(0, TestsUtil.exec(file1.toFile().getAbsolutePath()));

    serv = new Serv("-p", testPort, inputFolder.toFile().getAbsolutePath());

    InetSocketAddress address = serv.getAddress();

    // WHEN
    File resultFile = tempDir.resolve("input_folder_result.tar").toFile();
    getUrlToFile("http://" + address.getHostName() + ":" + address.getPort() + "/dl", resultFile);

    System.out.println("resultFile: " + resultFile);
    System.out.println("    size=" + resultFile.length());

    // THEN
    Path resultExtractedFolder = createTestFolder(tempDir, "result_folder");

    FileExtractUtils.extractTarOrTgz(resultFile, resultExtractedFolder.toFile());

    Path file1Res = resultExtractedFolder.resolve(fname1);
    assertFilesEqual(file1, file1Res);
    assertEquals(0, exec("tar", "-tvf", resultFile.getAbsolutePath())); // show perms in tar
    assertEquals(0, exec("ls", "-l", file1Res.toFile().getAbsolutePath())); // show perms
    assertEquals(0, exec(file1Res.toFile().getAbsolutePath())); // is still executable
  }

  @Test
  void testHandleSymlinks_issue45(@TempDir Path tempDir) throws IOException {
    // GIVEN
    Path inputFolder = createTestFolder(tempDir, "input_folder");

    String fname1 = "test.txt";
    String symLinkName = "test.lnk";

    Path file1 = createTestFile(inputFolder, fname1, "123");
    Path symLink = inputFolder.resolve(symLinkName);
    Files.createSymbolicLink(symLink, Path.of("./",fname1));

    assertEquals(0, exec("ls", "-l", inputFolder.toFile().getAbsolutePath())); // show symlink


    serv = new Serv("-p", testPort, inputFolder.toFile().getAbsolutePath());

    InetSocketAddress address = serv.getAddress();

    // WHEN
    File resultFile = tempDir.resolve("input_folder_result.tar").toFile();
    getUrlToFile("http://" + address.getHostName() + ":" + address.getPort() + "/dl", resultFile);

    System.out.println("resultFile: " + resultFile);
    System.out.println("    size=" + resultFile.length());

    // THEN
    Path resultExtractedFolder = createTestFolder(tempDir, "result_folder");

    assertEquals(
        0,
        exec(
            "tar",
            "-C",
            resultExtractedFolder.toAbsolutePath().toString(),
            "-xvf",
            resultFile.getAbsolutePath()));

    Path file1Res = resultExtractedFolder.resolve(fname1);

    assertFilesEqual(file1, file1Res);
    System.out.println("tar content:");
    assertEquals(0, exec("tar", "-tvf", resultFile.getAbsolutePath())); // show tar content
    System.out.println("dir listing:");
    assertEquals(0, exec("ls", "-l", resultExtractedFolder.toFile().getAbsolutePath())); // show extracted files

    Path symLinkRes = resultExtractedFolder.resolve(symLinkName);

    assertTrue(Files.isSymbolicLink(symLinkRes));
    assertTrue(Files.isSameFile(file1Res, resultExtractedFolder.resolve(Files.readSymbolicLink(symLinkRes))));
  }
}
