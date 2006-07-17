/*
 * $Id: TestTypedEntryMap.java,v 1.3 2006-07-17 05:08:43 tlipkis Exp $
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

import org.lockss.test.*;

public class TestTypedEntryMap extends LockssTestCase {
  static String key = "key_string";
  static String wrongKey = "wrong_key_string";
  TypedEntryMap map;

  public void setUp() throws Exception {
    super.setUp();
    map = new TypedEntryMap();
  }

  public void testMapElements() {
    String value = "test_value";
    assertEquals(Collections.EMPTY_SET, map.entrySet());
    assertEquals(Collections.EMPTY_SET, map.keySet());
    assertFalse(map.containsKey(key));

    map.setMapElement(key, value);
    assertEquals(1, map.entrySet().size());
    assertEquals(1, map.keySet().size());
    assertEquals(value, map.getMapElement(key));
    assertTrue(map.containsKey(key));

    map.removeMapElement(key);
    assertEquals(Collections.EMPTY_SET, map.entrySet());
    assertEquals(Collections.EMPTY_SET, map.keySet());
    assertFalse(map.containsKey(key));
  }


  public void testString() {
    String value = "test_value";
    String defaultValue = "default_string";

    map.putString(key, value);
   // using default, still returns value
    assertEquals(value, map.getString(key, defaultValue));
    assertNull(map.getString(wrongKey, null));
    assertEquals(defaultValue, map.getString(wrongKey, defaultValue));
    // no default, returns value
    assertEquals(value, map.getString(key));
    try {
      // no entry, throws
      map.getString(wrongKey);
      assertFalse("failed to throw, when no entry and no default given.",false);
    }
    catch(NoSuchElementException ex) {
    }
  }

  public void testBoolean() {
    boolean value = false;
    boolean defaultValue = true;

    map.putBoolean(key, value);
    assertEquals(value, map.getBoolean(key, defaultValue));

    assertEquals(defaultValue, map.getBoolean(wrongKey, defaultValue));
    // no default, returns value
    assertEquals(value, map.getBoolean(key));
    try {
      // no entry, throws
      map.getBoolean(wrongKey);
      assertFalse("failed to throw, when no entry and no default given.",false);
    }
    catch(NoSuchElementException ex) {
    }
  }

  public void testDouble() {
    double value = 1.0d;
    double defaultValue = 2.0d;

    map.putDouble(key, value);
    assertEquals(value, map.getDouble(key, defaultValue), 0);
    assertEquals(defaultValue, map.getDouble(wrongKey, defaultValue), 0);
    // no default, returns value
    assertEquals(value, map.getDouble(key),0);
    try {
      // no entry, throws
      map.getDouble(wrongKey);
      assertFalse("failed to throw, when no entry and no default given.",false);
    }
    catch(NoSuchElementException ex) {
    }
  }

  public void testFloat() {
   float value = 1.0f;
    float defaultValue = 2.0f;

    map.putFloat(key, value);
    assertEquals(value, map.getFloat(key, defaultValue),0);
    assertEquals(defaultValue, map.getFloat(wrongKey, defaultValue),0);
    // no default, returns value
    assertEquals(value, map.getFloat(key),0);
    try {
      // no entry, throws
      map.getFloat(wrongKey);
      assertFalse("failed to throw, when no entry and no default given.",false);
    }
    catch(NoSuchElementException ex) {
    }
  }

  public void testInt() {
    int value = 1;
    int defaultValue = 2;

    map.putInt(key, value);
    assertEquals(value, map.getInt(key, defaultValue));
    assertEquals(defaultValue, map.getInt(wrongKey, defaultValue));
    // no default, returns value
    assertEquals(value, map.getInt(key));
    try {
      // no entry, throws
      map.getInt(wrongKey);
      assertFalse("failed to throw, when no entry and no default given.",false);
    }
    catch(NoSuchElementException ex) {
    }
  }

  public void testLong() {
    long value = 1;
    long defaultValue = 2;

    map.putLong(key, value);
    assertEquals(value, map.getLong(key, defaultValue));
    assertEquals(defaultValue, map.getLong(wrongKey, defaultValue));
    // no default, returns value
    assertEquals(value, map.getLong(key));
    try {
      // no entry, throws
      map.getLong(wrongKey);
      assertFalse("failed to throw, when no entry and no default given.",false);
    }
    catch(NoSuchElementException ex) {
    }
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

    // no default, returns value
    assertEquals(value, map.getUrl(key));
    try {
      // no entry, throws
      map.getUrl(wrongKey);
      assertFalse("failed to throw, when no entry and no default given.",false);
    }
    catch(NoSuchElementException ex) {
    }
  }

  public void testCollection() {
    Collection value = ListUtil.list("One", "Two", "Three");
    Collection defaultValue = ListUtil.list("Four", "Five", "Six");

    map.putCollection(key, value);
    assertEquals(value, map.getCollection(key, defaultValue));
    assertNull(map.getCollection(wrongKey, null));
    assertEquals(defaultValue, map.getCollection(wrongKey, defaultValue));
    // no default, returns value
    assertEquals(value, map.getCollection(key));
    try {
      // no entry, throws
      map.getCollection(wrongKey);
      assertFalse("failed to throw, when no entry and no default given.",false);
    }
    catch(NoSuchElementException ex) {
    }
  }

  public void testMap() {
    Map value = new HashMap();
    value.put("ValueMap", "Entry");
    Map defaultValue = new HashMap();
    defaultValue.put("DefaultMap", "DefaultEntry");

    map.putMap(key, value);
    assertEquals(value, map.getMap(key, defaultValue));
    assertNull(map.getMap(wrongKey, null));
    assertEquals(defaultValue, map.getMap(wrongKey, defaultValue));
    // no default, returns value
    assertEquals(value, map.getMap(key));
    try {
      // no entry, throws
      map.getMap(wrongKey);
      assertFalse("failed to throw, when no entry and no default given.",false);
    }
    catch(NoSuchElementException ex) {
    }
  }
}
