package com.cmlteam.serv;

import lombok.SneakyThrows;

import java.net.URI;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

class QueryParser {
  private final URI uri;
  private Map<String, String> parsedQuery;

  QueryParser(URI uri) {
    this.uri = uri;
  }

  String getParam(String param) {
    ensureParsed();
    return parsedQuery.get(param);
  }

  boolean hasParam(String param) {
    ensureParsed();
    return parsedQuery.containsKey(param);
  }

  private void ensureParsed() {
    if (parsedQuery == null) {
      parsedQuery = splitQuery(uri);
    }
  }

  @SneakyThrows
  private Map<String, String> splitQuery(URI uri) {
    Map<String, String> queryPairs = new LinkedHashMap<>();
    String query = uri.getQuery();
    if (query != null) {
      String[] pairs = query.split("&");
      if (pairs.length == 1) {
        queryPairs.put(pairs[0], null);
      } else {
        for (String pair : pairs) {
          int idx = pair.indexOf("=");
          queryPairs.put(
              URLDecoder.decode(pair.substring(0, idx), UTF_8.name()),
              URLDecoder.decode(pair.substring(idx + 1), UTF_8.name()));
        }
      }
    }
    return queryPairs;
  }
}
