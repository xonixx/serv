package com.cmlteam.serv;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class QueryParserTests {
  @Test
  void testSimple() {
    // WHEN
    QueryParser queryParser = new QueryParser("/aaa?param=value&param1=123");

    // THEN
    Assertions.assertTrue(queryParser.hasParam("param"));
    Assertions.assertTrue(queryParser.hasParam("param1"));
    Assertions.assertFalse(queryParser.hasParam("param2"));

    Assertions.assertEquals("value", queryParser.getParam("param"));
    Assertions.assertEquals("123", queryParser.getParam("param1"));
    Assertions.assertNull(queryParser.getParam("param2"));
  }

  @Test
  void testParamNoVal1() {
    // WHEN
    QueryParser queryParser = new QueryParser("/dl?z");

    // THEN
    Assertions.assertTrue(queryParser.hasParam("z"));
  }

  @Test
  void testParamNoVal2() {
    // WHEN
    QueryParser queryParser = new QueryParser("/dl?f=aaa&z");

    // THEN
    Assertions.assertTrue(queryParser.hasParam("f"));
    Assertions.assertTrue(queryParser.hasParam("z"));
  }

  @Test
  void testParamNoVal3() {
    // WHEN
    QueryParser queryParser = new QueryParser("/dl?z&f=aaa");

    // THEN
    Assertions.assertTrue(queryParser.hasParam("f"));
    Assertions.assertTrue(queryParser.hasParam("z"));
  }
}
