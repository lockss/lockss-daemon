/*
 * $Id: TestIpFilter.java,v 1.1 2003-04-21 05:42:53 tal Exp $
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

import junit.framework.TestCase;
import java.io.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.test.*;

/**
 * This is the test class for org.lockss.util.IpFilter
 */
public class TestIpFilter extends LockssTestCase {
  IpFilter filt;

  public void setUp() throws Exception {
    super.setUp();
    filt = new IpFilter();
  }

  private static void shouldFail(String s, boolean maskOk) {
    try {
      IpFilter.Mask ip = new IpFilter.Mask(s, maskOk);
      fail(s + " should have failed, didn't: " + ip);
    } catch (IpFilter.MalformedException e) {
    }
  }

  private static void shouldSucceed(String s, boolean maskOk) {
    try {
      IpFilter.Mask ip = new IpFilter.Mask(s, maskOk);
    } catch (IpFilter.MalformedException e) {
      fail(s + " failed: " + e);
    }
  }

  public void checkMatch(String s1, String s2, boolean shouldMatch)
      throws IpFilter.MalformedException {
    IpFilter.Mask f1 = new IpFilter.Mask(s1, true);
    IpFilter.Mask f2 = new IpFilter.Mask(s2 ,true);
    boolean match = f1.match(f2);
    if (match != shouldMatch) {
      fail(f1 + ".match(" + f2 + ") was " + match +
	   ", should have been " + shouldMatch);
    }
  }

  public void checkFilter(String s, boolean shouldBeAllowed)
      throws IpFilter.MalformedException {
    IpFilter.Addr ip = new IpFilter.Addr(s);
    boolean match = filt.isIpAllowed(ip);
    if (match != shouldBeAllowed) {
      fail("isIpAllowed(" + ip + ") was " + match +
	   ", should have been " + shouldBeAllowed);
    }
  }

  public void testConstructor() throws Exception {
    shouldSucceed("127.0.0.1", false);
    shouldFail("127.0.1.0/24", false);
    shouldSucceed("127.0.1.0/24", true);
    shouldFail("123.45.12.*", false);
    shouldSucceed("123.45.12.*", true);
    shouldSucceed("123.45.*.*", true);

    shouldFail("36.48.*.0", true);
    shouldFail("36.48.0.23/33", true);
    shouldFail("36.48.0.4/29", true);
    shouldFail("36.48.0.a", false);
    shouldFail("36.48.0.2.3", false);
    shouldFail("36.48.0.2/", true);
  }

  public void testMatch() throws Exception {
    checkMatch("127.0.1.0/24", "127.0.1.0/24", true);
    checkMatch("127.0.1.0/24", "127.0.1.0", true);
    checkMatch("127.0.1.0/24", "127.0.1.255", true);
    checkMatch("127.0.1.255", "127.0.1.0/24", true);
    checkMatch("127.0.1.0/24", "127.2.1.0", false);
  }

  public void testFilter() throws Exception {
    filt.setFilters("172.16.25.*;10.0.4.1","172.16.25.128/25", ';');
    checkFilter("10.0.4.13", false);
    checkFilter("10.0.4.1", true);
    checkFilter("172.16.25.1", true);
    checkFilter("172.16.25.131", false);
  }

  public void testDefault() throws Exception {
    // default is to match nothing
    checkFilter("1.1.1.1", false);
  }
}
