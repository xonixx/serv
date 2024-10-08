package com.cmlteam.serv;

import com.cmlteam.util.Util;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.cmlteam.serv.TestsUtil.createTestFile;
import static com.cmlteam.serv.TestsUtil.createTestFolder;
import static org.junit.jupiter.api.Assertions.*;

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

    serv = new Serv("-p", testPort, inputFolder.toFile().getAbsolutePath());

    InetSocketAddress address = serv.getAddress();

    String listingUrl = "http://" + address.getHostName() + ":" + address.getPort() + "/" + path;

    //    System.out.println(getUrlToString(listingUrl));

    // WHEN
    Document document = Jsoup.connect(listingUrl).get();
    DocumentAccessor doc = new DocumentAccessor(document);
    // THEN
//    System.out.println(document);

    assertEquals("Index of /", doc.getH1Text());
    assertFalse(doc.hasUpLink());

    assertEquals(List.of(fname1,fname2,fname3), doc.getFileNames());

    assertEquals(
        List.of(
          Util.renderFileSize(file1.toFile().length()),
          Util.renderFileSize(file2.toFile().length()),
          Util.renderFileSize(file3.toFile().length())),
        doc.getFileSizes());
  }

  @Test
  void listingFilesOrderingTest(@TempDir Path tempDir) throws IOException {
    // GIVEN
    Path inputFolder = createTestFolder(tempDir, "input_folder");

    List<String> filesUnordered = List.of("x","b.txt","a.txt","z","y");

    for (String f : filesUnordered) {
      createTestFile(inputFolder, f, "");
    }

    Path folder = createTestFolder(inputFolder, "folder");

    for (String f : filesUnordered) {
      createTestFile(folder, f, "");
    }

    serv = new Serv("-p", testPort, inputFolder.toFile().getAbsolutePath());

    InetSocketAddress address = serv.getAddress();

    // WHEN
    DocumentAccessor doc1 = new DocumentAccessor(Jsoup.connect("http://" + address.getHostName() + ":" + address.getPort()).get());
    String link = doc1.getFolderLinks().get("folder");
    DocumentAccessor doc2 = new DocumentAccessor(Jsoup.connect("http://" + address.getHostName() + ":" + address.getPort() + link).get());

    // THEN
    assertEquals(List.of("folder", "a.txt", "b.txt", "x", "y", "z"), doc1.getFileNames());
    assertEquals(List.of("a.txt", "b.txt", "x", "y", "z"), doc2.getFileNames());
  }

  @Test
  void shouldNotSeverOutsideTest(@TempDir Path tempDir) throws IOException {
    // GIVEN
    Path inputFolder = createTestFolder(tempDir, "input_folder");
    String fname1 = "file1.txt";
    Path file1 = createTestFile(inputFolder, fname1, "hello world 123");

    serv = new Serv("-p", testPort, inputFolder.toFile().getAbsolutePath());

    InetSocketAddress address = serv.getAddress();
    //    System.out.println(getUrlToString(listingUrl));

    // WHEN
    String listingUrl = "http://" + address.getHostName() + ":" + address.getPort() + "/?f=0&name=../../../../../../../../../";
    TestsUtil.GetReply reply = TestsUtil.getUrl(listingUrl);

    // THEN
    assertEquals(404, reply.statusCode);
  }

  @Test
  void basicFolderListingCorrectTitle(@TempDir Path tempDir) throws IOException {
    // GIVEN
    Path folder1 = createTestFolder(tempDir, "folder1");
    Path file1 = createTestFile(folder1, "f1.txt", "abc");

    Path folder2 = createTestFolder(folder1, "folder2");
    Path file2 = createTestFile(folder2, "f2.txt", "abc");

    serv = new Serv("-p", testPort, folder1.toFile().getAbsolutePath());

    InetSocketAddress address = serv.getAddress();

    String listingUrl = "http://" + address.getHostName() + ":" + address.getPort() + "/";

    //    System.out.println(getUrlToString(listingUrl));

    Document document = Jsoup.connect(listingUrl).get();
    DocumentAccessor doc = new DocumentAccessor(document);

//    System.out.println(document);

//    System.out.println(href);

    String listingUrl1 = "http://" + address.getHostName() + ":" + address.getPort() + doc.getFolderLinks().get("folder2");

    // WHEN
    Document document1 = Jsoup.connect(listingUrl1).get();
    DocumentAccessor doc1 = new DocumentAccessor(document1);
//    System.out.println(document1);

    // THEN
    assertEquals("Index of /", doc.getH1Text());
    assertEquals("Index of /" + folder2.getFileName() + "/", doc1.getH1Text());
    assertEquals("/",doc1.getUpLink());
    assertEquals("/dlRef?name=folder2%2F",doc1.getTarLink());
    assertEquals("/dlRef?name=folder2%2F&z",doc1.getTarGzLink());
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
            "-p", testPort, file1.toFile().getAbsolutePath(), file3.toFile().getAbsolutePath());

    InetSocketAddress address = serv.getAddress();

    String listingUrl = "http://" + address.getHostName() + ":" + address.getPort() + "/";

    //    System.out.println(getUrlToString(listingUrl));

    // WHEN
    Document document = Jsoup.connect(listingUrl).get();
    DocumentAccessor doc = new DocumentAccessor(document);

    // THEN
    System.out.println(document);

    assertEquals("Index of /", doc.getH1Text());
    assertFalse(doc.hasUpLink());
    assertEquals(List.of(fname1,fname3), doc.getFileNames());
    assertEquals(
        List.of(Util.renderFileSize(file1.toFile().length()),
          Util.renderFileSize(file3.toFile().length())),
        doc.getFileSizes());
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
    DocumentAccessor doc = new DocumentAccessor(document);

    assertEquals("Index of /", doc.getH1Text());
    assertFalse(doc.hasUpLink());
    assertEquals(List.of(given.folderName1,given.folderName2,given.fname7),doc.getFileNames());

    Map<String, String> folderLinks = doc.getFolderLinks();
    assertEquals("/?f=0&name=", folderLinks.get(given.folderName1));
    assertEquals("/?f=1&name=", folderLinks.get(given.folderName2));
    assertEquals(
        List.of("","",Util.renderFileSize(given.file7.toFile().length())),
        doc.getFileSizes());
  }

  @Test
  void complexFileSetListingNavigationTest(@TempDir Path tempDir) throws IOException {
    // GIVEN
    GivenForComplexFileSet given = GivenForComplexFileSet.prepare(this, tempDir);

    String listingUrl = given.baseUrl + "/";

    //    System.out.println(getUrlToString(listingUrl));

    Document documentRoot = Jsoup.connect(listingUrl).get();

    String nextPage = new DocumentAccessor(documentRoot).getFolderLinks().get(given.folderName2);

    // WHEN
    // emulate click on a folder 'input_folder2' link
    String listingUrlNext = given.baseUrl + nextPage;
    Document document = Jsoup.connect(listingUrlNext).get();
    DocumentAccessor doc = new DocumentAccessor(document);

    // THEN
    System.out.println(document);

    assertEquals("Index of /" + given.folderName2 + "/", doc.getH1Text());
    assertTrue(doc.hasUpLink());
    assertEquals(List.of(given.folderName3,given.fname4,given.fname5), doc.getFileNames());
    assertEquals(List.of("",
            Util.renderFileSize(given.file4.toFile().length()),
            Util.renderFileSize(given.file5.toFile().length())),
          doc.getFileSizes());
  }

  @Test
  void complexFileSetListingNavigationUpTest(@TempDir Path tempDir) throws IOException {
    // GIVEN
    GivenForComplexFileSet given = GivenForComplexFileSet.prepare(this, tempDir);

    String listingUrl = given.baseUrl + "/";

    //    System.out.println(getUrlToString(listingUrl));

    Document documentRoot = Jsoup.connect(listingUrl).get();

    String nextPage = new DocumentAccessor(documentRoot).getFolderLinks().get(given.folderName2);;

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
              "-p",
              testPort,
              inputFolder1.toFile().getAbsolutePath(),
              inputFolder2.toFile().getAbsolutePath(),
              file7.toFile().getAbsolutePath());
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
