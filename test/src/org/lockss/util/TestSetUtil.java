/*
 * $Id: TestSetUtil.java,v 1.1 2003-01-15 18:17:17 tal Exp $
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
import junit.framework.TestCase;
import org.lockss.test.*;

/**
 * This is the test class for org.lockss.util.SetUtil
 */
public class TestSetUtil extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.util.SetUtil.class
  };

  public TestSetUtil(String msg) {
    super(msg);
  }
  
  private Set s1;

  public void setUp() throws Exception {
    super.setUp();
    s1 = new HashSet();
    s1.add("1");
    s1.add("2");
    s1.add("4");
  }

  public void testArgs() {
    assertEquals(s1, SetUtil.set("1", "2", "4"));
  }

  public void testFromArray() {
    String arr[] = {"1", "2", "4"};
    assertEquals(s1, SetUtil.fromArray(arr));
  }

  public void testFromCSV() {
    String csv = "1,2,4";
    assertEquals(s1, SetUtil.fromCSV(csv));
  }

  public void testFromIterator() {
    String arr[] = {"1", "2", "4"};
    assertEquals(s1, SetUtil.fromIterator(new ArrayIterator(arr)));
  }
}
