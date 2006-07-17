/*
 * $Id: TestAuUtil.java,v 1.4 2006-07-17 05:06:13 tlipkis Exp $
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
import org.lockss.test.*;
import org.lockss.plugin.base.*;
import org.lockss.util.*;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;

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
    mau.setConfiguration(ConfigurationUtil.fromArgs(ConfigParamDescr.PUB_DOWN.getKey(), "true"));
    assertTrue(AuUtil.isPubDown(mau));
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


  private static class LocalMockArchivalUnit extends MockArchivalUnit {
    TitleConfig tc = null;
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

    public ArchivalUnit createAu(Configuration auConfig) throws
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
