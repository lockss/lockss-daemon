/*
* $Id: TestEncodedProperty.java,v 1.1 2002-10-04 17:37:40 claire Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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
public class TestEncodedProperty extends TestCase {
  public static Class testedClasses[] = {
    org.lockss.util.EncodedProperty.class
  };

  private static byte testbyte = 127;
  private static boolean testbool = true;
  private static byte[]  testarray = {
    1,2,3,4,5,6,7,8,9,10,11,12,13,14,15
  };
  private static double testdbl = 1.0d;
  private static float testfloat = 1.0f;
  private static int testint = 1;
  private static long testlong = 1280000000;

  private static Properties p1 = new Properties();
  static {
    p1.put("byte","127");
    p1.put("boolean","true");
    p1.put("bytearr", "0102030405060708090A0B0C0D0E0F");
    p1.put("double","1.0d");
    p1.put("float","1.0f");
    p1.put("int", "1");
    p1.put("long","1280000000");
  };

  public TestEncodedProperty(String msg) {
    super(msg);
  }

  public void testDefaultTransformation() {
    EncodedProperty props1 = new EncodedProperty(p1);
    EncodedProperty props2 = new EncodedProperty();
    byte[] encoded = null;
    try {
      encoded = props1.encode();
    }
    catch (IOException ex) {
      fail("prop encoding failed\n");
    }
    try {
      props2.decode(encoded);
    }
    catch (IOException ex) {
      fail("prop decoding failed\n");
    }
    assertTrue(PropUtil.equalProps(props1,props2));
  }

  public void testTransformation() {
    EncodedProperty props1 = new EncodedProperty(p1);
    EncodedProperty props2 = new EncodedProperty();
    byte[] encoded = null;
    try {
      encoded = props1.encode("UTF-16");
    }
    catch (IOException ex) {
      fail("prop encoding for UTF-16 failed\n");
    }
    try {
      props2.decode(encoded, "UTF-16");
    }
    catch (IOException ex) {
      fail("prop decoding UTF-16 failed\n");
    }
    assertTrue(PropUtil.equalProps(props1,props2));

  }


  public void testBooleanData() {
    EncodedProperty props = new EncodedProperty();

    // check request for missing type returns requested
    assertEquals(props.getBoolean("bool",testbool),testbool);

    // check request for expected item
    props.putBoolean("bool",testbool);
    assertEquals(props.getBoolean("bool",false), testbool);
  }

  public void testByteArrayData() {
    EncodedProperty props = new EncodedProperty();

    // check request for missing type returns requested
    assertEquals(props.getByteArray("bytearr",testarray),testarray);

    // check request for expected item
    props.putByteArray("bytearr",testarray);
    assertTrue(Arrays.equals(props.getByteArray("bytearr",new byte[0]),
                             testarray));
  }

  public void testDoubleData() {
    EncodedProperty props = new EncodedProperty();

    // check request for missing type returns requested
    assertEquals(props.getDouble("double",testdbl),testdbl,0);

    // check request for expected item
    props.putDouble("double",testdbl);
    assertEquals(props.getDouble("double",0.0d),testdbl,0);

  }

  public void testFloatData() {
    EncodedProperty props = new EncodedProperty();

    // check request for missing type returns requested
    assertEquals(props.getFloat("float",testfloat),testfloat,0);

    // check request for expected item
    props.putFloat("float",testfloat);
    assertEquals(props.getFloat("float",0.0f),testfloat,0);

  }

  public void testIntData() {
    EncodedProperty props = new EncodedProperty();

    // check request for missing type returns requested
    assertEquals(props.getInt("int",testint),testint);

    // check request for expected item
    props.putInt("int",testint);
    assertEquals(props.getInt("int",0),testint);

  }
  public void testLongData() {
    EncodedProperty props = new EncodedProperty();

    // check request for missing type returns requested
    assertEquals(props.getLong("long",testlong),testlong);

    // check request for expected item
    props.putLong("long",testlong);
    assertEquals(props.getLong("long",0),testlong);
  }


}