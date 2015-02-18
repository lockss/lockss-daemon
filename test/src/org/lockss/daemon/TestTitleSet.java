/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon;

import java.util.*;

import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * This is the test class for org.lockss.daemon.TitleSet
 */

public class TestTitleSet extends LockssTestCase {
  private static Logger log = Logger.getLogger("TestTitleSet");
  private MockLockssDaemon daemon;

  public void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon();
  }

  public void testIll() throws Exception {
    try {
      new TitleSetAllAus(null);
      fail("Should throw NullPointerException");
    } catch (NullPointerException e) {
    }
    try {
      TitleSetXpath.create(null, "T1", "[foo]");
      fail("Should throw NullPointerException");
    } catch (NullPointerException e) {
    }
    try {
      TitleSetXpath.create(daemon, null, "[foo]");
      fail("Should throw NullPointerException");
    } catch (NullPointerException e) {
    }
  }

  public void testName() throws Exception {
    TitleSet ts1 = TitleSetXpath.create(daemon, "Title a", "[foo]");
    TitleSet ts2 = TitleSetXpath.create(daemon, "Cedilla \u015Furf", "[foo]");
    TitleSet ts3 = TitleSetXpath.create(daemon, "E-accent-aigu \u00E9", "[fo]");
    assertEquals("Title a", ts1.getName());
    assertEquals("Title a", ts1.getId());
    assertEquals("Cedilla \u015Furf", ts2.getName());
    assertEquals("Cedilla surf", ts2.getId());
    assertEquals("E-accent-aigu \u00E9", ts3.getName());
    assertEquals("E-accent-aigu e", ts3.getId());
  }

  public void testSort() throws Exception {
    TitleSet tsAll1 = new TitleSetAllAus(daemon);
    TitleSet tsAll2 = new TitleSetActiveAus(daemon);
    TitleSet ts1 = TitleSetXpath.create(daemon, "Title a", "[foo]");
    TitleSet ts2 = TitleSetXpath.create(daemon, "Title B", "[foo]");
    TitleSet ts3 = TitleSetXpath.create(daemon, "Title c", "[foo]");
    assertCompareIsGreaterThan(ts1, tsAll1);
    assertCompareIsGreaterThan(ts1, tsAll2);
    assertCompareIsGreaterThan(ts2, ts1);
    assertCompareIsGreaterThan(ts3, ts2);
    assertCompareIsGreaterThan(ts3, ts1);
    List lst = ListUtil.list(ts2, ts1, ts3, tsAll2, tsAll1);
    Collections.sort(lst);
    assertEquals(ListUtil.list(tsAll1, tsAll2, ts1, ts2, ts3), lst);
  }
}
