/*
 * $Id: TestBasePlugin.java,v 1.20 2010-03-14 18:33:45 tlipkis Exp $
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
import org.lockss.app.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.extractor.*;
import org.lockss.util.*;

/**
 * This is the test class for org.lockss.plugin.base.BasePlugin
 *
 */
public class TestBasePlugin extends LockssTestCase {

  static final ConfigParamDescr PD_VOL = ConfigParamDescr.VOLUME_NUMBER;
  static final ConfigParamDescr PD_YEAR = ConfigParamDescr.YEAR;

  static final String AUPARAM_VOL = PD_VOL.getKey();
  static final String AUPARAM_YEAR = PD_YEAR.getKey();

  MyBasePlugin mbp;

  public void setUp() throws Exception {
    super.setUp();
    mbp = new MyBasePlugin();
    mbp.initPlugin(getMockLockssDaemon());
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testConfigureAuNull() {
    // check for null config throws exception
    try {
      mbp.configureAu(null, null);
      fail("Didn't throw ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }

  public void testConfigureAu() throws Exception {
    mbp.setConfigDescrs(ListUtil.list(ConfigParamDescr.BASE_URL,
				      ConfigParamDescr.VOLUME_NUMBER));
    Configuration auconf =
      ConfigurationUtil.fromArgs(ConfigParamDescr.BASE_URL.getKey(),
				 "http://example.com/foo/",
				 ConfigParamDescr.VOLUME_NUMBER.getKey(),
				 "123");
    ArchivalUnit au = mbp.configureAu(auconf, null);
    assertNotNull(au);
    Configuration resconf = au.getConfiguration();
    assertEquals(auconf, resconf);
  }

  public void testStopPlugin() {
    assertEquals(0, mbp.configCbCnt);
    ConfigurationUtil.setFromArgs("foo", "bar");
    assertEquals(1, mbp.configCbCnt);
    ConfigurationUtil.setFromArgs("foo", "baz");
    assertEquals(2, mbp.configCbCnt);
    mbp.stopPlugin();
    ConfigurationUtil.setFromArgs("foo", "bat");
    assertEquals(2, mbp.configCbCnt);
  }
    
  public void testStopPluginWithAus() throws Exception {
    assertEquals(0, mbp.configCbCnt);
    ConfigurationUtil.setFromArgs("foo", "bar");
    assertEquals(1, mbp.configCbCnt);

    mbp.setConfigDescrs(ListUtil.list(ConfigParamDescr.BASE_URL,
				      ConfigParamDescr.VOLUME_NUMBER));
    Configuration auconf =
      ConfigurationUtil.fromArgs(ConfigParamDescr.BASE_URL.getKey(),
				 "http://example.com/foo/",
				 ConfigParamDescr.VOLUME_NUMBER.getKey(),
				 "123");
    ArchivalUnit au = mbp.configureAu(auconf, null);

    ConfigurationUtil.setFromArgs("foo", "baz");
    assertEquals(2, mbp.configCbCnt);
    mbp.stopPlugin();
    ConfigurationUtil.setFromArgs("foo", "bat");
    assertEquals(3, mbp.configCbCnt);
    mbp.stopAu(au);
    ConfigurationUtil.setFromArgs("foo", "bbb");
    assertEquals(3, mbp.configCbCnt);
  }

  public void testGetAuConfigDescrs() {
    mbp.setConfigDescrs(ListUtil.list(PD_VOL, PD_YEAR));
    List descrs = mbp.getAuConfigDescrs();
    assertEquals(SetUtil.set(PD_VOL, PD_YEAR,
			     PD_YEAR.getDerivedDescr("au_short_year"),
			     ConfigParamDescr.AU_CLOSED,
			     ConfigParamDescr.PUB_DOWN,
			     ConfigParamDescr.PUB_NEVER,
			     ConfigParamDescr.PROTOCOL_VERSION,
			     ConfigParamDescr.CRAWL_PROXY),
		 SetUtil.theSet(descrs));
  }

  public void testFindAuConfigDescr() {
    mbp.setConfigDescrs(ListUtil.list(PD_VOL, PD_YEAR));
    assertEquals(PD_VOL, mbp.findAuConfigDescr(PD_VOL.getKey()));
    assertEquals(PD_YEAR, mbp.findAuConfigDescr(PD_YEAR.getKey()));
    assertEquals(ConfigParamDescr.AU_CLOSED,
		 mbp.findAuConfigDescr(ConfigParamDescr.AU_CLOSED.getKey()));
    assertNull(mbp.findAuConfigDescr("noparam"));
  }

  public void testInitTitleDB() {
    String plugName = "org.lockss.plugin.base.TestBasePlugin$MyBasePlugin";
    mbp.setConfigDescrs(ListUtil.list(PD_VOL, PD_YEAR));
    Properties p = new Properties();
    p.put("org.lockss.title.0.title", "Not me");
    p.put("org.lockss.title.0.plugin", "org.lockss.NotThisClass");
    p.put("org.lockss.title.1.title", "It's");
    p.put("org.lockss.title.1.journalTitle", "jtitle");
    p.put("org.lockss.title.1.plugin", plugName);
    p.put("org.lockss.title.1.pluginVersion", "4");
    p.put("org.lockss.title.1.param.1.key", AUPARAM_VOL);
    p.put("org.lockss.title.1.param.1.value", "vol_1");
    p.put("org.lockss.title.1.param.2.key", AUPARAM_YEAR);
    p.put("org.lockss.title.1.param.2.value", "year_1");
    p.put("org.lockss.title.1.param.2.editable", "true");
    p.put("org.lockss.title.2.title", "Howl");
    p.put("org.lockss.title.2.journalTitle", "hj");
    p.put("org.lockss.title.2.plugin", plugName);
    p.put("org.lockss.title.2.pluginVersion", "4");
    p.put("org.lockss.title.2.attributes.attr1", "av111");
    p.put("org.lockss.title.2.attributes.attr2", "av222");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    assertSameElements(ListUtil.list("It's", "Howl"),
		       mbp.getSupportedTitles());
    TitleConfig tc = mbp.getTitleConfig(new String("It's"));
    assertEquals("It's", tc.getDisplayName());
    assertEquals("jtitle", tc.getJournalTitle());
    assertEquals("4", tc.getPluginVersion());
    assertEquals(plugName, tc.getPluginName());
    ConfigParamAssignment epa1 = new ConfigParamAssignment(PD_YEAR, "year_1");
    epa1.setEditable(true);
    assertEquals(SetUtil.set(epa1, new ConfigParamAssignment(PD_VOL, "vol_1")),
		 SetUtil.theSet(tc.getParams()));
    assertNull(tc.getAttributes());

    tc = mbp.getTitleConfig(new String("Howl"));
    assertEquals("av111", tc.getAttributes().get("attr1"));
    assertEquals("av222", tc.getAttributes().get("attr2"));
  }

  public void testGetMimeTypeInfo() throws IOException {
    assertSame(MimeTypeInfo.NULL_INFO, mbp.getMimeTypeInfo(null));
    assertSame(MimeTypeInfo.NULL_INFO, mbp.getMimeTypeInfo(""));
    assertSame(MimeTypeInfo.NULL_INFO, mbp.getMimeTypeInfo("foo/bar"));
    MimeTypeInfo mti = mbp.getMimeTypeInfo("text/html");
    assertTrue(mti.getLinkExtractorFactory()
	       instanceof GoslingHtmlLinkExtractor.Factory);
    assertNull(mti.getHashFilterFactory());
    assertNull(mti.getCrawlFilterFactory());
    mti = mbp.getMimeTypeInfo("text/css");
    assertTrue(mti.getLinkExtractorFactory()
	       instanceof CssLinkExtractor.Factory);
    assertNull(mti.getHashFilterFactory());
    assertNull(mti.getCrawlFilterFactory());
  }

  public void testGetLinkExtractor() throws IOException {
    LinkExtractor ue = mbp.getLinkExtractor("text/html");
    assertTrue(ue instanceof GoslingHtmlLinkExtractor);
  }

  public void testFilterRuleCaching() throws IOException {
    MockFilterRule rule1 = new MockFilterRule();
    rule1.setFilteredReader(new StringReader("rule1"));
    MockFilterRule rule2 = new MockFilterRule();
    rule2.setFilteredReader(new StringReader("rule2"));

    assertNull(mbp.rule);
    assertEquals(0, mbp.ruleCacheMiss);
    assertNull(mbp.getFilterRule("test1"));
    assertEquals(1, mbp.ruleCacheMiss);
    mbp.rule = rule1;
    assertNotNull(mbp.getFilterRule("test1"));
    assertEquals(2, mbp.ruleCacheMiss);
    mbp.rule = rule2;
    assertNotNull(mbp.getFilterRule("test2"));
    assertEquals(3, mbp.ruleCacheMiss);

    rule1 = (MockFilterRule)mbp.getFilterRule("test2");
    assertEquals(3, mbp.ruleCacheMiss);
    assertEquals("rule2", StringUtil.fromReader(
        rule1.createFilteredReader(null)));
    rule2 = (MockFilterRule)mbp.getFilterRule("test1");
    assertEquals(3, mbp.ruleCacheMiss);
    assertEquals("rule1", StringUtil.fromReader(
        rule2.createFilteredReader(null)));
  }

  public void donttestFilterFactoryCaching() throws IOException {
    MockFilterFactory factory1 = new MockFilterFactory();
    factory1.setFilteredInputStream(new StringInputStream("factory1"));
    MockFilterFactory factory2 = new MockFilterFactory();
    factory2.setFilteredInputStream(new StringInputStream("factory2"));

    assertNull(mbp.factory);
    assertEquals(0, mbp.factoryCacheMiss);
    assertNull(mbp.getHashFilterFactory("test1"));
    assertEquals(1, mbp.factoryCacheMiss);
    mbp.factory = factory1;
    assertNotNull(mbp.getHashFilterFactory("test1"));
    assertEquals(2, mbp.factoryCacheMiss);
    mbp.factory = factory2;
    assertNotNull(mbp.getHashFilterFactory("test2"));
    assertEquals(3, mbp.factoryCacheMiss);

    factory1 = (MockFilterFactory)mbp.getHashFilterFactory("test2");
    assertEquals(3, mbp.factoryCacheMiss);
    assertEquals("factory2", StringUtil.fromInputStream(
        factory1.createFilteredInputStream(null, null, null)));
    factory2 = (MockFilterFactory)mbp.getHashFilterFactory("test1");
    assertEquals(3, mbp.factoryCacheMiss);
    assertEquals("factory1", StringUtil.fromInputStream(
        factory2.createFilteredInputStream(null, null, null)));
  }

  private static class MyBasePlugin extends BasePlugin {
    String name;
    String version;
    List configDescrs;
    int ruleCacheMiss = 0;
    int factoryCacheMiss = 0;
    FilterRule rule = null;
    FilterFactory factory = null;
    int configCbCnt = 0;

    public MyBasePlugin() {
      super();
    }

    public void setPluginName(String name) {
      this.name = name;
    }

    public void setVersion(String version) {
      this.version = version;
    }

    public void setConfigDescrs(List configDescrs) {
      this.configDescrs = configDescrs;
    }

    @Override
    public void setConfig(Configuration newConfig,
			  Configuration prevConfig,
			  Configuration.Differences changedKeys) {
      configCbCnt++;
      super.setConfig(newConfig, prevConfig, changedKeys);
    }

    protected ArchivalUnit createAu0(Configuration auConfig) throws
        ConfigurationException {
      TestBaseArchivalUnit.MyBaseArchivalUnit mau =
          new TestBaseArchivalUnit.MyBaseArchivalUnit(this);
      mau.setConfiguration(auConfig);
      return mau;
    }

    public String getVersion() {
      return version;
    }

    public String getPluginName() {
      return name;
    }

    public List getLocalAuConfigDescrs() {
      return configDescrs;
    }

    protected FilterRule constructFilterRule(String mimeType) {
      ruleCacheMiss++;
      return rule;
    }

    protected FilterFactory constructFilterFactory(String mimeType) {
      factoryCacheMiss++;
      return factory;
    }

  }
}
