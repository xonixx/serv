package com.cmlteam.serv;

import com.cmlteam.util.Util;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;

import static com.cmlteam.serv.TestsUtil.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ListingTests {
  private static final String testPort = "18888";

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

    Serv serv = new Serv(new String[] {"-p", testPort, inputFolder.toFile().getAbsolutePath()});

    InetSocketAddress address = serv.getAddress();

    String listingUrl = "http://" + address.getHostName() + ":" + address.getPort() + "/listing";

    //    System.out.println(getUrlToString(listingUrl));

    // WHEN
    Document document = Jsoup.connect(listingUrl).get();

    // THEN
    System.out.println(document);

    Elements trElements = document.select("table tbody tr");

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

    Serv serv =
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

    assertEquals(2, trElements.size());
    assertEquals(fname1, trElements.get(0).select("td").first().text());
    assertEquals(fname3, trElements.get(1).select("td").first().text());
    assertEquals(
        Util.renderFileSize(file1.toFile().length()), trElements.get(0).select("td").get(1).text());
    assertEquals(
        Util.renderFileSize(file3.toFile().length()), trElements.get(1).select("td").get(1).text());
  }
}
