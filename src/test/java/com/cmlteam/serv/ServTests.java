package com.cmlteam.serv;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ServTests {
  @Test
  void testServeSingleFileUncompressed(@TempDir Path tempDir) throws IOException {
    Path file = createTestFile(tempDir, "file.txt", "hello world 123");

    File inputFile = file.toFile();
    Serv serv = new Serv(new String[] {inputFile.getAbsolutePath()});

    InetSocketAddress address = serv.getAddress();

    ReadableByteChannel readableByteChannel =
        Channels.newChannel(
            new URL("http://" + address.getHostName() + ":" + address.getPort() + "/dl")
                .openStream());

    File resultFile = tempDir.resolve("file_result.txt").toFile();
    FileOutputStream fileOutputStream = new FileOutputStream(resultFile);

    fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

    assertFilesEqual(inputFile, resultFile);

    serv.stop();
  }

  @Test
  void testServeSingleFileCompressed(@TempDir Path tempDir) throws IOException {
    Path file = createTestFile(tempDir, "file.txt", "hello world 123");

    File inputFile = file.toFile();
    Serv serv = new Serv(new String[] {inputFile.getAbsolutePath()});

    InetSocketAddress address = serv.getAddress();

    ReadableByteChannel readableByteChannel =
        Channels.newChannel(
            new URL("http://" + address.getHostName() + ":" + address.getPort() + "/dl?z")
                .openStream());

    File resultFileGz = tempDir.resolve("file_result.txt.gz").toFile();

    FileOutputStream fileOutputStream = new FileOutputStream(resultFileGz);
    fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

    GzipCompressorInputStream gzipCompressorInputStream =
        new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(resultFileGz)));

    File resultFile = tempDir.resolve("file_result.txt").toFile();

    Files.copy(gzipCompressorInputStream, resultFile.toPath());

    assertFilesEqual(inputFile, resultFile);

    serv.stop();
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
    Path inputFolder = createTestFolder(tempDir, "input_folder");

    String fname1 = "file1.txt";
    String fname2 = "file2.txt";
    String fname3 = "file3";

    Path file1 = createTestFile(inputFolder, fname1, "hello world 123");
    Path file2 = createTestFile(inputFolder, fname2, "");
    Path file3 =
        createTestFile(inputFolder, fname3, "123\n456\n789000000000000000000000000000000000");

    Serv serv = new Serv(new String[] {inputFolder.toFile().getAbsolutePath()});

    InetSocketAddress address = serv.getAddress();

    ReadableByteChannel readableByteChannel =
        Channels.newChannel(
            new URL(
                    "http://"
                        + address.getHostName()
                        + ":"
                        + address.getPort()
                        + "/dl"
                        + (isGz ? "?z" : ""))
                .openStream());

    File resultFile = tempDir.resolve("input_folder_result.tar" + (isGz ? ".gz" : "")).toFile();
    FileOutputStream fileOutputStream = new FileOutputStream(resultFile);
    fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

    Path resultExtractedFolder = createTestFolder(tempDir, "result_folder");

    FileExtractUtils.extractTarOrTgz(resultFile, resultExtractedFolder.toFile());

    assertFilesEqual(file1, resultExtractedFolder.resolve(fname1));
    assertFilesEqual(file2, resultExtractedFolder.resolve(fname2));
    assertFilesEqual(file3, resultExtractedFolder.resolve(fname3));
    String[] list = resultExtractedFolder.toFile().list();
    assertNotNull(list);
    assertEquals(3, list.length, "number of files should be same");

    serv.stop();
  }

  private Path createTestFile(Path folder, String name, String content) throws IOException {
    Path file = folder.resolve(name);
    Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    return file;
  }

  private Path createTestFolder(Path inFolder, String name) throws IOException {
    Path folder = inFolder.resolve(name);

    if (!folder.toFile().mkdirs()) {
      throw new IOException("Unable to created a folder");
    }

    return folder;
  }

  private void assertFilesEqual(Path expectedFile, Path resultFile) {
    assertFilesEqual(expectedFile.toFile(), resultFile.toFile());
  }

  private void assertFilesEqual(File expectedFile, File resultFile) {
    assertEquals(expectedFile.length(), resultFile.length(), "file size must be same");
    assertThat(resultFile)
        .describedAs("files should have same content")
        .hasSameContentAs(expectedFile);
  }
}
