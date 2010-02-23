/*
 * $Id: TestAuUtil.java,v 1.9.8.1 2010-02-23 06:18:38 tlipkis Exp $
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

package org.lockss.plugin;

import java.io.*;
import java.util.*;
import org.lockss.app.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.crawler.*;
import org.lockss.state.*;
import org.lockss.test.*;
import org.lockss.plugin.base.*;
import org.lockss.util.*;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.exploded.*;

/**
 * This is the test class for org.lockss.plugin.AuUtil
 */
public class TestAuUtil extends LockssTestCase {

  static final ConfigParamDescr PD_VOL = ConfigParamDescr.VOLUME_NUMBER;
  static final ConfigParamDescr PD_YEAR = ConfigParamDescr.YEAR;
  static final ConfigParamDescr PD_OPT = new ConfigParamDescr("OPT_KEY");
  static {
    PD_OPT.setDefinitional(false);
  }

  static final String AUPARAM_VOL = PD_VOL.getKey();
  static final String AUPARAM_YEAR = PD_YEAR.getKey();
  static final String AUPARAM_OPT = PD_OPT.getKey();

  LocalMockPlugin mbp;

  public void setUp() throws Exception {
    super.setUp();
    mbp = new LocalMockPlugin();
    mbp.initPlugin(getMockLockssDaemon());
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  TitleConfig makeTitleConfig(ConfigParamDescr descr, String val) {
    TitleConfig tc = new TitleConfig("foo", new MockPlugin());
    tc.setParams(ListUtil.list(new ConfigParamAssignment(descr, val)));
    return tc;
  }

  public void testGetDaemon()  {
    LocalMockArchivalUnit mau = new LocalMockArchivalUnit(mbp);
    assertSame(getMockLockssDaemon(), AuUtil.getDaemon(mau));
  }

  public void testGetAuState() throws IOException {
    setUpDiskPaths();
    LocalMockArchivalUnit mau = new LocalMockArchivalUnit(mbp);
    getMockLockssDaemon().getNodeManager(mau).startService();
    AuState aus = AuUtil.getAuState(mau);
    assertEquals(AuState.CLOCKSS_SUB_UNKNOWN,
		 aus.getClockssSubscriptionStatus());
  }

  public void testIsClosed() throws Exception {
    LocalMockArchivalUnit mau = new LocalMockArchivalUnit();
    assertFalse(AuUtil.isClosed(mau));
    mau.setConfiguration(ConfigurationUtil.fromArgs(ConfigParamDescr.AU_CLOSED.getKey(), "true"));
    assertTrue(AuUtil.isClosed(mau));
    mau.setTitleConfig(makeTitleConfig(ConfigParamDescr.AU_CLOSED, "false"));
    assertTrue(AuUtil.isClosed(mau));
    mau.setConfiguration(ConfigurationUtil.fromArgs("foo", "bar"));
    assertFalse(AuUtil.isClosed(mau));
    mau.setTitleConfig(makeTitleConfig(ConfigParamDescr.AU_CLOSED, "true"));
    assertTrue(AuUtil.isClosed(mau));
    mau.setTitleConfig(makeTitleConfig(ConfigParamDescr.AU_CLOSED, "false"));
    assertFalse(AuUtil.isClosed(mau));
  }

  public void testIsPubDown() throws Exception {
    LocalMockArchivalUnit mau = new LocalMockArchivalUnit();
    assertFalse(AuUtil.isPubDown(mau));
    assertFalse(AuUtil.isPubNever(mau));
    mau.setConfiguration(ConfigurationUtil.fromArgs(ConfigParamDescr.PUB_DOWN.getKey(), "true"));
    assertTrue(AuUtil.isPubDown(mau));
    assertFalse(AuUtil.isPubNever(mau));
    mau.setTitleConfig(makeTitleConfig(ConfigParamDescr.PUB_DOWN, "false"));
    assertTrue(AuUtil.isPubDown(mau));
    mau.setConfiguration(ConfigurationUtil.fromArgs("foo", "bar"));
    assertFalse(AuUtil.isPubDown(mau));
    mau.setTitleConfig(makeTitleConfig(ConfigParamDescr.PUB_DOWN, "true"));
    assertTrue(AuUtil.isPubDown(mau));
    mau.setTitleConfig(makeTitleConfig(ConfigParamDescr.PUB_DOWN, "false"));
    assertFalse(AuUtil.isPubDown(mau));
  }

  public void testIsPubDownTC() throws Exception {
    assertTrue(AuUtil.isPubDown(makeTitleConfig(ConfigParamDescr.PUB_DOWN,
						"true")));
    assertFalse(AuUtil.isPubDown(makeTitleConfig(ConfigParamDescr.PUB_DOWN,
						 "false")));
    assertFalse(AuUtil.isPubDown(makeTitleConfig(ConfigParamDescr.BASE_URL,
						 "http://foo.bar/")));
  }

  public void testIsPubNever() throws Exception {
    LocalMockArchivalUnit mau = new LocalMockArchivalUnit();
    assertFalse(AuUtil.isPubNever(mau));
    assertFalse(AuUtil.isPubDown(mau));
    mau.setConfiguration(ConfigurationUtil.fromArgs(ConfigParamDescr.PUB_NEVER.getKey(), "true"));
    assertTrue(AuUtil.isPubNever(mau));
    assertTrue(AuUtil.isPubDown(mau));
    mau.setTitleConfig(makeTitleConfig(ConfigParamDescr.PUB_NEVER, "false"));
    assertTrue(AuUtil.isPubNever(mau));
    assertTrue(AuUtil.isPubDown(mau));
    mau.setConfiguration(ConfigurationUtil.fromArgs("foo", "bar"));
    assertFalse(AuUtil.isPubNever(mau));
    assertFalse(AuUtil.isPubDown(mau));
    mau.setTitleConfig(makeTitleConfig(ConfigParamDescr.PUB_NEVER, "true"));
    assertTrue(AuUtil.isPubNever(mau));
    assertTrue(AuUtil.isPubDown(mau));
    mau.setTitleConfig(makeTitleConfig(ConfigParamDescr.PUB_NEVER, "false"));
    assertFalse(AuUtil.isPubNever(mau));
    assertFalse(AuUtil.isPubDown(mau));
  }

  public void testIsPubNeverTC() throws Exception {
    assertTrue(AuUtil.isPubNever(makeTitleConfig(ConfigParamDescr.PUB_NEVER,
						 "true")));
    assertFalse(AuUtil.isPubNever(makeTitleConfig(ConfigParamDescr.PUB_NEVER,
						  "false")));
    assertFalse(AuUtil.isPubNever(makeTitleConfig(ConfigParamDescr.BASE_URL,
						  "http://foo.bar/")));
  }

  public void testGetTitleAttribute() {
    LocalMockArchivalUnit mau = new LocalMockArchivalUnit();
    TitleConfig tc = makeTitleConfig(ConfigParamDescr.PUB_DOWN, "false");
    mau.setTitleConfig(tc);
    assertNull(AuUtil.getTitleAttribute(mau, null));
    assertNull(AuUtil.getTitleAttribute(mau, "foo"));
    assertEquals("7", AuUtil.getTitleAttribute(mau, null, "7"));
    assertEquals("7", AuUtil.getTitleAttribute(mau, "foo", "7"));
    Map attrs = new HashMap();
    tc.setAttributes(attrs);
    assertNull(AuUtil.getTitleAttribute(mau, null));
    assertNull(AuUtil.getTitleAttribute(mau, "foo"));
    assertEquals("7", AuUtil.getTitleAttribute(mau, null, "7"));
    assertEquals("7", AuUtil.getTitleAttribute(mau, "foo", "7"));
    attrs.put("foo", "bar");
    assertEquals("bar", AuUtil.getTitleAttribute(mau, "foo"));
    assertEquals("bar", AuUtil.getTitleAttribute(mau, "foo", "7"));
  }

  public void testGetTitleDefault() {
    TitleConfig tc = makeTitleConfig(ConfigParamDescr.CRAWL_PROXY, "foo:47");
    assertEquals(null, AuUtil.getTitleDefault(tc, ConfigParamDescr.BASE_URL));
    assertEquals("foo:47",
		 AuUtil.getTitleDefault(tc, ConfigParamDescr.CRAWL_PROXY));
  }

  public void testGetAuParamOrTitleDefault() throws Exception {
    LocalMockArchivalUnit mau = new LocalMockArchivalUnit();
    TitleConfig tc = makeTitleConfig(ConfigParamDescr.CRAWL_PROXY, "foo:47");
    assertNull(AuUtil.getAuParamOrTitleDefault(mau,
 					       ConfigParamDescr.CRAWL_PROXY));
    mau.setTitleConfig(tc);
    assertEquals("foo:47",
		 AuUtil.getAuParamOrTitleDefault(mau,
						 ConfigParamDescr.CRAWL_PROXY));
    Configuration config =
      ConfigurationUtil.fromArgs(ConfigParamDescr.CRAWL_PROXY.getKey(),
				 "abc:8080");
    mau.setConfiguration(config);
    assertEquals("abc:8080",
		 AuUtil.getAuParamOrTitleDefault(mau,
						 ConfigParamDescr.CRAWL_PROXY));
  }

  void setGlobalProxy(String host, int port) {
    Properties p = new Properties();
    if (host != null) {
      p.put(BaseCrawler.PARAM_PROXY_ENABLED, "true");
      p.put(BaseCrawler.PARAM_PROXY_HOST, host);
      p.put(BaseCrawler.PARAM_PROXY_PORT, ""+port);
    } else {
      p.put(BaseCrawler.PARAM_PROXY_ENABLED, "false");
    }
    ConfigurationUtil.setCurrentConfigFromProps(p);
  }
				    
  public void testGetAuProxyInfo() throws Exception {
    AuUtil.AuProxyInfo aupi;
    TitleConfig tc;

    LocalMockArchivalUnit mau = new LocalMockArchivalUnit();
    aupi = AuUtil.getAuProxyInfo(mau);
    assertEquals(null, aupi.getHost());
    assertFalse(aupi.isAuOverride());

    setGlobalProxy("host", 1111);
    aupi = AuUtil.getAuProxyInfo(mau);
    assertFalse(aupi.isAuOverride());
    assertEquals("host", aupi.getHost());
    assertEquals(1111, aupi.getPort());

    tc = makeTitleConfig(ConfigParamDescr.CRAWL_PROXY, "foo:47");
    mau.setTitleConfig(tc);
    aupi = AuUtil.getAuProxyInfo(mau);
    assertTrue(aupi.isAuOverride());
    assertEquals("foo", aupi.getHost());
    assertEquals(47, aupi.getPort());

    tc = makeTitleConfig(ConfigParamDescr.CRAWL_PROXY, "HOST:1111");
    mau.setTitleConfig(tc);
    aupi = AuUtil.getAuProxyInfo(mau);
    assertFalse(aupi.isAuOverride());
    assertEquals("HOST", aupi.getHost());
    assertEquals(1111, aupi.getPort());

    setGlobalProxy(null, 0);
    aupi = AuUtil.getAuProxyInfo(mau);
    assertTrue(aupi.isAuOverride());
    assertEquals("HOST", aupi.getHost());
    assertEquals(1111, aupi.getPort());

    tc = makeTitleConfig(ConfigParamDescr.CRAWL_PROXY, "HOST:1112");
    mau.setTitleConfig(tc);
    aupi = AuUtil.getAuProxyInfo(mau);
    assertTrue(aupi.isAuOverride());
    assertEquals("HOST", aupi.getHost());
    assertEquals(1112, aupi.getPort());
  }

  public void testIsConfigCompatibleWithPlugin() {
    String plugName = "org.lockss.plugin.base.TestAuUtil$MyMockBasePlugin";
    mbp.setConfigDescrs(ListUtil.list(PD_VOL, PD_YEAR, PD_OPT));
    Configuration auconf;
    Properties p = new Properties();

    // missing definitional param
    p.put(AUPARAM_VOL, "42");
    auconf = ConfigurationUtil.fromProps(p);
    assertFalse(AuUtil.isConfigCompatibleWithPlugin(auconf, mbp));

    // has all definitional params
    p.put(AUPARAM_YEAR, "1942");
    auconf = ConfigurationUtil.fromProps(p);
    assertTrue(AuUtil.isConfigCompatibleWithPlugin(auconf, mbp));

    // extra non-definitional
    p.put(AUPARAM_OPT, "foo");
    auconf = ConfigurationUtil.fromProps(p);
    assertTrue(AuUtil.isConfigCompatibleWithPlugin(auconf, mbp));

    // wrong type
    p.put(AUPARAM_YEAR, "foo");
    auconf = ConfigurationUtil.fromProps(p);
    assertFalse(AuUtil.isConfigCompatibleWithPlugin(auconf, mbp));
  }

  public void testOkDeleteExtraFiles() {
    assertTrue(AuUtil.okDeleteExtraFiles(new MockArchivalUnit()));
    assertFalse(AuUtil.okDeleteExtraFiles(new ExplodedArchivalUnit(new ExplodedPlugin(), null)));
  }

  private static class LocalMockArchivalUnit extends MockArchivalUnit {
    TitleConfig tc = null;

    LocalMockArchivalUnit() {
      super();
    }

    LocalMockArchivalUnit(Plugin plugin) {
      super(plugin);
    }

    public TitleConfig getTitleConfig() {
      return tc;
    }
    public void setTitleConfig(TitleConfig tc) {
      this.tc = tc;
    }
  }

  private static class LocalMockPlugin extends BasePlugin {
    String name;
    String version;
    List configDescrs;

    public LocalMockPlugin() {
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

    protected ArchivalUnit createAu0(Configuration auConfig) throws
        ConfigurationException {
      MockArchivalUnit mau = new MockArchivalUnit();
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
  }
}
