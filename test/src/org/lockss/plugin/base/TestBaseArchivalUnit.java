/*
 * $Id: TestBaseArchivalUnit.java,v 1.13 2004-01-13 04:46:27 clairegriffin Exp $
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

package org.lockss.plugin.base;

import java.io.*;
import java.util.*;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.util.TimeBase;
import org.lockss.daemon.Configuration;

/**
 * This is the test class for org.lockss.plugin.simulated.GenericFileUrlCacher
 *
 * @author  Emil Aalto
 * @version 0.0
 */
public class TestBaseArchivalUnit extends LockssTestCase {
  MockBaseArchivalUnit mbau;

  public void setUp() throws Exception {
    super.setUp();
    Properties props = new Properties();
    props.setProperty(BaseArchivalUnit.PARAM_TOPLEVEL_POLL_INTERVAL_MIN, "5s");
    props.setProperty(BaseArchivalUnit.PARAM_TOPLEVEL_POLL_INTERVAL_MAX, "10s");
    props.setProperty(BaseArchivalUnit.PARAM_TOPLEVEL_POLL_PROB_INITIAL, "50");
    props.setProperty(BaseArchivalUnit.PARAM_TOPLEVEL_POLL_PROB_INCREMENT, "5");
    props.setProperty(BaseArchivalUnit.PARAM_TOPLEVEL_POLL_PROB_MAX, "85");
    ConfigurationUtil.setCurrentConfigFromProps(props);

    mbau = new MockBaseArchivalUnit(null);
  }

  public void tearDown() throws Exception {
    TimeBase.setReal();
    super.tearDown();
  }

  public void testCheckNextPollInterval() {
    TimeBase.setSimulated();
    for (int ii=0; ii<10; ii++) {
      mbau.nextPollInterval = -1;
      mbau.checkNextPollInterval();
      assertTrue(mbau.nextPollInterval >= 5000);
      assertTrue(mbau.nextPollInterval <= 10000);
    }

    Properties props = new Properties();
    props.setProperty(BaseArchivalUnit.PARAM_TOPLEVEL_POLL_INTERVAL_MIN, "1s");
    props.setProperty(BaseArchivalUnit.PARAM_TOPLEVEL_POLL_INTERVAL_MAX, "2s");
    ConfigurationUtil.setCurrentConfigFromProps(props);

    mbau.checkNextPollInterval();
    assertTrue(mbau.nextPollInterval >= 1000);
    assertTrue(mbau.nextPollInterval <= 2000);
  }

  public void testIncrementPollProb() {
    assertEquals(0.15, mbau.incrementPollProb(0.10), 0.001);
    assertEquals(0.50, mbau.incrementPollProb(0.45), 0.001);
    // shouldn't increment past max
    assertEquals(0.85, mbau.incrementPollProb(0.83), 0.001);
    assertEquals(0.85, mbau.incrementPollProb(0.85), 0.001);
  }

  public void testCheckPollProb() {
    mbau.curTopLevelPollProb = -1.0;
    mbau.checkPollProb();
    assertEquals(0.50, mbau.curTopLevelPollProb, 0.001);

    mbau.curTopLevelPollProb = .35;
    mbau.checkPollProb();
    assertEquals(0.50, mbau.curTopLevelPollProb, 0.001);

    mbau.curTopLevelPollProb = .90;
    mbau.checkPollProb();
    assertEquals(0.85, mbau.curTopLevelPollProb, 0.001);
  }

  public void testShouldCallTopLevelPoll() throws IOException {
    TimeBase.setSimulated(100);
    MockAuState state = new MockAuState(mbau, -1, TimeBase.nowMs(), -1, null);

    // no interval yet
    assertEquals(-1, mbau.nextPollInterval);
    assertEquals(-1.0, mbau.curTopLevelPollProb, 0);
    assertFalse(mbau.shouldCallTopLevelPoll(state));
    // should determine random interval
    assertTrue(mbau.nextPollInterval >= 5000);
    assertTrue(mbau.nextPollInterval <= 10000);
    assertEquals(0.5, mbau.curTopLevelPollProb, 0.001);

    // move to proper time
    TimeBase.step(mbau.nextPollInterval);
    boolean result = mbau.shouldCallTopLevelPoll(state);
    // should have reset interval
    assertEquals(-1, mbau.nextPollInterval);
    // may or may not have allowed poll
    if (mbau.curTopLevelPollProb != -1) {
      assertEquals(0.55, mbau.curTopLevelPollProb, 0.001);
      assertFalse(result);
    } else {
      assertEquals(-1.0, mbau.curTopLevelPollProb, 0);
      assertTrue(result);
    }

    TimeBase.setReal();
  }

