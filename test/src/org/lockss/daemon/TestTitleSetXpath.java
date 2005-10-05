/*
 * $Id: TestTitleSetXpath.java,v 1.4 2005-10-05 06:05:26 tlipkis Exp $
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

package org.lockss.daemon;

import java.util.*;
import org.apache.commons.jxpath.*;
import org.lockss.config.*;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * This is the test class for org.lockss.daemon.TitleSetXpath
 */

public class TestTitleSetXpath extends LockssTestCase {
  private static Logger log = Logger.getLogger("TestTitleSetXpath");

  MockPlugin mp1;
  MockPlugin mp2;
  TitleConfig tc1, tc2, tc3, tc4, tc5, tc6;
  List titles;

  public void setUp() throws Exception {
    super.setUp();
    makeTitles();
  }

  public void makeTitles() {
    titles = new ArrayList();
    String pid1 = "o.l.plug1";
    String pid2 = "o.l.plug2";
    tc1 = new TitleConfig("Journal of Title, 2001", pid1);
    tc1.setJournalTitle("Journal of Title");
    tc2 = new TitleConfig("Journal of Title, 2002", pid1);
    tc2.setJournalTitle("Journal of Title");
    tc3 = new TitleConfig("Journal of Title, 2003", pid1);
    tc3.setJournalTitle("Journal of Title");
    tc4 = new TitleConfig("Journal of Title, 2004", pid2);
    tc4.setJournalTitle("Journal of Title");
    tc5 = new TitleConfig("LOCKSS Journal of Dogs", pid2);
    tc5.setJournalTitle("Dog Journal");
    tc6 = new TitleConfig("LOCKSS Journal of Dog Toys 2002", pid2);
    tc6.setJournalTitle("Dog Journal");

    ConfigParamDescr d1 =new ConfigParamDescr("key1");
    ConfigParamDescr d2 =new ConfigParamDescr("key2");

    tc5.setAttributes(MapUtil.map("key1", "val1", "k0", "1"));
    tc6.setAttributes(MapUtil.map("key1", "val2", "k0", "1"));

    titles = ListUtil.list(tc1, tc2, tc3, tc4, tc5, tc6);
  }

  TitleSetXpath newSet(String pred) {
    return new TitleSetXpath(getMockLockssDaemon(), "Name1", pred);
  }

  public void testConstructor() {
    MockLockssDaemon daemon = getMockLockssDaemon();
    TitleSetXpath ts = new TitleSetXpath(daemon, "Name1", "[path1]");
    assertEquals(".[path1]", ts.getPath());
    // Checked in TitleSetXpath constructor
    try {
      new TitleSetXpath(daemon, "Name1", null);
      fail("Null path should be illegal");
    } catch (NullPointerException e) {
    }
    try {
      new TitleSetXpath(daemon, "Name1", "path1]");
      fail("Pattern should be illegal");
    } catch (IllegalArgumentException e) {
    }
    try {
      new TitleSetXpath(daemon, "Name1", "[path1");
      fail("Pattern should be illegal");
    } catch (IllegalArgumentException e) {
    }
    // Illegal predicate syntax, checked in JXPath code
    try {
      new TitleSetXpath(daemon, "Name1", "[journalTitle='Dog Journal']]");
      fail("Illegal xpath, should have thrown");
    } catch (JXPathException e) {
    }
  }

  public void testEquals() {
    MockLockssDaemon daemon = getMockLockssDaemon();
    assertEquals(new TitleSetXpath(daemon, "Name1", "[path1]"),
		 new TitleSetXpath(daemon, "Name1", "[path1]"));
    assertNotEquals(new TitleSetXpath(daemon, "Name1", "[path1]"),
		    new TitleSetXpath(daemon, "Name2", "[path1]"));
    assertNotEquals(new TitleSetXpath(daemon, "Name1", "[path1]"),
		    new TitleSetXpath(daemon, "Name1", "[path2]"));
    assertNotEquals(new TitleSetXpath(daemon, "Name1", "[path1]"),
		    new TitleSetActiveAus(daemon));
    assertNotEquals(new TitleSetXpath(daemon, "Name1", "[path1]"),
		    "foo");
  }

