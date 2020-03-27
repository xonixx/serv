package com.cmlteam.serv;

class Constants {
  static final String VERSION = "1.0.0";
  private static final String VERSION_FULL = VERSION + " (%GRAAL_VERSION%, %JAVA_VERSION%)";
  static final int DEFAULT_PORT = 17777;
  static final String UTILITY_NAME = "serv";
  static final String GITHUB = "https://github.com/xonixx/serv";

  // via getter such that the compiler won't inline it
  public static String getFullVersion() {
    return VERSION_FULL;
  }
}
