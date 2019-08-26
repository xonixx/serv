package com.cmlteam.serv;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ServTests {
  @Test
  void testServeSingleFileUncompressed(@TempDir Path tempDir) throws IOException {
    Path file = tempDir.resolve("file.txt");

    Files.write(file, "hello world 123".getBytes(StandardCharsets.UTF_8));

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

    assertEquals(inputFile.length(), resultFile.length(), "file size must be same");
    assertThat(resultFile)
        .describedAs("files should have same content")
        .hasSameContentAs(inputFile);

    serv.stop();
  }
}
