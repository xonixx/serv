package com.cmlteam.serv;

import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class TestsUtil {
  private TestsUtil() {}

  static Path createTestFile(Path folder, String name, String content) throws IOException {
    Path file = folder.resolve(name);
    Files.write(file, content.getBytes(UTF_8));
    return file;
  }

  static Path createTestFolder(Path inFolder, String name) throws IOException {
    Path folder = inFolder.resolve(name);

    if (!folder.toFile().mkdirs()) {
      throw new IOException("Unable to created a folder");
    }

    return folder;
  }

  static void assertFilesEqual(Path expectedFile, Path resultFile) {
    assertFilesEqual(expectedFile.toFile(), resultFile.toFile());
  }

  static void assertFilesEqual(File expectedFile, File resultFile) {
    assertEquals(expectedFile.length(), resultFile.length(), "file size must be same");
    assertThat(resultFile)
        .describedAs("files should have same content")
        .hasSameContentAs(expectedFile);
  }

  static void getUrlToFile(String url, File resultFile) throws IOException {
    ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(url).openStream());

    FileOutputStream fileOutputStream = new FileOutputStream(resultFile);

    fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
  }

  static String getUrlToString(String url) throws IOException {
    return inputStreamToString(new URL(url).openStream());
  }

  private static String inputStreamToString(InputStream inputStream) {
    try (Scanner scanner = new Scanner(inputStream, UTF_8.toString())) {
      scanner.useDelimiter("\\A");
      return scanner.hasNext() ? scanner.next() : "";
    }
  }

  @RequiredArgsConstructor
  static class GetReply {
    final int statusCode;
    final String body;
  }

  static GetReply getUrl(String url) throws IOException {
    URL url_ = new URL(url);
    HttpURLConnection connection = null;
    try {
      connection = (HttpURLConnection) url_.openConnection();
      connection.setRequestMethod("GET");
      connection.connect();
      int responseCode = connection.getResponseCode();
      return new GetReply(
          responseCode,
          inputStreamToString(
              responseCode < HttpURLConnection.HTTP_BAD_REQUEST
                  ? connection.getInputStream()
                  : connection.getErrorStream()));
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }
}
