/*
 * $Id: TestBaseArchivalUnit.java,v 1.19 2004-08-11 19:41:36 clairegriffin Exp $
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
import org.lockss.util.*;
import org.lockss.daemon.Configuration;
import org.lockss.crawler.*;

/**
 * This is the test class for org.lockss.plugin.simulated.GenericFileUrlCacher
 *
 * @author  Emil Aalto
 * @version 0.0
 */
public class TestBaseArchivalUnit extends LockssTestCase {
  MyMockBaseArchivalUnit mbau;

  public void setUp() throws Exception {
    super.setUp();
    Properties props = new Properties();
    props.setProperty(BaseArchivalUnit.PARAM_TOPLEVEL_POLL_INTERVAL_MIN, "5s");
    props.setProperty(BaseArchivalUnit.PARAM_TOPLEVEL_POLL_INTERVAL_MAX, "10s");
    props.setProperty(BaseArchivalUnit.PARAM_TOPLEVEL_POLL_PROB_INITIAL, "50");
    props.setProperty(BaseArchivalUnit.PARAM_TOPLEVEL_POLL_PROB_INCREMENT, "5");
    props.setProperty(BaseArchivalUnit.PARAM_TOPLEVEL_POLL_PROB_MAX, "85");
    ConfigurationUtil.setCurrentConfigFromProps(props);

    mbau = new MyMockBaseArchivalUnit(null);
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


  public void testGetContentParserReturnsNullForNullMimeTupe() {
    assertNull(mbau.getContentParser(null));
  }

  public void testGetContentParserReturnsNullForMissingMimeType() {
    assertNull(mbau.getContentParser(""));
  }

  public void testGetContentParserReturnsGoslingHtmlParser() {
    assertTrue(mbau.getContentParser("text/html")
	       instanceof org.lockss.crawler.GoslingHtmlParser);

    assertTrue(mbau.getContentParser("Text/Html")
	       instanceof org.lockss.crawler.GoslingHtmlParser);
  }

  public void testReturnsGHPWithJunkAfterContentType() {
    assertTrue(mbau.getContentParser("text/html blah")
	       instanceof org.lockss.crawler.GoslingHtmlParser);
  }

  public void testGetContentParserReturnsSameGoslingHtmlParser() {
    assertSame(mbau.getContentParser("text/html"),
	       mbau.getContentParser("text/html"));
  }

  public void testFilterRuleCaching() throws IOException {
    MockFilterRule rule1 = new MockFilterRule();
    rule1.setFilteredInputStream(new StringInputStream("rule1"));
    MockFilterRule rule2 = new MockFilterRule();
    rule2.setFilteredInputStream(new StringInputStream("rule2"));

    assertNull(mbau.rule);
    assertEquals(0, mbau.cacheMiss);
    assertNull(mbau.getFilterRule("test1"));
    assertEquals(1, mbau.cacheMiss);
    mbau.rule = rule1;
    assertNotNull(mbau.getFilterRule("test1"));
    assertEquals(2, mbau.cacheMiss);
    mbau.rule = rule2;
    assertNotNull(mbau.getFilterRule("test2"));
    assertEquals(3, mbau.cacheMiss);

    rule1 = (MockFilterRule)mbau.getFilterRule("test2");
    assertEquals(3, mbau.cacheMiss);
    assertEquals("rule2", StringUtil.fromInputStream(
        rule1.createFilteredInputStream(null)));
    rule2 = (MockFilterRule)mbau.getFilterRule("test1");
    assertEquals(3, mbau.cacheMiss);
    assertEquals("rule1", StringUtil.fromInputStream(
        rule2.createFilteredInputStream(null)));
  }

  TitleConfig makeTitleConfig() {
    ConfigParamDescr d1 = new ConfigParamDescr("key1");
    ConfigParamDescr d2 = new ConfigParamDescr("key2");
    ConfigParamAssignment a1 = new ConfigParamAssignment(d1, "a");
    ConfigParamAssignment a2 = new ConfigParamAssignment(d2, "foo");
    a1.setEditable(false);
    a2.setEditable(false);
    TitleConfig tc1 = new TitleConfig("a", "b");
    tc1.setParams(ListUtil.list(a1, a2));
    return tc1;
  }


  public void testGetTitleConfig() throws IOException {
    TitleConfig tc = makeTitleConfig();
    MyMockPlugin plug = new MyMockPlugin();
    plug.setTitleConfig(tc);
    plug.setSupportedTitles(ListUtil.list("a", "b"));
    MyMockBaseArchivalUnit au = new MyMockBaseArchivalUnit(plug);
    Configuration config = ConfigurationUtil.fromArgs("key1", "a",
						      "key2", "foo");
    assertEquals(tc, au.findTitleConfig(config));

    Configuration config2 = ConfigurationUtil.fromArgs("key1", "b",
						       "key2", "foo");
    assertNull(au.findTitleConfig(config2));
  }

  public static void main(String[] argv) {
    String[] testCaseList = { TestBaseArchivalUnit.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

  static class MyMockPlugin extends MockPlugin {
    TitleConfig tc;
    List titles;

    public TitleConfig getTitleConfig(String title) {
      return tc;
    }

    public List getSupportedTitles() {
      return titles;
    }

    void setTitleConfig(TitleConfig tc) {
      this.tc = tc;
    }
    void setSupportedTitles(List titles) {
      this.titles = titles;
    }

  }

  static class MyMockBaseArchivalUnit extends BaseArchivalUnit {
    private String auId = null;
    private String m_startUrl = "www.example.com/index.html";
    private String m_name = "MockBaseArchivalUnit";
    int cacheMiss = 0;
    FilterRule rule = null;

    public MyMockBaseArchivalUnit(Plugin myPlugin) {
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

    protected void loadAuConfigDescrs(Configuration config) {
    }

    protected FilterRule constructFilterRule(String mimeType) {
      cacheMiss++;
      return rule;
    }
  }
}
