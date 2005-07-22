/*
 * $Id: TestMaxDaemonVersion.java,v 1.1 2005-07-22 23:45:50 tlipkis Exp $
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
import org.lockss.test.*;

/**
 * Test class for org.lockss.util.MaxDaemonVersion
 */

public class TestMaxDaemonVersion extends LockssTestCase {

  String max(String[] args) {
    return MaxDaemonVersion.max(args);
  }

  public void testNone() {
    assertEquals("", max(new String[0]));
    assertEquals("", max(new String[] {"foo.bar"}));
    assertEquals("", max(new String[] {"foo.bar", "x.y"}));
  }

  public void testOne() {
    assertEquals("1.2.3", max(new String[] {"1.2.3"}));
    assertEquals("1-a.2.333", max(new String[] {"1-a.2.333"}));
  }

  public void testMany() {
    assertEquals("1.2.3", max(new String[] {"1.2.3", "1.1.1"}));
    assertEquals("1.2.3", max(new String[] {"1.1.1", "1.2.3"}));
    assertEquals("1.10.3", max(new String[] {"1.9.4", "1.10.3"}));
    assertEquals("1.42.3", max(new String[] {"1.1.1", "1.2.3",
					     "1.9.4", "1.42.3",
					     "1.7.1", "1.10.3"}));
  }

  public void testReturnsFirstAmongEquals() {
    assertEquals("1.42-v.3", max(new String[] {"1.1.1", "1.2.3",
					       "1.9.4", "1.42-v.3",
					       "1.42-b.3", "1.10.3"}));
    assertEquals("1.42-b.3", max(new String[] {"1.1.1", "1.2.3",
					       "1.9.4", "1.42-b.3",
					       "1.42-v.3", "1.10.3"}));
  }
}
