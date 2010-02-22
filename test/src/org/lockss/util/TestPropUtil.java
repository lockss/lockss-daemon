/*
 * $Id: TestPropUtil.java,v 1.18.10.1 2010-02-22 06:46:36 tlipkis Exp $
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
 * Test class for <code>org.lockss.util.PropUtil</code>
 */

public class TestPropUtil extends LockssTestCase {
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
  }

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

  public void testCopy() {
    Properties copy = PropUtil.copy(p1);
    assertTrue(PropUtil.equalProps(p1, copy));
    copy.setProperty("foo", "bar");
    assertEquals(null, p1.getProperty("foo"));
    copy.setProperty("k1", "2");
    assertEquals("1", p1.getProperty("k1"));
  }

  public void testFromArgs() {
    Properties test;
    Properties props = new Properties();

    test = PropUtil.fromArgs("k1", "v1");
    props.setProperty("k1", "v1");
    assertTrue(PropUtil.equalProps(props, test));

    test = PropUtil.fromArgs("k1", "v1", "k2", "v2");
    props.setProperty("k2", "v2");
    assertTrue(PropUtil.equalProps(props, test));

    test = PropUtil.fromArgs("k1", "v1", "k2", "v2", "k3", "v3");
    props.setProperty("k3", "v3");
    assertTrue(PropUtil.equalProps(props, test));
  }

  public void testFromFile() throws IOException {
    File tmp = FileTestUtil.writeTempFile("test", "foo=bar\nx.y=z\n");
    Properties exp = PropUtil.fromArgs("foo", "bar", "x.y", "z");
    Properties props = PropUtil.fromFile(tmp);
    assertEquals(exp, props);
  }

  public void testToFile() throws IOException {
    Properties props = new Properties();
    props.setProperty("k1", "1v");
    props.setProperty("k3", "3v");
    File tmp = FileTestUtil.writeTempFile("test", "foo=bar\nx.y=z\n");
    PropUtil.toFile(tmp, props);
    Properties p2 = PropUtil.fromFile(tmp);
    assertEquals("1v", p2.get("k1"));
    assertEquals("3v", p2.get("k3"));
    assertEquals(props, p2);
  }

  public void testDifferentKeys() {
    assertEquals(p1.keySet(), PropUtil.differentKeys(p1, null));
    assertEquals(p1.keySet(), PropUtil.differentKeys(null, p1));
    assertNull(PropUtil.differentKeys(null, null));
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

  public void testDifferentKeysAndPrefixesNull() {
    PropertyTree pt1 = new PropertyTree();
    try {
      PropUtil.differentKeysAndPrefixes(pt1, null);
      fail("differentKeysAndPrefixes with null arg should throw");
    } catch (NullPointerException e) {
    }
    try {
      PropUtil.differentKeysAndPrefixes(null, pt1);
      fail("differentKeysAndPrefixes with null arg should throw");
    } catch (NullPointerException e) {
    }
    try {
      PropUtil.differentKeysAndPrefixes(null, null);
      fail("differentKeysAndPrefixes with null arg should throw");
    } catch (NullPointerException e) {
    }
  }

  public void testDifferentKeysAndPrefixes() {
    PropertyTree pt1 = new PropertyTree();
    PropertyTree pt2 = new PropertyTree();
    pt1.put("one.two.three", "123");
    assertEquals(SetUtil.set("one.two.three", "one.two.", "one.two",
			     "one.", "one"),
		 PropUtil.differentKeysAndPrefixes(pt1, pt2));
  }

  public void testDifferentKeysAndPrefixes2() {
    PropertyTree pt1 = new PropertyTree();
    PropertyTree pt2 = new PropertyTree();
    pt1.put("x.y.", "123");
    assertEquals(SetUtil.set("x.y.", "x.y", "x.", "x"),
		 PropUtil.differentKeysAndPrefixes(pt1, pt2));
  }

  public void testDifferentKeysAndPrefixesCombination() {
    PropertyTree pt1 = new PropertyTree();
    PropertyTree pt2 = new PropertyTree();
    // same value
    pt1.put("foox.bar.blecch", "123");
    pt2.put("foox.bar.blecch", "123");
    // different value
    pt1.put("foo.bar.blecch", "123");
    pt2.put("foo.bar.blecch", "124");
    // same value in subtree in which there are differences
    pt1.put("foo.bar.bleep", "123");
    pt2.put("foo.bar.bleep", "123");
    // pt1-only key, tree
    pt1.put("foo.bar.gorp", "123");
    pt1.put("x.y", "123");
    // pt2-only key, tree
    pt2.put("foo.bar.blah", "124");
    pt2.put("bar.foo.blah", "124");
    String expa[] = {"foo.bar.blecch", "foo.bar.", "foo.bar", "foo.", "foo",
		    "foo.bar.gorp", "foo.bar.blah",
		     "bar.foo.blah", "bar.foo.", "bar.foo", "bar.", "bar",
		     "x.y", "x.", "x"};
    Set exp = SetUtil.fromArray(expa);
    assertEquals(exp, PropUtil.differentKeysAndPrefixes(pt1, pt2));
    assertEquals(exp, PropUtil.differentKeysAndPrefixes(pt2, pt1));
  }

  public void testDifferentKeysAndPrefixesDoubleDot() {
    // Due to oddities in PropertyTree (get(foo.bar) is same as get(foo..bar),
    // don't make any assertions about result, just ensure double dot doesn't
    // cause error
    PropertyTree pt1 = new PropertyTree();
    PropertyTree pt2 = new PropertyTree();
    pt1.put("foox.bar.blecch", "123");
    pt2.put("foo.bar..bleep", "123");
    PropUtil.differentKeysAndPrefixes(pt1, pt2);
    PropUtil.differentKeysAndPrefixes(pt2, pt1);
  }

  public void testPropsToEncodedStringNullProps() {
    assertEquals("", PropUtil.propsToCanonicalEncodedString(null));
  }

  public void testPropsToEncodedStringEmptyProps() {
    assertEquals("", PropUtil.propsToCanonicalEncodedString(new Properties()));
  }

  public void testPropsToEncodedStringOneElement() {
    Properties props = new Properties();
    props.setProperty("key1", "val1");
    assertEquals("key1~val1", PropUtil.propsToCanonicalEncodedString(props));
  }

  public void testPropsToEncodedStringMultipleElements() {
    Properties props = new Properties();
    props.setProperty("key1", "val1");
    props.setProperty("key2", "val2");
    String expected = "key1~val1&key2~val2";
    String actual = PropUtil.propsToCanonicalEncodedString(props);
    assertEquals(expected, actual);
  }

  public void testPropsToEncodedStringEncodePropStrings() {
    Properties props = new Properties();
    props.setProperty("key&1", "val=1");
    props.setProperty("key2", "val 2");
    props.setProperty("key.3", "val:3");
    props.setProperty("key4", "val.4");
    String actual = PropUtil.propsToCanonicalEncodedString(props);
    String expected =
      "key%261~val%3D1&"+
      "key%2E3~val%3A3&"+
      "key2~val+2&"+
      "key4~val%2E4";
    assertEquals(expected, actual);
  }

  public void testCanonicalEncodedStringToProps() {
    assertEmpty(PropUtil.canonicalEncodedStringToProps(null));
    assertEmpty(PropUtil.canonicalEncodedStringToProps(""));
    try {
      PropUtil.canonicalEncodedStringToProps("foo");
      fail("Illegal prop string should throw");
    } catch (IllegalArgumentException e) {
    }
    try {
      PropUtil.canonicalEncodedStringToProps("foo&");
      fail("Illegal prop string should throw");
    } catch (IllegalArgumentException e) {
    }
    try {
      PropUtil.canonicalEncodedStringToProps("foo~");
      fail("Illegal prop string should throw");
    } catch (IllegalArgumentException e) {
    }
    try {
      PropUtil.canonicalEncodedStringToProps("foo~bar~");
      fail("Illegal prop string should throw");
    } catch (IllegalArgumentException e) {
    }
  }

  void assertEncodedPropsInverse(Properties props) {
    String s = PropUtil.propsToCanonicalEncodedString(props);
    Properties newProps = PropUtil.canonicalEncodedStringToProps(s);
    assertEquals(props, newProps);
  }

  public void testEncodedPropsInverse() {
    assertEncodedPropsInverse(new Properties());

    Properties props = new Properties();
    props.setProperty("key&1", "val=1");
    props.setProperty("key2", "val 2");
    props.setProperty("key.3", "val:3");
    props.setProperty("key4", "val.4");
    assertEncodedPropsInverse(props);
  }

  public void testToHeaderString() {
    Properties props = new Properties();
    props.setProperty("key1", "val1");
    props.setProperty("key2", "val 2");
    assertEquals("key1: val1\r\nkey2: val 2\r\n",
		 PropUtil.toHeaderString(props));
  }

}
