/*
 * $Id: TestBasePlugin.java,v 1.4 2004-01-27 04:07:06 tlipkis Exp $
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
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;

/**
 * This is the test class for org.lockss.plugin.base.BasePlugin
 *
 */
public class TestBasePlugin extends LockssTestCase {

  static final ConfigParamDescr PD_VOL = ConfigParamDescr.VOLUME_NUMBER;
  static final ConfigParamDescr PD_YEAR = ConfigParamDescr.YEAR;

  static final String AUPARAM_VOL = PD_VOL.getKey();
  static final String AUPARAM_YEAR = PD_YEAR.getKey();

  MockBasePlugin mbp;

  public void setUp() throws Exception {
    super.setUp();
    mbp = new MockBasePlugin();
    mbp.initPlugin(getMockLockssDaemon());
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  /** test for method configureAu(..) */
  public void testConfigureAu() {
    // check for null config throws exception
    try {
      mbp.configureAu(null, null);
      fail("Didn't throw ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }

  public void testInitTitleDB() {
    mbp.setConfigDescrs(ListUtil.list(PD_VOL, PD_YEAR));
    Properties p = new Properties();
    p.put("org.lockss.title.0.title", "Not me");
    p.put("org.lockss.title.0.plugin", "org.lockss.NotThisClass");
    p.put("org.lockss.title.1.title", "It's");
    p.put("org.lockss.title.1.plugin",
	  "org.lockss.plugin.base.TestBasePlugin$MockBasePlugin");
    p.put("org.lockss.title.1.param.1.key", AUPARAM_VOL);
    p.put("org.lockss.title.1.param.1.value", "vol_1");
    p.put("org.lockss.title.1.param.2.key", AUPARAM_YEAR);
    p.put("org.lockss.title.1.param.2.value", "year_1");
    p.put("org.lockss.title.1.param.2.editable", "true");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    assertEquals(ListUtil.list("It's"), mbp.getSupportedTitles());
    TitleConfig tc = mbp.getTitleConfig(new String("It's"));
    assertEquals("It's", tc.getDisplayName());
    ConfigParamAssignment epa1 = new ConfigParamAssignment(PD_YEAR, "year_1");
    epa1.setEditable(true);
    assertEquals(SetUtil.set(epa1, new ConfigParamAssignment(PD_VOL, "vol_1")),
		 SetUtil.theSet(tc.getParams()));
  }

  private static class MockBasePlugin extends BasePlugin {
    String name;
    String version;
    List configDescrs;

    public MockBasePlugin() {
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
      TestBaseArchivalUnit.MockBaseArchivalUnit mau =
          new TestBaseArchivalUnit.MockBaseArchivalUnit(this);
      mau.setConfiguration(auConfig);
      return mau;
    }

    public String getVersion() {
      return version;
    }

    public String getPluginName() {
      return name;
    }

    public List getAuConfigDescrs() {
      return configDescrs;
    }
  }
}
