/*
 * $Id$
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
 * This is the test class for org.lockss.alert.AlertLog
 */
public class TestAlertLog extends LockssTestCase {
  AlertLog alog;
  Alert a1, a2, a3, a4;

  public void setUp() throws Exception {
    super.setUp();
    a1 = alert("a1", 2);
    a2 = alert("a2", 2);
    a3 = alert("a3", 6);
    a4 = alert("a4", 8);
    alog = new AlertLog("alname", 3);
  }

  Alert alert(String name, int severity) {
    Alert a = new Alert(name);
    a.setAttribute(Alert.ATTR_SEVERITY, severity);
    return a;
  }

  public void testAdd() {
    alog.add(a1);
    alog.add(a2);
    alog.add(a3);
    assertEquals(3, alog.size());
    assertIsomorphic(ListUtil.list(a1, a2, a3), listOf(alog));
    alog.add(a1);
    assertEquals(3, alog.size());
    assertIsomorphic(ListUtil.list(a2, a3, a1), listOf(alog));
  }

  public void testIsEmpty() {
    AlertLog h = new AlertLog("foo", 3);
    assertTrue(h.isEmpty());
    h.add(a1);
    assertFalse(h.isEmpty());
  }

  public void testSize() {
    AlertLog h = new AlertLog("foo", 3);
    assertEquals(0, h.size());
    h.add(a1);
    assertEquals(1, h.size());
    h.add(a2);
    assertEquals(2, h.size());
  }

  public void testChangeSize() {
    AlertLog h = new AlertLog("foo", 4);
    h.add(a1);
    h.add(a2);
    h.add(a3);
    h.add(a4);
    assertIsomorphic(ListUtil.list(a1, a2, a3, a4), listOf(h));
    assertEquals(4, h.size());
    h.setMax(3);
    assertIsomorphic(ListUtil.list(a2, a3, a4), listOf(h));
    assertEquals(3, h.size());
    h.setMax(2);
    assertIsomorphic(ListUtil.list(a3, a4), listOf(h));
    h.add(a1);
    assertIsomorphic(ListUtil.list(a4, a1), listOf(h));
    h.setMax(3);
    h.add(a3);
    assertIsomorphic(ListUtil.list(a4, a1, a3), listOf(h));
  }

  List listOf(AlertLog log) {
    return ListUtil.fromIterator(log.iterator());
  }
}
