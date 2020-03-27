package com.cmlteam.serv;

class Constants {
  private static final String VERSION = "%APP_VERSION%";
  private static final String VERSION_EXT = " (%GRAAL_VERSION%, %JAVA_VERSION%)";

  static final int DEFAULT_PORT = 17777;
  static final String UTILITY_NAME = "serv";
  static final String GITHUB = "https://github.com/xonixx/serv";

  // via getter such that the compiler won't inline it
  public static String getVersion() {
    return VERSION;
  }

  // via getter such that the compiler won't inline it
  public static String getFullVersion() {
    return VERSION + VERSION_EXT;
  }
}
