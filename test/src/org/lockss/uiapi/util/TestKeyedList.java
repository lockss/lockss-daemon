/*
 * $Id: TestKeyedList.java,v 1.2 2005-10-11 05:52:20 tlipkis Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.uiapi.util;

import java.io.*;

import junit.framework.TestCase;
import org.lockss.test.*;


/**
 * Test Transaction components
 */
public class TestKeyedList extends LockssTestCase implements ApiParameters {

  private final static String     N1        = "name1";
  private final static String     N2        = "name2";
  private final static String     N3        = "name3";

  private final static String     V1        = "value1";
  private final static String     V2        = "value2";
  private final static String     V3        = "value3";

  private final static String[]   _names    = {N1, N2, N3};
  private final static String[]   _values   = {V1, V2, V3};

  private KeyedList  _keyedList;


  public void setUp() throws Exception {

    super.setUp();

    _keyedList = new KeyedList();
  }

  private int populateList() {
    int length = _names.length;

    for (int i = 0; i < length; i++) {
      _keyedList.put(_names[i], _values[i]);
    }

    return length;
  }

  /*
   * Verify basic state - can we populate the list?
   */
  public void testPopulate() throws Exception {
    int length;

    assertEquals(0, _keyedList.size());
    length = populateList();
    assertEquals(length, _keyedList.size());
  }

  /*
   * Is list order preserved?
   */
  public void testListOrder() throws Exception {
    int length = populateList();

    for (int i = 0; i < length; i++) {
      assertEquals(_names[i],  (String) _keyedList.getKey(i));
      assertEquals(_values[i], (String) _keyedList.getValue(i));
    }
  }

  /*
   * Fetch values by name?
   */
  public void testGet() throws Exception {
    String value;

    populateList();

    value = (String) _keyedList.get(N1);
    assertEquals(V1, value);

    value = (String) _keyedList.get(N3);
    assertEquals(V3, value);

    value = (String) _keyedList.get("no_such_value");
    assertNull(value);
  }

  /*
   * Verify support for multiple occurances of the same name in the list
   */
  public void testPutMultiple() throws Exception {
    int length;

    /*
     * Add multiple ocurances to the list
     *
     * Duplicate names should not be overwritted
     */
    length = populateList();
    assertEquals(_names.length * 1, length);

    length += populateList();
    assertEquals(_names.length * 2, length);

    assertEquals(_names[0],  (String) _keyedList.getKey(0));
    assertEquals(_values[0], (String) _keyedList.getValue(0));

    assertEquals(_names[0],  (String) _keyedList.getKey(0 + _names.length));
    assertEquals(_values[0], (String) _keyedList.getValue(0 + _names.length));
    /*
     * Order should be preserved
     */
    _keyedList.put("X", "abc");
    _keyedList.put("X", "def");
    _keyedList.put("X", "ghi");

    assertEquals("X",   (String) _keyedList.getKey(length));
    assertEquals("abc", (String) _keyedList.getValue(length));

    assertEquals("X",   (String) _keyedList.getKey(length + 2));
    assertEquals("ghi", (String) _keyedList.getValue(length + 2));
    /*
     * Get() should only fetch the first matching value
     */
    assertEquals("abc", (String) _keyedList.get("X"));
  }

  /*
   * Does put() guard against null names and values?
   */
  public void testPutNull() throws Exception {
    boolean error;

    error = false;
    try {
      _keyedList.put(null, "value");
    } catch (IllegalArgumentException exception) {
      error = true;
    }
    if (!error) {
      fail("put(null, value) should have thrown InvalidArgumentException");
    }
  }

  public static void main(String[] args) {
    String[] testCaseList = { TestKeyedList.class.getName() };

    junit.swingui.TestRunner.main(testCaseList);
  }
}
