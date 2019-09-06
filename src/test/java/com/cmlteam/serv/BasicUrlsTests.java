package com.cmlteam.serv;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Path;

import static com.cmlteam.serv.TestsUtil.createTestFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BasicUrlsTests {
  private static final String testPort = "18888";
  private Serv serv;

  @AfterEach
  void afterEach() {
    serv.stop();
  }

  @Test
  void testFaviconUrl(@TempDir Path tempDir) throws IOException {
    // GIVEN
    String baseUrl = startSampleApp(tempDir);

    // WHEN
    URL url = new URL(baseUrl + "/favicon.ico");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    connection.connect();

    // THEN
    assertEquals(200, connection.getResponseCode());
    assertEquals("image/x-icon", connection.getHeaderField("Content-Type"));
  }

  @Test
  void test404(@TempDir Path tempDir) throws IOException {
    // GIVEN
    String baseUrl = startSampleApp(tempDir);

    // WHEN
    URL url = new URL(baseUrl + "/nonExistentUrl123");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    connection.connect();

    // THEN
    assertEquals(404, connection.getResponseCode());
  }

  @Test
  void testInfoPage(@TempDir Path tempDir) throws IOException {
    // GIVEN
    String baseUrl = startSampleApp(tempDir);

    // WHEN
    String infoText = TestsUtil.getUrlToString(baseUrl + "/");

    // THEN
//    System.out.println(infoText);
    assertTrue(infoText.contains("To download the"));
  }

  private String startSampleApp(@TempDir Path tempDir) throws IOException {
    Path file = createTestFile(tempDir, "file.txt", "hello world 123");

    File inputFile = file.toFile();
    serv = new Serv(new String[] {"-p", testPort, inputFile.getAbsolutePath()});

    InetSocketAddress address = serv.getAddress();

    return "http://" + address.getHostName() + ":" + address.getPort();
  }
}
