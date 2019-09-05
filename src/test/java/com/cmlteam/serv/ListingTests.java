package com.cmlteam.serv;

import com.cmlteam.util.Util;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;

import static com.cmlteam.serv.TestsUtil.createTestFile;
import static com.cmlteam.serv.TestsUtil.createTestFolder;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ListingTests {
  private static final String testPort = "18888";
  private Serv serv;

  @AfterEach
  void afterEach() {
    serv.stop();
  }

  @Test
  void basicFolderListingTest(@TempDir Path tempDir) throws IOException {
    // GIVEN
    Path inputFolder = createTestFolder(tempDir, "input_folder");

    String fname1 = "file1.txt";
    String fname2 = "file2.txt";
    String fname3 = "file3";

    Path file1 = createTestFile(inputFolder, fname1, "hello world 123");
    Path file2 = createTestFile(inputFolder, fname2, "");
    Path file3 =
        createTestFile(inputFolder, fname3, "123\n456\n789000000000000000000000000000000000");

    serv = new Serv(new String[] {"-p", testPort, inputFolder.toFile().getAbsolutePath()});

    InetSocketAddress address = serv.getAddress();

    String listingUrl = "http://" + address.getHostName() + ":" + address.getPort() + "/listing";

    //    System.out.println(getUrlToString(listingUrl));

    // WHEN
    Document document = Jsoup.connect(listingUrl).get();

    // THEN
    System.out.println(document);

    Elements trElements = document.select("table tbody tr");

    assertEquals("Index of /", document.select("h1").first().text());
    assertEquals(3, trElements.size());
    assertEquals(fname1, trElements.get(0).select("td").first().text());
    assertEquals(fname2, trElements.get(1).select("td").first().text());
    assertEquals(fname3, trElements.get(2).select("td").first().text());
    assertEquals(
        Util.renderFileSize(file1.toFile().length()), trElements.get(0).select("td").get(1).text());
    assertEquals(
        Util.renderFileSize(file2.toFile().length()), trElements.get(1).select("td").get(1).text());
    assertEquals(
        Util.renderFileSize(file3.toFile().length()), trElements.get(2).select("td").get(1).text());
  }

  @Test
  void basicFileSetListingTest(@TempDir Path tempDir) throws IOException {
    // GIVEN
    Path inputFolder = createTestFolder(tempDir, "input_folder");

    String fname1 = "file1.txt";
    String fname2 = "file2.txt";
    String fname3 = "file3";

    Path file1 = createTestFile(inputFolder, fname1, "hello world 123");
    Path file2 = createTestFile(inputFolder, fname2, "");
    Path file3 =
        createTestFile(inputFolder, fname3, "123\n456\n789000000000000000000000000000000000");

    serv =
        new Serv(
            new String[] {
              "-p", testPort, file1.toFile().getAbsolutePath(), file3.toFile().getAbsolutePath()
            });

    InetSocketAddress address = serv.getAddress();

    String listingUrl = "http://" + address.getHostName() + ":" + address.getPort() + "/listing";

    //    System.out.println(getUrlToString(listingUrl));

    // WHEN
    Document document = Jsoup.connect(listingUrl).get();

    // THEN
    System.out.println(document);

    Elements trElements = document.select("table tbody tr");

    assertEquals("Index of /", document.select("h1").first().text());
    assertEquals(2, trElements.size());
    assertEquals(fname1, trElements.get(0).select("td").first().text());
    assertEquals(fname3, trElements.get(1).select("td").first().text());
    assertEquals(
        Util.renderFileSize(file1.toFile().length()), trElements.get(0).select("td").get(1).text());
    assertEquals(
        Util.renderFileSize(file3.toFile().length()), trElements.get(1).select("td").get(1).text());
  }

  @Test
  void complexFileSetListingTest(@TempDir Path tempDir) throws IOException {
    // GIVEN
    GivenForComplexFileSet given01 = GivenForComplexFileSet.prepare(tempDir);

    serv =
        new Serv(
            new String[] {
              "-p",
              testPort,
              given01.inputFolder1.toFile().getAbsolutePath(),
              given01.inputFolder2.toFile().getAbsolutePath(),
              given01.file7.toFile().getAbsolutePath()
            });

    InetSocketAddress address = serv.getAddress();

    String listingUrl = "http://" + address.getHostName() + ":" + address.getPort() + "/listing";

    //    System.out.println(getUrlToString(listingUrl));

    // WHEN
    Document document = Jsoup.connect(listingUrl).get();

    // THEN
    System.out.println(document);

    Elements trElements = document.select("table tbody tr");

    assertEquals("Index of /", document.select("h1").first().text());
    assertEquals(3, trElements.size());
    assertEquals(given01.folderName1, trElements.get(0).select("td").first().text());
    assertEquals(given01.folderName2, trElements.get(1).select("td").first().text());
    assertEquals(given01.fname7, trElements.get(2).select("td").first().text());
    assertEquals("", trElements.get(0).select("td").get(1).text());
    assertEquals("", trElements.get(1).select("td").get(1).text());
    assertEquals(
        Util.renderFileSize(given01.file7.toFile().length()),
        trElements.get(2).select("td").get(1).text());
  }

  @Test
  void complexFileSetListingNavigationTest(@TempDir Path tempDir) throws IOException {
    // GIVEN
    GivenForComplexFileSet given = GivenForComplexFileSet.prepare(tempDir);

    serv =
        new Serv(
            new String[] {
              "-p",
              testPort,
              given.inputFolder1.toFile().getAbsolutePath(),
              given.inputFolder2.toFile().getAbsolutePath(),
              given.file7.toFile().getAbsolutePath()
            });

    InetSocketAddress address = serv.getAddress();

    String listingUrl = "http://" + address.getHostName() + ":" + address.getPort() + "/listing";

    //    System.out.println(getUrlToString(listingUrl));

    Document documentRoot = Jsoup.connect(listingUrl).get();

    String nextPage =
        documentRoot
            .select("table tbody tr")
            .get(1)
            .select("td")
            .first()
            .select("a")
            .first()
            .attr("href");
    String listingUrlNext = "http://" + address.getHostName() + ":" + address.getPort() + nextPage;

    // WHEN
    // emulate click on a folder 'input_folder2' link
    Document document = Jsoup.connect(listingUrlNext).get();

    // THEN
    System.out.println(document);

    Elements trElements = document.select("table tbody tr");

    assertEquals("Index of /" + given.folderName2 + "/", document.select("h1").first().text());
    assertEquals(2, trElements.size());
    assertEquals(given.fname4, trElements.get(0).select("td").first().text());
    assertEquals(given.fname5, trElements.get(1).select("td").first().text());
    assertEquals(
        Util.renderFileSize(given.file4.toFile().length()),
        trElements.get(0).select("td").get(1).text());
    assertEquals(
        Util.renderFileSize(given.file5.toFile().length()),
        trElements.get(1).select("td").get(1).text());
  }

  @RequiredArgsConstructor
  private static class GivenForComplexFileSet {
    private final String folderName1;
    private final Path inputFolder1;
    private final String fname1;
    private final String fname2;
    private final String fname3;
    private final Path file1;
    private final Path file2;
    private final Path file3;
    private final String folderName2;
    private final Path inputFolder2;
    private final String fname4;
    private final String fname5;
    private final Path file4;
    private final Path file5;
    private final String fname7;
    private final Path file7;

    private static GivenForComplexFileSet prepare(Path tempDir) throws IOException {
      String folderName1 = "input_folder1";
      Path inputFolder1 = createTestFolder(tempDir, folderName1);

      String fname1 = "file1.txt";
      String fname2 = "file2.txt";
      String fname3 = "file3";

      Path file1 = createTestFile(inputFolder1, fname1, "hello world 123");
      Path file2 = createTestFile(inputFolder1, fname2, "");
      Path file3 =
          createTestFile(inputFolder1, fname3, "123\n456\n789000000000000000000000000000000000");

      String folderName2 = "input_folder2";
      Path inputFolder2 = createTestFolder(tempDir, folderName2);

      String fname4 = "file4.txt";
      String fname5 = "file5.csv";

      Path file4 = createTestFile(inputFolder2, fname4, "hello world 123");
      Path file5 = createTestFile(inputFolder2, fname5, "");

      String fname7 = "FiLe777";
      Path file7 = createTestFile(tempDir, fname7, "");

      return new GivenForComplexFileSet(
          folderName1,
          inputFolder1,
          fname1,
          fname2,
          fname3,
          file1,
          file2,
          file3,
          folderName2,
          inputFolder2,
          fname4,
          fname5,
          file4,
          file5,
          fname7,
          file7);
    }
  }
}
