package com.cmlteam.serv;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.net.URI;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@RequiredArgsConstructor
class QueryParser {
  private final URI uri;
  private Map<String, String> parsedQuery;

  QueryParser(String uri) {
    this(URI.create(uri));
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
      for (String pair : pairs) {
        int idx = pair.indexOf("=");
        String key = idx < 0 ? pair : pair.substring(0, idx);
        String val = idx < 0 ? "" : pair.substring(idx + 1);
        queryPairs.put(URLDecoder.decode(key, UTF_8.name()), URLDecoder.decode(val, UTF_8.name()));
      }
    }
    return queryPairs;
  }
}
