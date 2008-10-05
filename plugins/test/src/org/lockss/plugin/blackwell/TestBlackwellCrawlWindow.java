/*
 * $Id: TestBlackwellCrawlWindow.java,v 1.1 2006-08-01 05:21:51 tlipkis Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.blackwell;

import java.io.*;
import java.text.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.test.LockssTestCase;

public class TestBlackwellCrawlWindow extends LockssTestCase {

  BlackwellCrawlWindow fact = new BlackwellCrawlWindow();
  CrawlWindow window;
  SimpleDateFormat sdf;

  String MON = "2006-07-24 ";
  String FRI = "2006-07-28 ";
  String SAT = "2006-07-29 ";
  String SUN = "2006-07-30 ";

  public void setUp() throws Exception {
    TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    fact = new BlackwellCrawlWindow();
    window = fact.makeCrawlWindow();
    sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
    super.setUp();
  }

  void assertOpen(String time) throws ParseException {
    assertTrue("Should be open at " + time + " (" + sdf.parse(time) + ")",
	       window.canCrawl(sdf.parse(time)));
  }

  void assertClosed(String time) throws ParseException {
    assertFalse("Should be closed at " + time + " (" + sdf.parse(time) + ")",
		window.canCrawl(sdf.parse(time)));
  }

  public void testPacific() throws ParseException {
    sdf.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));

    assertOpen(MON + "23:59:00.0");
    assertOpen(FRI + "23:59:00.0");
    assertOpen(SAT + "23:59:00.0");
    assertOpen(SUN + "23:59:00.0");
    assertClosed(MON + "00:00:00.0");
    assertClosed(FRI + "00:00:00.0");
    assertOpen(SAT + "00:00:00.0");
    assertOpen(SUN + "00:00:00.0");
    assertClosed(MON + "00:01:00.0");
    assertClosed(FRI + "00:01:00.0");
    assertOpen(SAT + "00:01:00.0");
    assertOpen(SUN + "00:01:00.0");

    assertClosed(MON + "11:59:00.0");
    assertClosed(FRI + "11:59:00.0");
    assertOpen(SAT + "11:59:00.0");
    assertOpen(SUN + "11:59:00.0");
    assertOpen(MON + "12:00:00.0");
    assertOpen(FRI + "12:00:00.0");
    assertOpen(SAT + "12:00:00.0");
    assertOpen(SUN + "12:00:00.0");
    assertOpen(MON + "12:01:00.0");
    assertOpen(FRI + "12:01:00.0");
  }

  public void testGMT() throws ParseException {
    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

    assertOpen(MON + "06:59:00.0");
    assertOpen(FRI + "06:59:00.0");
    assertOpen(SAT + "06:59:00.0");
    assertOpen(SUN + "06:59:00.0");
    assertClosed(MON + "07:00:00.0");
    assertClosed(FRI + "07:00:00.0");
    assertOpen(SAT + "07:00:00.0");
    assertOpen(SUN + "07:00:00.0");
    assertClosed(MON + "07:01:00.0");
    assertClosed(FRI + "07:01:00.0");
    assertOpen(SAT + "07:01:00.0");
    assertOpen(SUN + "07:01:00.0");

    assertClosed(MON + "18:59:00.0");
    assertClosed(FRI + "18:59:00.0");
    assertOpen(SAT + "18:59:00.0");
    assertOpen(SUN + "18:59:00.0");
    assertOpen(MON + "19:00:00.0");
    assertOpen(FRI + "19:00:00.0");
    assertOpen(SAT + "19:00:00.0");
    assertOpen(SUN + "19:00:00.0");
    assertOpen(MON + "19:01:00.0");
    assertOpen(FRI + "19:01:00.0");
  }
}
