/*
 * $Id: TestAlertPatterns.java,v 1.1.90.1 2009-06-09 05:47:47 tlipkis Exp $
 */

/*

Copyright (c) 2000-2004 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.alert;




import java.io.*;
import java.util.*;
import java.net.*;
import java.text.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * This is the test class for org.lockss.alert.AlertPatterns
 */
public class TestAlertPatterns extends LockssTestCase {
  static String ATTR1 = "attr1";
  static String ATTR2 = "attr2";

  Alert a1;
  AlertPattern pat1;

  public void setUp() throws Exception {
    super.setUp();
    a1 = new Alert("alert1");
    a1.setAttribute(ATTR1, "test text");
    a1.setAttribute(ATTR2, 7);
  }

  void assertMatch(AlertPattern pat) {
    assertTrue(pat.isMatch(a1));
  }

  void assertNoMatch(AlertPattern pat) {
    assertFalse(pat.isMatch(a1));
  }

  public void testPred() {
    assertMatch(AlertPatterns.EQ(ATTR1, "test text"));
    assertMatch(AlertPatterns.EQ(ATTR2, new Integer(7)));
    assertNoMatch(AlertPatterns.NE(ATTR1, "test text"));
    assertNoMatch(AlertPatterns.NE(ATTR2, new Integer(7)));

    assertMatch(AlertPatterns.GT(ATTR2, new Integer(6)));
    assertNoMatch(AlertPatterns.GT(ATTR2, new Integer(7)));
    assertNoMatch(AlertPatterns.GT(ATTR2, new Integer(8)));
    assertMatch(AlertPatterns.GE(ATTR2, new Integer(6)));
    assertMatch(AlertPatterns.GE(ATTR2, new Integer(7)));
    assertNoMatch(AlertPatterns.GE(ATTR2, new Integer(8)));

    assertNoMatch(AlertPatterns.LT(ATTR2, new Integer(6)));
    assertNoMatch(AlertPatterns.LT(ATTR2, new Integer(7)));
    assertMatch(AlertPatterns.LT(ATTR2, new Integer(8)));
    assertNoMatch(AlertPatterns.LE(ATTR2, new Integer(6)));
    assertMatch(AlertPatterns.LE(ATTR2, new Integer(7)));
    assertMatch(AlertPatterns.LE(ATTR2, new Integer(8)));

    assertMatch(AlertPatterns.CONTAINS(ATTR1, ListUtil.list("four",
							    "test text")));
    assertNoMatch(AlertPatterns.CONTAINS(ATTR1, ListUtil.list("four",
							      "testing 123")));
  }

  public void testBool() {
    AlertPattern t1 = AlertPatterns.EQ(ATTR1, "test text");
    AlertPattern f1 = AlertPatterns.EQ(ATTR1, "foo bar");
    AlertPattern t2 = AlertPatterns.GT(ATTR2, new Integer(4));
    AlertPattern f2 = AlertPatterns.LT(ATTR2, new Integer(4));

    assertMatch(AlertPatterns.Not(f1));
    assertNoMatch(AlertPatterns.Not(t1));

    assertMatch(AlertPatterns.And(Collections.EMPTY_LIST));
    assertMatch(AlertPatterns.And(ListUtil.list(t1)));
    assertNoMatch(AlertPatterns.And(ListUtil.list(f1)));
    assertMatch(AlertPatterns.And(ListUtil.list(t1, t2)));
    assertNoMatch(AlertPatterns.And(ListUtil.list(t1, f2)));
    assertNoMatch(AlertPatterns.And(ListUtil.list(f1, t2)));
    assertNoMatch(AlertPatterns.And(ListUtil.list(f1, f2)));

    assertNoMatch(AlertPatterns.Or(Collections.EMPTY_LIST));
    assertMatch(AlertPatterns.Or(ListUtil.list(t1)));
    assertNoMatch(AlertPatterns.Or(ListUtil.list(f1)));
    assertMatch(AlertPatterns.Or(ListUtil.list(t1, t2)));
    assertMatch(AlertPatterns.Or(ListUtil.list(t2, f1)));
    assertMatch(AlertPatterns.Or(ListUtil.list(f1, t2)));
    assertNoMatch(AlertPatterns.Or(ListUtil.list(f1, f2)));
  }
}
