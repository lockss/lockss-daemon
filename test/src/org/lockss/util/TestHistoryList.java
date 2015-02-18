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
import java.io.*;
import junit.framework.TestCase;
import org.lockss.util.*;
import org.lockss.test.*;


/**
 * Test class for org.lockss.util.HistoryList
 */

public class TestHistoryList extends LockssTestCase {

  public void testIsEmpty() {
    HistoryList h = new HistoryList(3);
    assertTrue(h.isEmpty());
    h.add("foo");
    assertFalse(h.isEmpty());
  }

  public void testSize() {
    HistoryList h = new HistoryList(3);
    assertEquals(0, h.size());
    h.add("foo");
    assertEquals(1, h.size());
    h.add("bar");
    assertEquals(2, h.size());
  }

  public void testAdd() {
    HistoryList h = new HistoryList(3);
    h.add("1");
    h.add("3");
    h.add("2");
    assertIsomorphic(ListUtil.list("1", "3", "2"), h);
    h.add("0");
    assertIsomorphic(ListUtil.list("3", "2", "0"), h);
  }

  public void testChangeSize() {
    HistoryList h = new HistoryList(4);
    h.add("1");
    h.add("2");
    h.add("3");
    h.add("4");
    assertIsomorphic(ListUtil.list("1", "2", "3", "4"), h);
    assertEquals(4, h.size());
    h.setMax(3);
    assertIsomorphic(ListUtil.list("2", "3", "4"), h);
    assertEquals(3, h.size());
    h.setMax(2);
    assertIsomorphic(ListUtil.list("3", "4"), h);
    h.add("5");
    assertIsomorphic(ListUtil.list("4", "5"), h);
    h.setMax(3);
    h.add("6");
    assertIsomorphic(ListUtil.list("4", "5", "6"), h);
  }
}
