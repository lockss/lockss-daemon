/*
 * $Id: TestExternalizableMap.java,v 1.4 2004-04-28 22:52:06 clairegriffin Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.util;

import java.net.*;
import java.util.*;
import java.io.File;
import org.lockss.test.LockssTestCase;
import org.lockss.daemon.ConfigParamDescr;

public class TestExternalizableMap extends LockssTestCase {
  static String key = "key_string";
  static String wrongKey = "wrong_key_string";
  ExternalizableMap map;
  private String tempDirPath;

  public void setUp() throws Exception {
    super.setUp();
    map = new ExternalizableMap();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
  }

  public void testString() {
    String value = "test_value";
    String defaultValue = "default_string";

    map.putString(key, value);
    assertEquals(value, map.getString(key, defaultValue));
    assertNull(map.getString(wrongKey, null));
    assertEquals(defaultValue, map.getString(wrongKey, defaultValue));
  }

  public void testBoolean() {
    boolean value = false;
    boolean defaultValue = true;

    map.putBoolean(key, value);
    assertEquals(value, map.getBoolean(key, defaultValue));

    assertEquals(defaultValue, map.getBoolean(wrongKey, defaultValue));
  }

  public void testDouble() {
    double value = 1.0d;
    double defaultValue = 2.0d;

    map.putDouble(key, value);
    assertEquals(value, map.getDouble(key, defaultValue), 0);
    assertEquals(defaultValue, map.getDouble(wrongKey, defaultValue), 0);
  }

  public void testFloat() {
   float value = 1.0f;
    float defaultValue = 2.0f;

    map.putFloat(key, value);
    assertEquals(value, map.getFloat(key, defaultValue),0);
    assertEquals(defaultValue, map.getFloat(wrongKey, defaultValue),0);
  }

  public void testInt() {
    int value = 1;
    int defaultValue = 2;

    map.putInt(key, value);
    assertEquals(value, map.getInt(key, defaultValue));
    assertEquals(defaultValue, map.getInt(wrongKey, defaultValue));
  }

  public void testLong() {
    long value = 1;
    long defaultValue = 2;

    map.putLong(key, value);
    assertEquals(value, map.getLong(key, defaultValue));
    assertEquals(defaultValue, map.getLong(wrongKey, defaultValue));
  }

  public void testUrl() {
    URL value = null;
    URL defaultValue = null;
    try {
      value = new URL("http://www.example.com/");
      defaultValue = new URL("http://www.example2.com/");
    } catch (MalformedURLException ex) { }

    map.putUrl(key, value);
    assertEquals(value, map.getUrl(key, defaultValue));
    assertNull(map.getUrl(wrongKey, null));
    assertEquals(defaultValue, map.getUrl(wrongKey, defaultValue));

  }

  public void testCollection() {
    Collection value = ListUtil.list("One", "Two", "Three");
    Collection defaultValue = ListUtil.list("Four", "Five", "Six");

    map.putCollection(key, value);
    assertEquals(value, map.getCollection(key, defaultValue));
    assertNull(map.getCollection(wrongKey, null));
    assertEquals(defaultValue, map.getCollection(wrongKey, defaultValue));
  }

/*
  public void testMap() {
    Map value = new HashMap();
    value.put("ValueMap", "Entry");
    Map defaultValue = new HashMap();
    defaultValue.put("DefaultMap", "DefaultEntry");

    map.putMap(key, value);
    assertEquals(value, map.getMap(key, defaultValue));
    assertNull(map.getMap(wrongKey, null));
    assertEquals(defaultValue, map.getMap(wrongKey, defaultValue));
  }
*/

  public void testMarshalling() throws MalformedURLException {
    Collection testCol = new ArrayList();
    testCol.add("string 1");
    testCol.add("string 2");
//    Map testMap = new HashMap();
  //  testMap.put("test 1", "value 1");
    //testMap.put("test 2", "value 2");
    URL testUrl = new URL("http://www.example.com");
    Collection testCpd = ListUtil.list(ConfigParamDescr.VOLUME_NUMBER,
                                      ConfigParamDescr.BASE_URL);
    map.putBoolean("test-b", false);
    map.putCollection("test-c", testCol);
    map.putDouble("test-d", 1.21);
    map.putFloat("test-f", (float)2.12);
    map.putInt("test-i", 123);
    map.putLong("test-l", 123321);
//    map.putMap("test-m", testMap);
    map.putString("test-s", "test string");
    map.putUrl("test-u", testUrl);
    // test for collections of ConfigParamDescr
    map.putCollection("test-cpd", testCpd);
    // marshal
    String fileLoc = tempDirPath + "testMap";
    String fileName = "testMap";
    map.storeMap(fileLoc, fileName, null);

    // new map
    map = new ExternalizableMap();
    assertTrue(map.getBoolean("test-b", true));
    assertIsomorphic(new ArrayList(),
                     map.getCollection("test-c", new ArrayList()));
    assertEquals(1.0, map.getDouble("test-d", 1.0), 0.01);
    assertEquals(1.0, map.getFloat("test-f", (float)1.0), 0.01);
    assertEquals(1, map.getInt("test-i", 1));
    assertEquals(1, map.getLong("test-l", 1));
//    assertEqual(new HashMap(), map.getMap("test-m", new HashMap()));
    assertEquals("foo", map.getString("test-s", "foo"));
    assertEquals(new URL("http://foo.com"),
                 map.getUrl("test-u", new URL("http://foo.com")));

    // unmarshal
    map.loadMap(fileLoc, fileName, null);
    assertFalse(map.getBoolean("test-b", true));
    assertIsomorphic(testCol,
                     map.getCollection("test-c", new ArrayList()));
    assertEquals(1.21, map.getDouble("test-d", 1.0), 0.01);
    assertEquals(2.12, map.getFloat("test-f", (float)1.0), 0.01);
    assertEquals(123, map.getInt("test-i", 1));
    assertEquals(123321, map.getLong("test-l", 1));
//    assertEqual(testMap, map.getMap("test-m", new HashMap()));
    assertEquals("test string", map.getString("test-s", "foo"));
    assertEquals(testUrl, map.getUrl("test-u", new URL("http://foo.com")));
    assertIsomorphic(testCpd,
                     map.getCollection("test-cpd", Collections.EMPTY_LIST));
  }

}
