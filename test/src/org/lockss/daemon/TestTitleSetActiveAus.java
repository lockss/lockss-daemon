/*
 * $Id: TestTitleSetActiveAus.java,v 1.3 2005-06-02 16:39:40 tlipkis Exp $
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

package org.lockss.daemon;

import java.util.*;

import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * This is the test class for org.lockss.daemon.TitleSetActiveAus
 */

public class TestTitleSetActiveAus extends LockssTestCase {
  private static Logger log = Logger.getLogger("TestTitleSetActiveAus");

  MockLockssDaemon daemon;
  MyPluginManager pluginMgr;
  MockPlugin mp;
  MyMockArchivalUnit mau1, mau2;
  TitleConfig tc1, tc2;
  ConfigParamAssignment cpa1, cpa2, cpa3;

  public void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon();
    pluginMgr = new MyPluginManager();
    daemon.setPluginManager(pluginMgr);
    pluginMgr.initService(daemon);
    pluginMgr.startService();
    mp = new MockPlugin();
    ConfigParamDescr d1 = new ConfigParamDescr("key1");
    ConfigParamDescr d2 = new ConfigParamDescr("key2");
    cpa1 = new ConfigParamAssignment(d1, "a");
    cpa2 = new ConfigParamAssignment(d2, "foo");
    cpa3 = new ConfigParamAssignment(d2, "bar");

    tc1 = new TitleConfig("title1", mp);
    tc1.setParams(ListUtil.list(cpa1, cpa2));
    tc2 = new TitleConfig("title2", mp);
    tc2.setParams(ListUtil.list(cpa3));
    mp.setAuConfigDescrs(ListUtil.list(d1, d2));
    mau1 = new MyMockArchivalUnit();
    mau2 = new MyMockArchivalUnit();
    mau1.setPlugin(mp);
    mau2.setPlugin(mp);
    mau1.setName("auname1");
    mau2.setName("auname2");
    mau1.setConfiguration(ConfigurationUtil.fromArgs("key1", "a",
						     "key2", "foo"));
    mau2.setConfiguration(ConfigurationUtil.fromArgs("key1", "a",
						     "key2", "bar"));
  }

  public void tearDown() throws Exception {
    pluginMgr.stopService();
    super.tearDown();
  }

  public void testFromAu() throws Exception {
    RegistryPlugin rplug = new RegistryPlugin();
    // ensure that internal AU doesn't produce title entries
    MyRegistryArchivalUnit rau = new MyRegistryArchivalUnit(rplug);
    mau1.setTitleConfig(tc1);
    rau.setTitleConfig(tc2);
    pluginMgr.addAu(mau1);
    pluginMgr.addAu(rau);
    Collection set =
      new TitleSetActiveAus(getMockLockssDaemon()).getTitles();
    System.err.println(set.getClass().toString());
    assertEquals(SetUtil.set(tc1), SetUtil.theSet(set));

    List lst = new ArrayList(set);
    assertSame(tc1, lst.get(0));
    assertEquals(1, set.size());
  }

  static class MyRegistryArchivalUnit extends RegistryArchivalUnit {
    TitleConfig tc;
    public MyRegistryArchivalUnit(RegistryPlugin plugin) {
      super(plugin);
    }
    public TitleConfig getTitleConfig() {
      return tc;
    }
    public void setTitleConfig(TitleConfig tc) {
      this.tc = tc;
    }
  }

  public void testSynthesized() throws Exception {
    PluginTestUtil.registerArchivalUnit(mp, mau2);
    Collection set =
      new TitleSetActiveAus(getMockLockssDaemon()).getTitles();
    assertEquals(1, set.size());
    List lst = new ArrayList(set);
    TitleConfig tc = (TitleConfig)lst.get(0);
    assertEquals("auname2", tc.getDisplayName());
    assertEquals(SetUtil.set(cpa1, cpa3),
		 SetUtil.theSet(tc.getParams()));
  }

  class MyMockArchivalUnit extends MockArchivalUnit {
    private TitleConfig titleConfig;
    public TitleConfig getTitleConfig() {
      return titleConfig;
    }
    public void setTitleConfig(TitleConfig tc) {
      titleConfig = tc;
    }
  }

  class MyPluginManager extends PluginManager {
    private List aus;
    public List getAllAus() {
      if (aus == null) {
	return super.getAllAus();
      } else {
	return aus;
      }
    }
    public void addAu(ArchivalUnit au) {
      if (aus == null) {
	aus = new ArrayList();
      }
      aus.add(au);
    }
  }
}
