package com.cmlteam.serv;

import com.cmlteam.util.Util;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

  @ParameterizedTest
  @ValueSource(strings = {"", "?f=0&name="})
  void basicFolderListingTest(String path, @TempDir Path tempDir) throws IOException {
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

    String listingUrl = "http://" + address.getHostName() + ":" + address.getPort() + "/" + path;

    //    System.out.println(getUrlToString(listingUrl));

    // WHEN
    Document document = Jsoup.connect(listingUrl).get();

    // THEN
    System.out.println(document);

    Elements trElements = document.select("table tbody tr");

    assertEquals("Index of /", getH1Text(document));
    assertEquals(0, document.select("a.up").size());
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
  void basicFolderListingCorrectTitle(@TempDir Path tempDir) throws IOException {
    // GIVEN
    Path folder1 = createTestFolder(tempDir, "folder1");
    Path folder2 = createTestFolder(folder1, "folder2");
    Path file = createTestFile(folder2, "f.txt", "abc");

    serv = new Serv(new String[] {"-p", testPort, folder1.toFile().getAbsolutePath()});

    InetSocketAddress address = serv.getAddress();

    String listingUrl = "http://" + address.getHostName() + ":" + address.getPort() + "/";

    //    System.out.println(getUrlToString(listingUrl));

    Document document = Jsoup.connect(listingUrl).get();

//    System.out.println(document);

    Elements trElements = document.select("table tbody tr");
    String href = trElements.get(0).select("td a").first().attr("href");
//    System.out.println(href);

    String listingUrl1 = "http://" + address.getHostName() + ":" + address.getPort() + href;

    // WHEN
    Document document1 = Jsoup.connect(listingUrl1).get();

    // THEN
    assertEquals("Index of /", getH1Text(document));
    assertEquals("Index of /" + folder2.getFileName() + "/", getH1Text(document1));
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

    String listingUrl = "http://" + address.getHostName() + ":" + address.getPort() + "/";

    //    System.out.println(getUrlToString(listingUrl));

    // WHEN
    Document document = Jsoup.connect(listingUrl).get();

    // THEN
    System.out.println(document);

    Elements trElements = document.select("table tbody tr");

    assertEquals("Index of /", getH1Text(document));
    assertEquals(0, document.select("a.up").size());
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
    GivenForComplexFileSet given = GivenForComplexFileSet.prepare(this, tempDir);

    String listingUrl = given.baseUrl + "/";

    //    System.out.println(getUrlToString(listingUrl));

    // WHEN
    Document document = Jsoup.connect(listingUrl).get();

    // THEN
    checkCorrectListingRoot(given, document);
  }

  private void checkCorrectListingRoot(GivenForComplexFileSet given, Document document) {
    System.out.println(document);

    Elements trElements = document.select("table tbody tr");

    assertEquals("Index of /", getH1Text(document));
    assertEquals(0, document.select("a.up").size());
    assertEquals(3, trElements.size());
    assertEquals(given.folderName1, trElements.get(0).select("td").first().text());
    assertEquals(given.folderName2, trElements.get(1).select("td").first().text());
    assertEquals(given.fname7, trElements.get(2).select("td").first().text());
    assertEquals("", trElements.get(0).select("td").get(1).text());
    assertEquals("", trElements.get(1).select("td").get(1).text());
    assertEquals(
        Util.renderFileSize(given.file7.toFile().length()),
        trElements.get(2).select("td").get(1).text());
  }

  private String getH1Text(Document document) {
    return document.select("h1").first().text().replace(" ↓ tar | ↓ tar.gz", "");
  }

  @Test
  void complexFileSetListingNavigationTest(@TempDir Path tempDir) throws IOException {
    // GIVEN
    GivenForComplexFileSet given = GivenForComplexFileSet.prepare(this, tempDir);

    String listingUrl = given.baseUrl + "/";

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

    // WHEN
    // emulate click on a folder 'input_folder2' link
    String listingUrlNext = given.baseUrl + nextPage;
    Document document = Jsoup.connect(listingUrlNext).get();

    // THEN
    System.out.println(document);

    Elements trElements = document.select("table tbody tr");

    assertEquals("Index of /" + given.folderName2 + "/", getH1Text(document));
    assertEquals(1, document.select("a.up").size());
    assertEquals(3, trElements.size());
    Element f0 = trElements.get(1);
    Element f1 = trElements.get(2);
    assertEquals(given.fname4, f0.select("td").first().text());
    assertEquals(given.fname5, f1.select("td").first().text());
    assertEquals(Util.renderFileSize(given.file4.toFile().length()), f0.select("td").get(1).text());
    assertEquals(Util.renderFileSize(given.file5.toFile().length()), f1.select("td").get(1).text());
  }

  @Test
  void complexFileSetListingNavigationUpTest(@TempDir Path tempDir) throws IOException {
    // GIVEN
    GivenForComplexFileSet given = GivenForComplexFileSet.prepare(this, tempDir);

    String listingUrl = given.baseUrl + "/";

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

    String listingUrlNext = given.baseUrl + nextPage;

    // emulate click on a folder 'input_folder2' link
    Document document = Jsoup.connect(listingUrlNext).get();

    // WHEN
    // emulate clicking UP link
    String listingUrlUp = given.baseUrl + document.select("a.up").attr("href");
    document = Jsoup.connect(listingUrlUp).get();

    // THEN
    checkCorrectListingRoot(given, document);
  }

  @Test
  void complexFileSetListingNavigationOk_1(@TempDir Path tempDir) throws IOException {
    // GIVEN
    GivenForComplexFileSet given = GivenForComplexFileSet.prepare(this, tempDir);

    String listingUrl = given.baseUrl + "/";

    //    System.out.println(getUrlToString(listingUrl));

    // WHEN
    TestsUtil.GetReply reply = TestsUtil.getUrl(listingUrl + "?f=1&name=");

    // THEN
    assertEquals(200, reply.statusCode);
  }

  @Test
  void complexFileSetListingNavigationOk_2(@TempDir Path tempDir) throws IOException {
    // GIVEN
    GivenForComplexFileSet given = GivenForComplexFileSet.prepare(this, tempDir);

    String listingUrl = given.baseUrl + "/";

    //    System.out.println(getUrlToString(listingUrl));

    // WHEN
    TestsUtil.GetReply reply = TestsUtil.getUrl(listingUrl + "?f=1&name=" + given.folderName3);

    // THEN
    assertEquals(200, reply.statusCode);
  }

  @Test
  void complexFileSetListingNavigation404_1(@TempDir Path tempDir) throws IOException {
    // GIVEN
    GivenForComplexFileSet given = GivenForComplexFileSet.prepare(this, tempDir);

    String listingUrl = given.baseUrl + "/";

    //    System.out.println(getUrlToString(listingUrl));

    // WHEN
    TestsUtil.GetReply reply = TestsUtil.getUrl(listingUrl + "?f=1&name=nonExistentFolder");

    // THEN
    assertEquals(404, reply.statusCode);
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
    private final String folderName3;
    private final String fname7;
    private final Path file7;
    private final String baseUrl;

    /**
     * Create file structure:
     *
     * <pre>
     *     /input_folder1/
     *        file1.txt
     *        file2.txt
     *        file3
     *     /input_folder2/
     *        file4.txt
     *        file5.csv
     *        /input_folder3/
     *            file6.html
     *     /FiLe777
     * </pre>
     */
    private static GivenForComplexFileSet prepare(ListingTests listingTests, Path tempDir)
        throws IOException {
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

      String folderName3 = "input_folder3";
      Path inputFolder3 = createTestFolder(inputFolder2, folderName3);
      createTestFolder(inputFolder3, "file6.html");

      String fname7 = "FiLe777";
      Path file7 = createTestFile(tempDir, fname7, "");

      Serv serv =
          new Serv(
              new String[] {
                "-p",
                testPort,
                inputFolder1.toFile().getAbsolutePath(),
                inputFolder2.toFile().getAbsolutePath(),
                file7.toFile().getAbsolutePath()
              });
      listingTests.serv = serv;
      InetSocketAddress address = serv.getAddress();

      String baseUrl = "http://" + address.getHostName() + ":" + address.getPort();

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
          folderName3,
          fname7,
          file7,
          baseUrl);
    }
  }
}
