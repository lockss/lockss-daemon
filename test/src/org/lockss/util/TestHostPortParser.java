/*
 * $Id: TestHostPortParser.java,v 1.2 2010-02-22 07:04:48 tlipkis Exp $
 */

/*

Copyright (c) 20010 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.test.*;

/**
 * Test class for org.lockss.util.HostPortParser
 */

public class TestHostPortParser extends LockssTestCase {

  public static Class testedClasses[] = {
    org.lockss.util.HostPortParser.class
  };

  public void testNull() throws HostPortParser.InvalidSpec {
    HostPortParser p1 = new HostPortParser(null);
    assertEquals(null, p1.getHost());
    p1 = new HostPortParser("");
    assertEquals(null, p1.getHost());
    p1 = new HostPortParser("none");
    assertEquals(null, p1.getHost());
    p1 = new HostPortParser("None");
    assertEquals(null, p1.getHost());
    p1 = new HostPortParser("NONE");
    assertEquals(null, p1.getHost());
    p1 = new HostPortParser("direct");
    assertEquals(null, p1.getHost());
    p1 = new HostPortParser("DirecT");
    assertEquals(null, p1.getHost());
    p1 = new HostPortParser("DIRECT");
    assertEquals(null, p1.getHost());
  }

  public void testName() throws HostPortParser.InvalidSpec {
    HostPortParser p1 = new HostPortParser("foo.bar:3245");
    assertEquals("foo.bar", p1.getHost());
    assertEquals(3245, p1.getPort());
  }

  public void testV4() throws HostPortParser.InvalidSpec {
    HostPortParser p1 = new HostPortParser("10.4.5.123:80");
    assertEquals("10.4.5.123", p1.getHost());
    assertEquals(80, p1.getPort());
  }

  public void testV6() throws HostPortParser.InvalidSpec {
    HostPortParser p1 =
      new HostPortParser("2001:0db8:85a3:0000:0000:8a2e:0370:7334:80");
    assertEquals("2001:0db8:85a3:0000:0000:8a2e:0370:7334", p1.getHost());
    assertEquals(80, p1.getPort());
  }

  public void testIsDirect() {
    assertTrue(HostPortParser.isDirect("direct"));
  }


  public void testIll() {
    try {
      new HostPortParser("foo.bar:32gb");
      fail("Should throw: foo.bar:32gb");
    } catch (HostPortParser.InvalidSpec e) {
    }
    try {
      new HostPortParser("foo.bar");
      fail("Should throw: foo.bar");
    } catch (HostPortParser.InvalidSpec e) {
    }
    try {
      new HostPortParser("foo.bar:");
      fail("Should throw: foo.bar:");
    } catch (HostPortParser.InvalidSpec e) {
    }
    try {
      new HostPortParser(":81");
      fail("Should throw: :81");
    } catch (HostPortParser.InvalidSpec e) {
    }
  }
}
