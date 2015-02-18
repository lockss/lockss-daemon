/*
 * $Id$
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

  public void testMarshalling() throws MalformedURLException {
    Collection testCol = new ArrayList();
    testCol.add("string 1");
    testCol.add("string 2");
    Map mapVal = new HashMap();
    mapVal.put("test 1", "value 1");
    mapVal.put("test 2", "value 2");
    URL testUrl = new URL("http://www.example.com");
    Collection testCpd = ListUtil.list(ConfigParamDescr.VOLUME_NUMBER,
                                      ConfigParamDescr.BASE_URL);
    map.putBoolean("test-b", false);
    map.putCollection("test-c", testCol);
    map.putDouble("test-d", 1.21);
    map.putFloat("test-f", (float)2.12);
    map.putInt("test-i", 123);
    map.putLong("test-l", 123321);
    map.putMap("test-m", mapVal);
    map.putString("test-s", "test string");
    map.putUrl("test-u", testUrl);
    // test for collections of ConfigParamDescr
    map.putCollection("test-cpd", testCpd);


    // marshal
    String fileLoc = tempDirPath + "testMap";
    String fileName = "testMap";
    try {
      map.storeMap(fileLoc, null);
      fail("Should have thrown (null file) but did not.");
    }
    catch(Exception ex) {
      // Exception successfully thrown
    }
    map.storeMap(fileLoc, fileName);

    // new map
    map = new ExternalizableMap();
    assertTrue(map.getBoolean("test-b", true));
    assertIsomorphic(new ArrayList(),
                     map.getCollection("test-c", new ArrayList()));
    assertEquals(1.0, map.getDouble("test-d", 1.0), 0.01);
    assertEquals(1.0, map.getFloat("test-f", (float)1.0), 0.01);
    assertEquals(1, map.getInt("test-i", 1));
    assertEquals(1, map.getLong("test-l", 1));
//    assertEquals(new HashMap(), map.getMap("test-m", new HashMap()));
    assertEquals("foo", map.getString("test-s", "foo"));
    assertEquals(new URL("http://foo.com"),
                 map.getUrl("test-u", new URL("http://foo.com")));

    // unmarshal
    try {
      map.loadMap(fileLoc, null);
      fail("Should have thrown (null file) but did not.");
    }
    catch(Exception ex) {
      // Exception successfully thrown
    }

    map.loadMap(fileLoc, fileName);
    assertFalse(map.getBoolean("test-b", true));
    assertIsomorphic(testCol,
                     map.getCollection("test-c", new ArrayList()));
    assertEquals(1.21, map.getDouble("test-d", 1.0), 0.01);
    assertEquals(2.12, map.getFloat("test-f", (float)1.0), 0.01);
    assertEquals(123, map.getInt("test-i", 1));
    assertEquals(123321, map.getLong("test-l", 1));
    assertEquals(mapVal, map.getMap("test-m", new HashMap()));
    assertEquals("test string", map.getString("test-s", "foo"));
    assertEquals(testUrl, map.getUrl("test-u", new URL("http://foo.com")));
    assertIsomorphic(testCpd,
                     map.getCollection("test-cpd", Collections.EMPTY_LIST));
  }

}