  public void testCheckCrawlPermission() {
    StringBuffer sb = new StringBuffer("laa-dee-dah-LOCK-KCOL\n\n");
    sb.append(BaseArchivalUnit.PERMISSION_STRING);
    sb.append("\n\nTheEnd!");
    String s_ok = sb.toString();
    String s_rev = sb.reverse().toString();
    String s_case = s_ok.toUpperCase();

    Reader reader = new StringReader(s_ok);
    assertTrue(mbau.checkCrawlPermission(reader));

    reader = new StringReader(s_case);
    assertTrue(mbau.checkCrawlPermission(reader));

    reader = new StringReader(s_rev);
    assertFalse(mbau.checkCrawlPermission(reader));
  }

  public void testCheckCrawlPermissionWithWhitespace() {
    int firstWS = BaseArchivalUnit.PERMISSION_STRING.indexOf(' ');
    if (firstWS <=0) {
      fail("No spaces in permission string, or starts with space");
    }

    String subStr1 = BaseArchivalUnit.PERMISSION_STRING.substring(0, firstWS);
    String subStr2 = BaseArchivalUnit.PERMISSION_STRING.substring(firstWS+1);

    // standard
    StringBuffer sb = new StringBuffer("laa-dee-dah-LOCK-KCOL\n\n");
    sb.append(subStr1);
    sb.append(' ');
    sb.append(subStr2);
    sb.append("\n\nTheEnd!");
    String testStr = sb.toString();

    Reader reader = new StringReader(testStr);
    assertTrue(mbau.checkCrawlPermission(reader));

    // different whitespace
    sb = new StringBuffer("laa-dee-dah-LOCK-KCOL\n\n");
    sb.append(subStr1);
    sb.append("\n");
    sb.append(subStr2);
    sb.append("\n\nTheEnd!");
    testStr = sb.toString();

    reader = new StringReader(testStr);
    assertTrue(mbau.checkCrawlPermission(reader));

    // extra whitespace
    sb = new StringBuffer("laa-dee-dah-LOCK-KCOL\n\n");
    sb.append(subStr1);
    sb.append(" \n\r\t ");
    sb.append(subStr2);
    sb.append("\n\nTheEnd!");
    testStr = sb.toString();

    reader = new StringReader(testStr);
    assertTrue(mbau.checkCrawlPermission(reader));

    // missing whitespace
    sb = new StringBuffer("laa-dee-dah-LOCK-KCOL\n\n");
    sb.append(subStr1);
    sb.append(subStr2);
    sb.append("\n\nTheEnd!");
    testStr = sb.toString();

    reader = new StringReader(testStr);
    assertFalse(mbau.checkCrawlPermission(reader));
  }

  public void testCheckCrawlPermissionWithHtml() {
    int firstWS = BaseArchivalUnit.PERMISSION_STRING.indexOf(' ');
    if (firstWS <= 0) {
      fail("No spaces in permission string, or starts with space");
    }

    String subStr1 = BaseArchivalUnit.PERMISSION_STRING.substring(0, firstWS);
    String subStr2 = BaseArchivalUnit.PERMISSION_STRING.substring(firstWS+1);

    // single
    StringBuffer sb = new StringBuffer("laa-dee-dah-LOCK-KCOL\n\n");
    sb.append(subStr1);
    sb.append("<br>");
    sb.append(subStr2);
    sb.append("\n\nTheEnd!");
    String testStr = sb.toString();

    Reader reader = new StringReader(testStr);
    assertTrue(mbau.checkCrawlPermission(reader));

    // multiple, with mixed case
    sb = new StringBuffer("laa-dee-dah-LOCK-KCOL\n\n");
    sb.append(subStr1);
    sb.append("<BR>&nbsp;");
    sb.append(subStr2);
    sb.append("\n\nTheEnd!");
    testStr = sb.toString();

    reader = new StringReader(testStr);
    assertTrue(mbau.checkCrawlPermission(reader));
  }

  public static void main(String[] argv) {
    String[] testCaseList = { TestBaseArchivalUnit.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

  static class MockBaseArchivalUnit extends BaseArchivalUnit {

    private String auId = null;
    private String m_startUrl = "www.example.com/index.html";
    private String m_name = "MockBaseArchivalUnit";

    public MockBaseArchivalUnit(Plugin myPlugin) {
      super(myPlugin);
    }

    public void setStartUrl(String url) {
      m_startUrl = url;
    }

    public void setName(String name) {
      m_name = name;
    }

    protected String makeName() {
      return m_name;
    }

    protected CrawlRule makeRules() {
      return new MockCrawlRule();
    }

    protected String makeStartUrl() {
      return m_startUrl;
    }

    protected void setAuParams(Configuration config) throws
        ConfigurationException {
      // ok to do nothing - so do nothing.
    }

    protected void loadDefiningConfig(Configuration config) {
    }
  }
}
