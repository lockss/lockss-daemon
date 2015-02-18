/*
 * $Id$
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

import java.util.*;
import java.io.*;
import java.net.*;
import junit.framework.TestCase;
import org.lockss.test.*;

/**
 * Test class for <code>EncodedProperty</code>.
 */
public class TestEncodedProperty extends LockssTestCase {

  private EncodedProperty p1;
  private EncodedProperty p2;
  private ArrayList nestedPropList;

  private byte[] testArray = new byte[] {
    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
  };

  // Base64-encoded version of the above byte array.  If the array
  // changes, this will need to change.
  private String b64TestArray = "AQIDBAUGBwgJCgsMDQ4P";

  public void setUp() throws Exception {
    super.setUp();
    p1 = new EncodedProperty();
    p1.putBoolean("boolean", true);
    p1.putInt("int", 1);
    p1.putLong("long", 280000000L);
    p1.putFloat("float", 3.14f);
    p1.putDouble("double", 3.14159265);
    p1.putByteArray("bytearray", testArray);
    p1.setProperty("encbytearray", b64TestArray);
    p1.setProperty("string", "String Value");

    // Add the nested property.
    p2 = new EncodedProperty();
    p2.setProperty("test", "Nested Value");
    p1.putEncodedProperty("nested", p2);

    // Add a two-level-deep nested property
    EncodedProperty p3 = new EncodedProperty();
    EncodedProperty p4 = new EncodedProperty();
    p4.setProperty("test", "Double Nested");
    p3.putEncodedProperty("nested2", p4);
    p1.putEncodedProperty("nested1", p3);

    // Add a list of nested properties.
    EncodedProperty p5 = new EncodedProperty();
    EncodedProperty p6 = new EncodedProperty();
    EncodedProperty p7 = new EncodedProperty();
    p5.setProperty("foo", "bar0");
    p6.setProperty("foo", "bar1");
    p7.setProperty("foo", "bar2");
    nestedPropList = (ArrayList)ListUtil.list(p5, p6, p7);
    p1.putEncodedPropertyList("nestedlist", nestedPropList);
  }

  public void testGetters() throws IOException {
    assertEquals(true, p1.getBoolean("boolean", false));
    assertEquals(1, p1.getInt("int", 0));
    assertEquals(280000000L, p1.getLong("long", 0L));
    assertEquals(3.14f, p1.getFloat("float", 0.0f), 0.0);
    assertEquals(3.14159265, p1.getDouble("double", 0.0), 0.0);
    assertEquals(testArray, p1.getByteArray("bytearray", ByteArray.EMPTY_BYTE_ARRAY));
    assertEquals(testArray, p1.getByteArray("encbytearray", ByteArray.EMPTY_BYTE_ARRAY));
    assertEquals(b64TestArray, p1.getProperty("encbytearray"));
    assertEquals("String Value", p1.getProperty("string"));

    // Test nested property
    EncodedProperty nested = p1.getEncodedProperty("nested");
    assertEquals(p2, nested);
    assertEquals("Nested Value", p2.getProperty("test"));

    // Test two-level nesting.
    EncodedProperty nested1 = p1.getEncodedProperty("nested1");
    EncodedProperty nested2 = nested1.getEncodedProperty("nested2");
    assertEquals("Double Nested", nested2.getProperty("test"));

    // Test nested property list
    List l = p1.getEncodedPropertyList("nestedlist");
    assertEquals(l.size(), nestedPropList.size());

    // Should be in the same order.
    for (int i = 0; i < l.size(); i++) {
      assertEquals((EncodedProperty)(l.get(i)),
		   (EncodedProperty)(nestedPropList.get(i)));
    }
  }

  public void testDefaultGetters() throws IOException {
    assertEquals(false, p1.getBoolean("foo", false));
    assertEquals(true, p1.getBoolean("foo", true));
    assertEquals(0, p1.getInt("foo", 0));
    assertEquals(1, p1.getInt("foo", 1));

    assertEquals(0L, p1.getLong("foo", 0L));
    assertEquals(1L, p1.getLong("foo", 1L));
    assertEquals(0.0f, p1.getFloat("foo", 0.0f), 0.0);
    assertEquals(1.0f, p1.getFloat("foo", 1.0f), 0.0);
    assertEquals(0.0, p1.getDouble("foo", 0.0), 0.0);
    assertEquals(1.0, p1.getDouble("foo", 1.0), 0.0);
    assertEquals(ByteArray.EMPTY_BYTE_ARRAY,
		 p1.getByteArray("foo", ByteArray.EMPTY_BYTE_ARRAY));
    assertNull(p1.getProperty("foo"));
    assertNull(p1.getEncodedProperty("foo"));
    assertNull(p1.getEncodedPropertyList("foo"));
  }

  public void testEncodeDecode() {
    EncodedProperty other = new EncodedProperty();
    try {
      byte[] encoded1 = p1.encode();
      other.decode(encoded1);
      assertEquals(p1, other);
    } catch (IOException ex) {
      fail("Should not have thrown.", ex);
    }
  }

  public void testNonDefaultEncodeDecode() {
    EncodedProperty other = new EncodedProperty();
    try {
      byte[] encoded2 = p1.encode("UTF-16");
      other.decode(encoded2, "UTF-16");
      assertEquals(p1, other);
    } catch (IOException ex) {
      fail("Should not have thrown.", ex);
    }
  }

  public void testBadEncoding() {
    EncodedProperty other = new EncodedProperty();
    try {
      byte[] encoded2 = p1.encode("UTF-3"); // unsupported
      fail("Should have thrown!");
    } catch (java.io.UnsupportedEncodingException ex) {
      // This is expected.
    } catch (IOException ex) {
      fail("Should not have thrown IO Exception", ex);
    }
  }

  public void testBadDecoding() {
    EncodedProperty other = new EncodedProperty();
    try {
      byte[] encoded2 = p1.encode("UTF-8");
      other.decode(encoded2, "UTF-3"); // unsupported
      fail("Should have thrown!");
    } catch (java.io.UnsupportedEncodingException ex) {
      // This is expected.
    } catch (IOException ex) {
      fail("Should not have thrown IO Exception", ex);
    }
  }
}
