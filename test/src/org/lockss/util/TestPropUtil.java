/*
 * $Id: TestPropUtil.java,v 1.3 2003-04-15 01:20:27 troberts Exp $
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
 * Test class for <code>org.lockss.util.PropUtil</code>
 */

public class TestPropUtil extends TestCase {
  public static Class testedClasses[] = {
    org.lockss.util.PropUtil.class
  };


  public TestPropUtil(String msg) {
    super(msg);
  }

//    public void setUp() {
//    }

  private static Properties p1 = new Properties();
  static {
    p1.put("k1", "1");
    p1.put("k2", "two");
  };

  private static Properties p2 = (Properties)p1.clone(); // p2 same as p1
  static {
    p2.put("k2", new String("two"));
  }

  private static Properties p3 = (Properties)p1.clone(); // p3 has a different value
  static {
    p3.put("k2", "not two");
  }

  private static Properties p4 = (Properties)p1.clone(); // p4 has an additional value
  static {
    p4.put("k3", "foo");
  }

  private static Properties p5 = (Properties)p1.clone(); // p5 omits one value
  static {
    p5.remove("k1");
  }

  private static Properties p6 = (Properties)p1.clone(); // p6 has multiple diffs
  static {
    p6.put("k3", "foo");
    p6.put("k2", "not two");
  }

  public void testEqual() {
    System.out.println("p1: "+p1);
    System.out.println("p2: "+p2);
    System.out.println("p3: "+p3);
    System.out.println("p4: "+p4);
    System.out.println("p5: "+p5);
    assertTrue(PropUtil.equalProps(p1, p2));
    assertFalse( PropUtil.equalProps(p1, p3));
    assertFalse( PropUtil.equalProps(p1, p4));
    assertFalse( PropUtil.equalProps(p1, p5));
    // commute args
    assertTrue(PropUtil.equalProps(p2, p1));
    assertFalse( PropUtil.equalProps(p3, p1));
    assertFalse( PropUtil.equalProps(p4, p1));
    assertFalse( PropUtil.equalProps(p5, p1));
  }

  public void testDifferentKeys() {
    assertEquals(SetUtil.set(), PropUtil.differentKeys(p1, p1));
    assertEquals(SetUtil.set(), PropUtil.differentKeys(p1, p2));
    assertEquals(SetUtil.set("k2"), PropUtil.differentKeys(p1, p3));
    assertEquals(SetUtil.set("k3"), PropUtil.differentKeys(p1, p4));
    assertEquals(SetUtil.set("k1"), PropUtil.differentKeys(p1, p5));
    assertEquals(SetUtil.set("k2","k3"), PropUtil.differentKeys(p1, p6));
    // commute args
    assertEquals(SetUtil.set(), PropUtil.differentKeys(p1, p1));
    assertEquals(SetUtil.set(), PropUtil.differentKeys(p2, p1));
    assertEquals(SetUtil.set("k2"), PropUtil.differentKeys(p3, p1));
    assertEquals(SetUtil.set("k3"), PropUtil.differentKeys(p4, p1));
    assertEquals(SetUtil.set("k1"), PropUtil.differentKeys(p5, p1));
    assertEquals(SetUtil.set("k2","k3"), PropUtil.differentKeys(p6, p1));
  }

  public void testPropsToEncodedStringNullProps() {
    assertEquals("", PropUtil.propsToEncodedString(null));
  }

  public void testPropsToEncodedStringEmptyProps() {
    assertEquals("", PropUtil.propsToEncodedString(new Properties()));
  }

  public void testPropsToEncodedStringOneElement() {
    Properties props = new Properties();
    props.setProperty("key1", "val1");
    assertEquals("key1~val1", PropUtil.propsToEncodedString(props));
  }

  public void testPropsToEncodedStringMultipleElements() {
    Properties props = new Properties();
    props.setProperty("key1", "val1");
    props.setProperty("key2", "val2");
    String encStr = PropUtil.propsToEncodedString(props);
    Set actual = new HashSet(StringUtil.breakAt(encStr, '&'));
    assertEquals(SetUtil.set("key1~val1", "key2~val2"), actual);
  }

  public void testPropsToEncodedStringEncodePropStrings() {
    Properties props = new Properties();
    props.setProperty("key&1", "val=1");
    props.setProperty("key2", "val 2");
    props.setProperty("key.3", "val:3");
    String encStr = PropUtil.propsToEncodedString(props);
    Set actual = new HashSet(StringUtil.breakAt(encStr, '&'));
    Set expected = SetUtil.set("key%261~val%3D1", 
			       "key2~val+2",
			       "key%2E3~val%3A3");
    assertEquals(expected, actual);
  }

}
