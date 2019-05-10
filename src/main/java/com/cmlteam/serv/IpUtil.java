package com.cmlteam.serv;

import java.net.DatagramSocket;
import java.net.InetAddress;

public class IpUtil {
  private IpUtil() {}

  /** TODO can we make this more reliable? */
  static String getLocalNetworkIp() {
    try (final DatagramSocket socket = new DatagramSocket()) {
      socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
      return socket.getLocalAddress().getHostAddress();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
