/*
 * $Id: TestActivityRegulator.java,v 1.1 2003-04-12 01:14:29 aalto Exp $
 */

/*

Copyright (c) 2001-2002 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.plugin.*;
import org.lockss.test.*;

public class TestActivityRegulator extends LockssTestCase {
  private ActivityRegulator allower;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();

    allower = new ActivityRegulator();
    mau = new MockArchivalUnit();
  }

  public void testGetAuActivity() {
    mau.setAuId("testid");
    assertEquals(allower.NO_ACTIVITY, allower.getAuActivity(mau));

    allower.setAuActivity(mau, allower.NEW_CONTENT_CRAWL);
    assertEquals(allower.NEW_CONTENT_CRAWL, allower.getAuActivity(mau));

    MockArchivalUnit mau2 = new MockArchivalUnit();
    mau2.setAuId("testid2");
    assertEquals(allower.NO_ACTIVITY, allower.getAuActivity(mau2));

    allower.setAuActivity(mau2, allower.TOP_LEVEL_POLL);
    assertEquals(allower.TOP_LEVEL_POLL, allower.getAuActivity(mau2));
    assertEquals(allower.NEW_CONTENT_CRAWL, allower.getAuActivity(mau));
  }

  public void testGetCusActivity() {
    MockCachedUrlSet mcus = new MockCachedUrlSet("test url");
    mcus.setArchivalUnit(mau);
    assertEquals(allower.NO_ACTIVITY, allower.getCusActivity(mcus));

    allower.setCusActivity(mcus, allower.REPAIR_CRAWL);
    assertEquals(allower.REPAIR_CRAWL, allower.getCusActivity(mcus));

    MockCachedUrlSet mcus2 = new MockCachedUrlSet("test url2");
    mcus2.setArchivalUnit(mau);
    assertEquals(allower.NO_ACTIVITY, allower.getCusActivity(mcus2));

    allower.setCusActivity(mcus2, allower.BACKGROUND_CRAWL);
    assertEquals(allower.BACKGROUND_CRAWL, allower.getCusActivity(mcus2));
    assertEquals(allower.REPAIR_CRAWL, allower.getCusActivity(mcus));
  }

  public void testAuActivityAllowed() {
    assertTrue(allower.startAuActivity(allower.NEW_CONTENT_CRAWL, mau));
    assertEquals(allower.NEW_CONTENT_CRAWL, allower.getAuActivity(mau));
    assertFalse(allower.startAuActivity(allower.TOP_LEVEL_POLL, mau));
  }

  public void testCusActivityAllowed() {
    MockCachedUrlSet mcus = new MockCachedUrlSet("test url");
    mcus.setArchivalUnit(mau);
    assertTrue(allower.startCusActivity(allower.REPAIR_CRAWL, mcus));
    assertEquals(allower.REPAIR_CRAWL, allower.getCusActivity(mcus));
    assertFalse(allower.startCusActivity(allower.BACKGROUND_CRAWL, mcus));
    allower.cusActivityFinished(allower.REPAIR_CRAWL, mcus);

    mcus = new MockCachedUrlSet("test url2");
    mcus.setArchivalUnit(mau);
    allower.startAuActivity(allower.NEW_CONTENT_CRAWL, mau);
    assertFalse(allower.startCusActivity(allower.REPAIR_CRAWL, mcus));

    allower.auActivityFinished(allower.NEW_CONTENT_CRAWL, mau);
    assertTrue(allower.startCusActivity(allower.REPAIR_CRAWL, mcus));
  }

  public void testAuActivityFinished() {
    assertTrue(allower.startAuActivity(allower.NEW_CONTENT_CRAWL, mau));
    assertEquals(allower.NEW_CONTENT_CRAWL, allower.getAuActivity(mau));
    allower.auActivityFinished(allower.NEW_CONTENT_CRAWL, mau);
    assertEquals(allower.NO_ACTIVITY, allower.getAuActivity(mau));
  }

  public void testCusActivityFinished() {
    MockCachedUrlSet mcus = new MockCachedUrlSet("test url");
    mcus.setArchivalUnit(mau);
    assertTrue(allower.startCusActivity(allower.REPAIR_CRAWL, mcus));
    assertEquals(allower.REPAIR_CRAWL, allower.getCusActivity(mcus));
    allower.cusActivityFinished(allower.REPAIR_CRAWL, mcus);
    assertEquals(allower.NO_ACTIVITY, allower.getCusActivity(mcus));
  }

  public void testCusActivityBlocking() {
    MockCachedUrlSet mcus = new MockCachedUrlSet("test url");
    mcus.setArchivalUnit(mau);
    assertEquals(allower.NO_ACTIVITY, allower.getAuActivity(mau));
    assertTrue(allower.startCusActivity(allower.REPAIR_CRAWL, mcus));
    assertEquals(allower.CUS_ACTIVITY, allower.getAuActivity(mau));
    assertFalse(allower.startAuActivity(allower.TOP_LEVEL_POLL, mau));

    MockCachedUrlSet mcus2 = new MockCachedUrlSet("test url2");
    mcus2.setArchivalUnit(mau);
    assertTrue(allower.startCusActivity(allower.BACKGROUND_CRAWL, mcus2));

    allower.cusActivityFinished(allower.REPAIR_CRAWL, mcus);
    assertFalse(allower.startAuActivity(allower.TOP_LEVEL_POLL, mau));
    assertEquals(allower.CUS_ACTIVITY, allower.getAuActivity(mau));

    allower.cusActivityFinished(allower.BACKGROUND_CRAWL, mcus2);
    assertEquals(allower.NO_ACTIVITY, allower.getAuActivity(mau));
    assertTrue(allower.startAuActivity(allower.TOP_LEVEL_POLL, mau));
  }

  public void testGetKeys() {
    MockCachedUrlSet mcus = new MockCachedUrlSet("test url");
    mcus.setArchivalUnit(mau);

    String expectedStr = mau.getGloballyUniqueId();
    assertEquals(expectedStr, allower.getAuKey(mau));

    expectedStr += "::";
    assertEquals(expectedStr, allower.getAuPrefix(mcus));

    expectedStr += mcus.getUrl();
    assertEquals(expectedStr, allower.getCusKey(mcus));
  }

}
