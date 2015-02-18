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
import java.net.*;
import java.io.*;
import org.lockss.test.*;


/**
 * Test class for <code>org.lockss.util.IPAddr</code>
 */

public class TestIPAddr extends LockssTestCase {

  public void testConstructor() throws Exception {
    try {
      IPAddr a1 = new IPAddr(null);
      fail("Should have thrown NullPointerException");
    } catch (NullPointerException e) {
    }
  }

  public void testAccessors() throws Exception {
    IPAddr a1 = IPAddr.getByName("1.2.3.4");
    InetAddress i1 = InetAddress.getByName("1.2.3.4");
    assertEquals(i1.getAddress(), a1.getAddress());
    assertEquals(i1.getHostAddress(), a1.getHostAddress());
    assertEquals(i1.hashCode(), a1.hashCode());
  }

  public void testEquals() throws Exception {
    IPAddr a1 = IPAddr.getByName("1.2.3.4");
    IPAddr a2 = IPAddr.getByName("1.2.3.4");
    IPAddr a3 = IPAddr.getByName("1.2.3.5");
    assertEquals(a1, a2);
    assertNotEquals(a1, a3);
    assertNotEquals(a1, "1.2.3.4");
  }

  public void testGetAllByName() throws Exception {
    IPAddr a[] = IPAddr.getAllByName("1.2.3.4");
    assertEquals(ListUtil.list(IPAddr.getByName("1.2.3.4")),
		 ListUtil.fromArray(a));
  }

  public void testIsLoopbackAddress() throws Exception {
    assertTrue(IPAddr.getByName("127.0.0.1").isLoopbackAddress());
    assertTrue(IPAddr.getByName("127.0.0.255").isLoopbackAddress());
    assertFalse(IPAddr.getByName("127.0.0.0").isLoopbackAddress());
    assertFalse(IPAddr.getByName("1.2.3.4").isLoopbackAddress());

    // static version
    assertTrue(IPAddr.isLoopbackAddress("127.0.0.1"));
    assertFalse(IPAddr.isLoopbackAddress("127.0.1.1"));
    assertFalse(IPAddr.isLoopbackAddress("127.0.0.0.1"));
  }
}
