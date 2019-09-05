package com.cmlteam.serv;

import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Path;

import static com.cmlteam.serv.TestsUtil.createTestFile;
import static com.cmlteam.serv.TestsUtil.createTestFolder;

public class ListingTests {
  private static final String testPort = "18888";

  public void basicFolderListingTest(@TempDir Path tempDir) throws IOException {
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

    URL listingUrl =
        new URL("http://" + address.getHostName() + ":" + address.getPort() + "/listing");
  }
}
