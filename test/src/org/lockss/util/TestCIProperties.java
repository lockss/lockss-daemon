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

import junit.framework.TestCase;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import org.lockss.util.*;
import org.lockss.test.*;

/**
 * This is the test class for org.lockss.util.CIProperties
 */
public class TestCIProperties extends LockssTestCase {
  CIProperties p;

  public void setUp() throws Exception {
    p = new CIProperties();
  }

  public void testIsEmpty() {
    assertTrue(p.isEmpty());
    p.put("foo", "bar");
    assertFalse(p.isEmpty());
    p.remove("foo");
    assertTrue(p.isEmpty());
  }

  public void testIgn() {
    p.put("foo", "bar");
    assertEquals("bar", p.get("foo"));
    assertEquals("bar", p.get("FOO"));
    p.put("FOO", "123");
    assertEquals("123", p.get("foo"));
    assertEquals("123", p.get("Foo"));
    assertEquals("123", p.get("FOO"));
    p.remove("fOo");
    assertTrue(p.isEmpty());
  }

  public void testIgn2() {
    p.setProperty("foo", "bar");
    assertEquals("bar", p.getProperty("foo"));
    assertEquals("bar", p.getProperty("FOO"));
    p.setProperty("FOO", "123");
    assertEquals("123", p.getProperty("foo"));
    assertEquals("123", p.getProperty("Foo"));
    assertEquals("123", p.getProperty("FOO"));
    p.remove("fOo");
    assertTrue(p.isEmpty());
  }

  public void testFromProps() {
    Properties props = new Properties();
    props.setProperty("foo", "123");
    props.setProperty("BAR", "456");
    CIProperties t = CIProperties.fromProperties(props);
    assertEquals("123", t.getProperty("foo"));
    assertEquals("123", t.getProperty("FOO"));
    assertEquals("456", t.getProperty("Bar"));
    assertEquals("456", t.getProperty("BAR"));
    assertEquals("456", t.getProperty("bar"));
  }
}
