/*
 * $Id: TestActivityRegulator.java,v 1.12 2003-05-30 01:46:55 aalto Exp $
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
import org.lockss.util.TimeBase;

public class TestActivityRegulator extends LockssTestCase {
  private ActivityRegulator regulator;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();

    mau = new MockArchivalUnit();
    regulator = new ActivityRegulator(mau);
    TimeBase.setSimulated(123);
  }

  public void tearDown() throws Exception {
    TimeBase.setReal();
    super.tearDown();
  }

  public void testGetAuActivity() {
    mau.setAuId("testid");
    assertEquals(regulator.NO_ACTIVITY, regulator.getAuActivity());

    regulator.setAuActivity(regulator.NEW_CONTENT_CRAWL, 123);
    assertEquals(regulator.NEW_CONTENT_CRAWL, regulator.getAuActivity());
  }

  public void testGetCusActivity() {
    MockCachedUrlSet mcus = new MockCachedUrlSet("test url");
    mcus.setArchivalUnit(mau);
    assertEquals(regulator.NO_ACTIVITY, regulator.getCusActivity(mcus));

    regulator.setCusActivity(mcus, regulator.REPAIR_CRAWL, 123);
    assertEquals(regulator.REPAIR_CRAWL, regulator.getCusActivity(mcus));

    MockCachedUrlSet mcus2 = new MockCachedUrlSet("test url2");
    mcus2.setArchivalUnit(mau);
    assertEquals(regulator.NO_ACTIVITY, regulator.getCusActivity(mcus2));

    regulator.setCusActivity(mcus2, regulator.BACKGROUND_CRAWL, 123);
    assertEquals(regulator.BACKGROUND_CRAWL, regulator.getCusActivity(mcus2));
    assertEquals(regulator.REPAIR_CRAWL, regulator.getCusActivity(mcus));
  }

  public void testAuActivityAllowed() {
    assertNotNull(regulator.startAuActivity(regulator.NEW_CONTENT_CRAWL, 123));
    assertEquals(regulator.NEW_CONTENT_CRAWL, regulator.getAuActivity());
    assertNull(regulator.startAuActivity(regulator.TOP_LEVEL_POLL, 123));
  }

  public void testCusActivityAllowed() {
    MockCachedUrlSet mcus = new MockCachedUrlSet("test url");
    mcus.setArchivalUnit(mau);
    assertNotNull(regulator.startCusActivity(regulator.REPAIR_CRAWL, mcus, 123));
    assertEquals(regulator.REPAIR_CRAWL, regulator.getCusActivity(mcus));
    assertNull(regulator.startCusActivity(regulator.BACKGROUND_CRAWL, mcus, 123));
    regulator.cusActivityFinished(regulator.REPAIR_CRAWL, mcus);

    mcus = new MockCachedUrlSet("test url2");
    mcus.setArchivalUnit(mau);
    regulator.startAuActivity(regulator.NEW_CONTENT_CRAWL, 123);
    assertNull(regulator.startCusActivity(regulator.REPAIR_CRAWL, mcus, 123));

    regulator.auActivityFinished(regulator.NEW_CONTENT_CRAWL);
    assertNotNull(regulator.startCusActivity(regulator.REPAIR_CRAWL, mcus, 123));
  }

  public void testAuFinished() {
    assertNotNull(regulator.startAuActivity(regulator.NEW_CONTENT_CRAWL, 123));
    assertEquals(regulator.NEW_CONTENT_CRAWL, regulator.getAuActivity());
    regulator.auActivityFinished(regulator.NEW_CONTENT_CRAWL);
    assertEquals(regulator.NO_ACTIVITY, regulator.getAuActivity());

    // calling 'finished' on the wrong activity shouldn't end the current one
    assertNotNull(regulator.startAuActivity(regulator.NEW_CONTENT_CRAWL, 123));
    assertEquals(regulator.NEW_CONTENT_CRAWL, regulator.getAuActivity());
    regulator.auActivityFinished(regulator.TOP_LEVEL_POLL);
    assertEquals(regulator.NEW_CONTENT_CRAWL, regulator.getAuActivity());
  }

  public void testCusFinished() {
    MockCachedUrlSet mcus = new MockCachedUrlSet("test url");
    mcus.setArchivalUnit(mau);
    assertNotNull(regulator.startCusActivity(regulator.REPAIR_CRAWL, mcus, 123));
    assertEquals(regulator.REPAIR_CRAWL, regulator.getCusActivity(mcus));
    regulator.cusActivityFinished(regulator.REPAIR_CRAWL, mcus);
    assertEquals(regulator.NO_ACTIVITY, regulator.getCusActivity(mcus));

    // calling 'finished' on the wrong activity shouldn't end the current one
    assertNotNull(regulator.startCusActivity(regulator.REPAIR_CRAWL, mcus, 123));
    assertEquals(regulator.REPAIR_CRAWL, regulator.getCusActivity(mcus));
    regulator.cusActivityFinished(regulator.BACKGROUND_CRAWL, mcus);
    assertEquals(regulator.REPAIR_CRAWL, regulator.getCusActivity(mcus));
  }

  public void testCusBlocking() {
    MockCachedUrlSet mcus = new MockCachedUrlSet("test url");
    mcus.setArchivalUnit(mau);
    assertEquals(regulator.NO_ACTIVITY, regulator.getAuActivity());
    assertNotNull(regulator.startCusActivity(regulator.REPAIR_CRAWL, mcus, 123));
    assertEquals(regulator.CUS_ACTIVITY, regulator.getAuActivity());
    assertNull(regulator.startAuActivity(regulator.TOP_LEVEL_POLL, 123));

    MockCachedUrlSet mcus2 = new MockCachedUrlSet("test url2");
    mcus2.setArchivalUnit(mau);
    assertNotNull(regulator.startCusActivity(regulator.BACKGROUND_CRAWL, mcus2, 123));

    regulator.cusActivityFinished(regulator.REPAIR_CRAWL, mcus);
    assertNull(regulator.startAuActivity(regulator.TOP_LEVEL_POLL, 123));
    assertEquals(regulator.CUS_ACTIVITY, regulator.getAuActivity());

    regulator.cusActivityFinished(regulator.BACKGROUND_CRAWL, mcus2);
    assertEquals(regulator.NO_ACTIVITY, regulator.getAuActivity());
    assertNotNull(regulator.startAuActivity(regulator.TOP_LEVEL_POLL, 123));
  }

  public void testCusRelationBlocking() {
    MockCachedUrlSet mcus = new MockCachedUrlSet("http://www.example.com/test");
    mcus.setArchivalUnit(mau);
    assertNotNull(regulator.startCusActivity(regulator.STANDARD_CONTENT_POLL, mcus, 123));

    // child should be blocked
    MockCachedUrlSet mcus2 = new MockCachedUrlSet("http://www.example.com/test/branch1");
    mcus2.setArchivalUnit(mau);
    assertNull(regulator.startCusActivity(regulator.BACKGROUND_CRAWL, mcus2, 123));

    // parent should be blocked on polls, but not crawls
    mcus2 = new MockCachedUrlSet("http://www.example.com");
    mcus2.setArchivalUnit(mau);
    assertNull(regulator.startCusActivity(regulator.STANDARD_CONTENT_POLL, mcus2, 123));
    assertNotNull(regulator.startCusActivity(regulator.REPAIR_CRAWL, mcus2, 123));

    // peer should be ok
    mcus2 = new MockCachedUrlSet("http://www.example.com/test2");
    mcus2.setArchivalUnit(mau);
    assertNotNull(regulator.startCusActivity(regulator.BACKGROUND_CRAWL, mcus2, 123));
  }

  public void testCusRangeAllowance() {
    MockCachedUrlSet mcus = new MockCachedUrlSet(
        new RangeCachedUrlSetSpec("http://www.example.com/test", "file1", "file3"));
    mcus.setArchivalUnit(mau);
    assertNotNull(regulator.startCusActivity(regulator.STANDARD_CONTENT_POLL, mcus, 123));

    // different range should be allowed
    MockCachedUrlSet mcus2 = new MockCachedUrlSet(
        new RangeCachedUrlSetSpec("http://www.example.com/test", "file4", "file6"));
    mcus2.setArchivalUnit(mau);
    assertNotNull(regulator.startCusActivity(regulator.STANDARD_CONTENT_POLL, mcus2, 123));
  }

  public void testCusSimultaneousPollAllowance() {
    MockCachedUrlSet mcus = new MockCachedUrlSet("http://www.example.com/test");
    mcus.setArchivalUnit(mau);
    assertNotNull(regulator.startCusActivity(regulator.STANDARD_CONTENT_POLL, mcus, 123));

    // name poll should be allowed
    mcus = new MockCachedUrlSet("http://www.example.com/test");
    mcus.setArchivalUnit(mau);
    assertNotNull(regulator.startCusActivity(regulator.STANDARD_NAME_POLL, mcus, 123));

    // content poll on child should be allowed
    mcus = new MockCachedUrlSet("http://www.example.com/test/branch");
    mcus.setArchivalUnit(mau);
    assertNotNull(regulator.startCusActivity(regulator.STANDARD_CONTENT_POLL, mcus, 123));

  }

  public void testGetRelation() {
    assertEquals(ActivityRegulator.RELATION_SAME,
                 regulator.getRelation("http://www.example.com::range",
                                     "http://www.example.com::range2"));
    assertEquals(ActivityRegulator.RELATION_CHILD,
                 regulator.getRelation("http://www.example.com/test::range",
                                     "http://www.example.com::range2"));
    assertEquals(ActivityRegulator.RELATION_PARENT,
                 regulator.getRelation("http://www.example.com::range",
                                     "http://www.example.com/test::range2"));
    assertEquals(ActivityRegulator.RELATION_NONE,
                 regulator.getRelation("http://www.example.com/test::range",
                                     "http://www.example.com/test2::range2"));
  }

  public void testIsAllowedOnAu() {
    // using NO_ACTIVITY as a generic activity marker, since this functions
    // don't specifically handle it

    // nothing allowed on these
    assertFalse(regulator.isAllowedOnAu(regulator.NO_ACTIVITY, regulator.NEW_CONTENT_CRAWL));
    assertFalse(regulator.isAllowedOnAu(regulator.NO_ACTIVITY, regulator.TREEWALK));

    // only other polls are allowed, but not standard name polls
    assertFalse(regulator.isAllowedOnAu(regulator.NO_ACTIVITY, regulator.TOP_LEVEL_POLL));
    assertFalse(regulator.isAllowedOnAu(regulator.STANDARD_NAME_POLL, regulator.TOP_LEVEL_POLL));
    assertTrue(regulator.isAllowedOnAu(regulator.TOP_LEVEL_POLL, regulator.TOP_LEVEL_POLL));
    assertTrue(regulator.isAllowedOnAu(regulator.STANDARD_CONTENT_POLL, regulator.TOP_LEVEL_POLL));


    // only CUS activity allowed on CUS_ACTIVITY
    assertTrue(regulator.isAllowedOnAu(regulator.BACKGROUND_CRAWL, regulator.CUS_ACTIVITY));
    assertTrue(regulator.isAllowedOnAu(regulator.REPAIR_CRAWL, regulator.CUS_ACTIVITY));
    assertTrue(regulator.isAllowedOnAu(regulator.STANDARD_CONTENT_POLL, regulator.CUS_ACTIVITY));
    assertFalse(regulator.isAllowedOnAu(regulator.NEW_CONTENT_CRAWL, regulator.CUS_ACTIVITY));
  }

  public void testIsAllowedOnCus() {
    // using NO_ACTIVITY as a generic activity marker, since this functions
    // don't specifically handle it

    // if a crawl-
    //   allow only name poll if same
    assertFalse(regulator.isAllowedOnCus(regulator.NO_ACTIVITY, regulator.BACKGROUND_CRAWL, regulator.RELATION_SAME));
    assertTrue(regulator.isAllowedOnCus(regulator.STANDARD_NAME_POLL, regulator.REPAIR_CRAWL, regulator.RELATION_SAME));
    //   allow anything if parent
    assertTrue(regulator.isAllowedOnCus(regulator.NO_ACTIVITY, regulator.BACKGROUND_CRAWL, regulator.RELATION_PARENT));
    //   allow only crawls if child
    assertFalse(regulator.isAllowedOnCus(regulator.NO_ACTIVITY, regulator.BACKGROUND_CRAWL, regulator.RELATION_CHILD));
    assertTrue(regulator.isAllowedOnCus(regulator.REPAIR_CRAWL, regulator.BACKGROUND_CRAWL, regulator.RELATION_CHILD));

    // if a poll-
    //   allow only name poll or repair crawl if same
    assertFalse(regulator.isAllowedOnCus(regulator.NO_ACTIVITY, regulator.STANDARD_CONTENT_POLL, regulator.RELATION_SAME));
    assertTrue(regulator.isAllowedOnCus(regulator.STANDARD_NAME_POLL, regulator.STANDARD_CONTENT_POLL, regulator.RELATION_SAME));
    assertTrue(regulator.isAllowedOnCus(regulator.REPAIR_CRAWL, regulator.STANDARD_CONTENT_POLL, regulator.RELATION_SAME));
    //   allow only content polls and repairs on sub-nodes if parent with name poll
    assertFalse(regulator.isAllowedOnCus(regulator.NO_ACTIVITY, regulator.STANDARD_CONTENT_POLL, regulator.RELATION_PARENT));
    assertTrue(regulator.isAllowedOnCus(regulator.STANDARD_CONTENT_POLL, regulator.STANDARD_NAME_POLL, regulator.RELATION_PARENT));
    assertTrue(regulator.isAllowedOnCus(regulator.REPAIR_CRAWL, regulator.STANDARD_NAME_POLL, regulator.RELATION_PARENT));
    //   allow only crawls and single node polls if child
    assertFalse(regulator.isAllowedOnCus(regulator.NO_ACTIVITY, regulator.STANDARD_CONTENT_POLL, regulator.RELATION_CHILD));
    assertTrue(regulator.isAllowedOnCus(regulator.REPAIR_CRAWL, regulator.STANDARD_CONTENT_POLL, regulator.RELATION_CHILD));
    assertTrue(regulator.isAllowedOnCus(regulator.SINGLE_NODE_CONTENT_POLL, regulator.STANDARD_CONTENT_POLL, regulator.RELATION_CHILD));
    //   for single node polls, allow only repair crawl if same
    assertFalse(regulator.isAllowedOnCus(regulator.STANDARD_NAME_POLL, regulator.SINGLE_NODE_CONTENT_POLL, regulator.RELATION_SAME));
    assertTrue(regulator.isAllowedOnCus(regulator.REPAIR_CRAWL, regulator.SINGLE_NODE_CONTENT_POLL, regulator.RELATION_SAME));
    //   allow anything if parent
    assertTrue(regulator.isAllowedOnCus(regulator.NO_ACTIVITY, regulator.SINGLE_NODE_CONTENT_POLL, regulator.RELATION_PARENT));
    //   allow only crawls if child
    assertFalse(regulator.isAllowedOnCus(regulator.STANDARD_NAME_POLL, regulator.SINGLE_NODE_CONTENT_POLL, regulator.RELATION_CHILD));
    assertTrue(regulator.isAllowedOnCus(regulator.REPAIR_CRAWL, regulator.SINGLE_NODE_CONTENT_POLL, regulator.RELATION_CHILD));
  }

  public void testAuExpiration() {
    assertNotNull(regulator.startAuActivity(regulator.NEW_CONTENT_CRAWL, 10));
    assertEquals(regulator.NEW_CONTENT_CRAWL, regulator.getAuActivity());
    TimeBase.step(5);
    assertEquals(regulator.NEW_CONTENT_CRAWL, regulator.getAuActivity());
    TimeBase.step(5);
    assertEquals(regulator.NO_ACTIVITY, regulator.getAuActivity());
  }

  public void testCusExpiration() {
    MockCachedUrlSet mcus = new MockCachedUrlSet("test url");
    mcus.setArchivalUnit(mau);
    assertNotNull(regulator.startCusActivity(regulator.REPAIR_CRAWL, mcus, 10));
    assertEquals(regulator.REPAIR_CRAWL, regulator.getCusActivity(mcus));
    assertEquals(regulator.CUS_ACTIVITY, regulator.getAuActivity());
    TimeBase.step(5);
    assertEquals(regulator.REPAIR_CRAWL, regulator.getCusActivity(mcus));
    assertEquals(regulator.CUS_ACTIVITY, regulator.getAuActivity());
    TimeBase.step(5);
    assertEquals(regulator.NO_ACTIVITY, regulator.getCusActivity(mcus));
    assertEquals(regulator.NO_ACTIVITY, regulator.getAuActivity());
  }

  public void testGetCusKeys() {
    MockCachedUrlSet mcus = new MockCachedUrlSet("test url");

    String expectedStr = "test url::";
    assertEquals(expectedStr, regulator.getCusKey(mcus));

    mcus = new MockCachedUrlSet(
        new RangeCachedUrlSetSpec("test", "lwr", "upr"));

    expectedStr = "test::lwr-upr";
    assertEquals(expectedStr, regulator.getCusKey(mcus));
  }

}
