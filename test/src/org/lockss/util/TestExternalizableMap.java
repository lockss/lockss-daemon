package org.lockss.util;

import org.lockss.test.LockssTestCase;
import java.net.URL;
import java.net.*;
import java.util.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class TestExternalizableMap extends LockssTestCase {
  static String key = "key_string";
  static String wrongKey = "wrong_key_string";

  public TestExternalizableMap(String msg) {
    super(msg);
  }

  public void testString() {
    ExternalizableMap map = new ExternalizableMap();

    String value = "test_value";
    String defaultValue = "default_string";

    map.putString(key, value);
    assertEquals(value, map.getString(key, defaultValue));
    assertNull(map.getString(wrongKey, null));
    assertEquals(defaultValue, map.getString(wrongKey, defaultValue));
  }

  public void testBoolean() {
    ExternalizableMap map = new ExternalizableMap();

    boolean value = false;
    boolean defaultValue = true;

    map.putBoolean(key, value);
    assertEquals(value, map.getBoolean(key, defaultValue));

    assertEquals(defaultValue, map.getBoolean(wrongKey, defaultValue));
  }

  public void testDouble() {
    ExternalizableMap map = new ExternalizableMap();

    double value = 1.0d;
    double defaultValue = 2.0d;

    map.putDouble(key, value);
    assertEquals(value, map.getDouble(key, defaultValue), 0);
    assertEquals(defaultValue, map.getDouble(wrongKey, defaultValue), 0);
  }

  public void testFloat() {
    ExternalizableMap map = new ExternalizableMap();

   float value = 1.0f;
    float defaultValue = 2.0f;

    map.putFloat(key, value);
    assertEquals(value, map.getFloat(key, defaultValue),0);
    assertEquals(defaultValue, map.getFloat(wrongKey, defaultValue),0);
  }

  public void testInt() {
    ExternalizableMap map = new ExternalizableMap();

    int value = 1;
    int defaultValue = 2;

    map.putInt(key, value);
    assertEquals(value, map.getInt(key, defaultValue));
    assertEquals(defaultValue, map.getInt(wrongKey, defaultValue));
  }

  public void testLong() {
    ExternalizableMap map = new ExternalizableMap();

    long value = 1;
    long defaultValue = 2;

    map.putLong(key, value);
    assertEquals(value, map.getLong(key, defaultValue));
    assertEquals(defaultValue, map.getLong(wrongKey, defaultValue));
  }

  public void testUrl() {
    ExternalizableMap map = new ExternalizableMap();

    URL value = null;
    URL defaultValue = null;
    try {
      value = new URL("www.example.com/");
      defaultValue = new URL("www.example2.com/");
    }
    catch (MalformedURLException ex) {
    }
    map.putUrl(key, value);
    assertEquals(value, map.getUrl(key, defaultValue));
    assertNull(map.getUrl(wrongKey, null));
    assertEquals(defaultValue, map.getUrl(wrongKey, defaultValue));

  }

  public void testCollection() {
    ExternalizableMap map = new ExternalizableMap();

    Collection value = ListUtil.list("One", "Two", "Three");
    Collection defaultValue = ListUtil.list("Four", "Five", "Six");

    map.putCollection(key, value);
    assertEquals(value, map.getCollection(key, defaultValue));
    assertNull(map.getCollection(wrongKey, null));
    assertEquals(defaultValue, map.getCollection(wrongKey, defaultValue));
  }

  public void testMap() {
    ExternalizableMap map = new ExternalizableMap();

    Map value = new HashMap();
    value.put("ValueMap", "Entry");
    Map defaultValue = new HashMap();
    defaultValue.put("DefaultMap", "DefaultEntry");

    map.putMap(key, value);
    assertEquals(value, map.getMap(key, defaultValue));
    assertNull(map.getMap(wrongKey, null));
    assertEquals(defaultValue, map.getMap(wrongKey, defaultValue));
  }
}