  public void testSimplePat() {
    TitleSetXpath ts = newSet("[journalTitle='Dog Journal']");
    assertEquals(SetUtil.set(tc5, tc6),
		 SetUtil.theSet(ts.filterTitles(titles)));
    assertEquals("Name1", ts.getName());
  }

  public void testComplexPat() {
    TitleSetXpath ts = newSet("[attributes/key1='val1']");
    assertEquals(SetUtil.set(tc5),
		 SetUtil.theSet(ts.filterTitles(titles)));
    assertEquals("Name1", ts.getName());
    ts = newSet("[attributes/k0='1']");
    assertEquals(SetUtil.set(tc5, tc6),
		 SetUtil.theSet(ts.filterTitles(titles)));
    ts = newSet("[journalTitle='Dog Journal' and attributes/key1='val1']");
    assertEquals(SetUtil.set(tc5),
		 SetUtil.theSet(ts.filterTitles(titles)));
  }

  // Ensure that TitleConfig bean accessor names are as we expect.  Because
  // JXPath accesses them at runtime using reflection, renaming an accessor
  // wouldn't otherwise break the build.

  public void testAllFields() {
    assertEquals(SetUtil.set(tc5, tc6),
		 SetUtil.theSet(newSet("[journalTitle='Dog Journal']").filterTitles(titles)));
    assertEquals(SetUtil.set(tc1),
		 SetUtil.theSet(newSet("[displayName='Journal of Title, 2001']").filterTitles(titles)));
    assertEquals(SetUtil.set(tc1, tc2, tc3),
		 SetUtil.theSet(newSet("[pluginName='o.l.plug1']").filterTitles(titles)));
  }

  public void testFunc() {
    TitleSetXpath ts = newSet("[starts-with(journalTitle, \"Dog\")]");
    assertEquals(SetUtil.set(tc5, tc6),
		 SetUtil.theSet(ts.filterTitles(titles)));
  }

  public void testBool() {
    TitleSetXpath ts;
    ts = newSet("[journalTitle=\"Dog Journal\" or pluginName=\"o.l.plug2\"]");
    assertEquals(SetUtil.set(tc4, tc5, tc6),
		 SetUtil.theSet(ts.filterTitles(titles)));
    ts = newSet("[journalTitle='Journal of Title' and pluginName='o.l.plug2']");
    assertEquals(SetUtil.set(tc4),
		 SetUtil.theSet(ts.filterTitles(titles)));
    ts = newSet("[journalTitle='Journal of Title' and (pluginName='o.l.plug2' or RE:isMatchRe(displayName, '2002'))]");
    assertEquals(SetUtil.set(tc2, tc4),
		 SetUtil.theSet(ts.filterTitles(titles)));
  }

  public void testRe() {
    TitleSetXpath ts = newSet("[RE:isMatchRe(displayName, \"D.g[^s]\")]");
    assertEquals(SetUtil.set(tc6),
		 SetUtil.theSet(ts.filterTitles(titles)));
  }

  // forgetting to quote regexp shouldn't cause accidental matches
  public void testNullRe() {
    TitleSetXpath ts = newSet("[RE:isMatchRe(displayName, Dog)]");
    assertEmpty(SetUtil.theSet(ts.filterTitles(titles)));
    ts = newSet("[RE:isMatchRe(displayName, '')]");
    assertEmpty(SetUtil.theSet(ts.filterTitles(titles)));
  }

  // Illegal RE causes failure when TitleSet is used, not when created
  public void testIllRe() {
    TitleSetXpath ts = newSet("[RE:isMatchRe(displayName, 'a[ab')]");
    try {
      assertEmpty(SetUtil.theSet(ts.filterTitles(titles)));
      fail("Illegal regexp, should have thrown");
    } catch (JXPathException e) {
    }
  }

}
