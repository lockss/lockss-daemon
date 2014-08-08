/*
 * $Id: TestGPOFDSysCrawlWindow.java,v 1.1 2014-08-08 18:33:47 thib_gc Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.usdocspln.gov.gpo.fdsys;

import java.text.*;
import java.util.*;

import org.lockss.daemon.CrawlWindow;
import org.lockss.test.LockssTestCase;

public class TestGPOFDSysCrawlWindow extends LockssTestCase {

  protected CrawlWindow window;
  
  public void setUp() throws Exception {
    TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    window = new GPOFDSysCrawlWindow().makeCrawlWindow();
  }
  
  public void testCrawlWindowNY() throws Exception {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    sdf.setTimeZone(TimeZone.getTimeZone("US/Eastern"));
    
    // August 4, 2014 was a Monday
    assertTrue(window.canCrawl(sdf.parse("2014-08-04 08:59:59")));
    assertFalse(window.canCrawl(sdf.parse("2014-08-04 09:00:00")));
    assertFalse(window.canCrawl(sdf.parse("2014-08-04 20:59:59")));
    assertTrue(window.canCrawl(sdf.parse("2014-08-04 21:00:00")));
    // August 8, 2014 was a Friday
    assertTrue(window.canCrawl(sdf.parse("2014-08-08 08:59:59")));
    assertFalse(window.canCrawl(sdf.parse("2014-08-08 09:00:00")));
    assertFalse(window.canCrawl(sdf.parse("2014-08-08 20:59:59")));
    assertTrue(window.canCrawl(sdf.parse("2014-08-08 21:00:00")));
    // August 9, 2014 was a Saturday
    assertTrue(window.canCrawl(sdf.parse("2014-08-09 01:59:59")));
    assertTrue(window.canCrawl(sdf.parse("2014-08-09 02:00:00")));
    assertTrue(window.canCrawl(sdf.parse("2014-08-09 08:59:59")));
    assertTrue(window.canCrawl(sdf.parse("2014-08-09 09:00:00")));
    assertTrue(window.canCrawl(sdf.parse("2014-08-09 09:59:59")));
    assertTrue(window.canCrawl(sdf.parse("2014-08-09 10:00:00")));
    assertTrue(window.canCrawl(sdf.parse("2014-08-09 20:59:59")));
    assertTrue(window.canCrawl(sdf.parse("2014-08-09 21:00:00")));
    // August 10, 2014 was a Sunday
    assertTrue(window.canCrawl(sdf.parse("2014-08-10 01:59:59")));
    assertFalse(window.canCrawl(sdf.parse("2014-08-10 02:00:00")));
    assertFalse(window.canCrawl(sdf.parse("2014-08-10 09:59:59")));
    assertTrue(window.canCrawl(sdf.parse("2014-08-10 10:00:00")));
  }
  
  public void testCrawlWindowCA() throws Exception {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    sdf.setTimeZone(TimeZone.getTimeZone("US/Pacific"));
    
    // August 4, 2014 was a Monday
    assertTrue(window.canCrawl(sdf.parse("2014-08-04 05:59:59")));
    assertFalse(window.canCrawl(sdf.parse("2014-08-04 06:00:00")));
    assertFalse(window.canCrawl(sdf.parse("2014-08-04 17:59:59")));
    assertTrue(window.canCrawl(sdf.parse("2014-08-04 18:00:00")));
    // August 8, 2014 was a Friday
    assertTrue(window.canCrawl(sdf.parse("2014-08-08 05:59:59")));
    assertFalse(window.canCrawl(sdf.parse("2014-08-08 06:00:00")));
    assertFalse(window.canCrawl(sdf.parse("2014-08-08 17:59:59")));
    assertTrue(window.canCrawl(sdf.parse("2014-08-08 18:00:00")));
    // August 9, 2014 was a Saturday
    assertTrue(window.canCrawl(sdf.parse("2014-08-08 22:59:59")));
    assertTrue(window.canCrawl(sdf.parse("2014-08-08 23:00:00")));
    assertTrue(window.canCrawl(sdf.parse("2014-08-09 05:59:59")));
    assertTrue(window.canCrawl(sdf.parse("2014-08-09 06:00:00")));
    assertTrue(window.canCrawl(sdf.parse("2014-08-09 06:59:59")));
    assertTrue(window.canCrawl(sdf.parse("2014-08-09 07:00:00")));
    assertTrue(window.canCrawl(sdf.parse("2014-08-09 17:59:59")));
    assertTrue(window.canCrawl(sdf.parse("2014-08-09 18:00:00")));
    // August 10, 2014 was a Sunday
    assertTrue(window.canCrawl(sdf.parse("2014-08-09 22:59:59")));
    assertFalse(window.canCrawl(sdf.parse("2014-08-09 23:00:00")));
    assertFalse(window.canCrawl(sdf.parse("2014-08-10 06:59:59")));
    assertTrue(window.canCrawl(sdf.parse("2014-08-10 07:00:00")));
  }
  
}
