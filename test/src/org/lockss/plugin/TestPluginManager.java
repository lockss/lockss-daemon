/*
 * $Id: TestPluginManager.java,v 1.26 2003-12-23 00:37:32 tlipkis Exp $
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
import java.net.*;
import java.util.*;
import junit.framework.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.repository.*;
import org.lockss.poller.PollSpec;

/**
 * Test class for org.lockss.plugin.PluginManager
 */

public class TestPluginManager extends LockssTestCase {
  private MockLockssDaemon theDaemon;

  static String mockPlugKey = "org|lockss|test|MockPlugin";
  static Properties props1 = new Properties();
  static Properties props2 = new Properties();
  static {
    props1.setProperty(MockPlugin.CONFIG_PROP_1, "val1");
    props1.setProperty(MockPlugin.CONFIG_PROP_2, "val2");
    props2.setProperty(MockPlugin.CONFIG_PROP_1, "val1");
    props2.setProperty(MockPlugin.CONFIG_PROP_2, "va.l3");//auid contains a dot
  }

  static String mauauidKey1 = PropUtil.propsToCanonicalEncodedString(props1);
  static String mauauid1 = mockPlugKey+"&"+ mauauidKey1;
  //  static String m


  static String mauauidKey2 = PropUtil.propsToCanonicalEncodedString(props2);
  static String mauauid2 = mockPlugKey+"&"+mauauidKey2;

  static String p1param =
    PluginManager.PARAM_AU_TREE + "." + mockPlugKey + ".";

  static String p1a1param = p1param + mauauidKey1 + ".";
  static String p1a2param = p1param + mauauidKey2 + ".";

  static String configStr =
    p1a1param + MockPlugin.CONFIG_PROP_1 + "=val1\n" +
    p1a1param + MockPlugin.CONFIG_PROP_2 + "=val2\n" +
    p1a2param + MockPlugin.CONFIG_PROP_1 + "=val1\n" +
    p1a2param + MockPlugin.CONFIG_PROP_2 + "=va.l3\n" + // value contains a dot
// needed to allow PluginManager to register AUs
// leave value blank to allow 'doConfig()' to fill it in dynamically
    LockssRepositoryImpl.PARAM_CACHE_LOCATION + "=";


  PluginManager mgr;

  public TestPluginManager(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();

    theDaemon = new MockLockssDaemon();

    mgr = new PluginManager();
    theDaemon.setPluginManager(mgr);
    theDaemon.setDaemonInited(true);
    mgr.initService(theDaemon);
  }

  public void tearDown() throws Exception {
    mgr.stopService();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  private void doConfig() throws Exception {
    mgr.startService();
    String localConfig = configStr + getTempDir().getAbsolutePath() +
        File.separator;
    ConfigurationUtil.setCurrentConfigFromString(localConfig);
  }

  private void minimalConfig() throws Exception {
    mgr.startService();
    ConfigurationUtil.setFromArgs(LockssRepositoryImpl.PARAM_CACHE_LOCATION,
				  getTempDir().getAbsolutePath() +
				  File.separator);
  }

  public void testNameFromKey() {
    assertEquals("org.lockss.Foo", PluginManager.pluginNameFromKey("org|lockss|Foo"));
  }

  public void testKeyFromName() {
    assertEquals("org|lockss|Foo", PluginManager.pluginKeyFromName("org.lockss.Foo"));
  }

  public void testKeyFromId() {
    assertEquals("org|lockss|Foo", PluginManager.pluginKeyFromId("org.lockss.Foo"));
  }

  public void testEnsurePluginLoaded() throws Exception {
    // non-existent class shouldn't load
    String key = "org|lockss|NoSuchClass";
    assertFalse(mgr.ensurePluginLoaded(key));
    assertNull(mgr.getPlugin(key));
    // MockPlugin should load
    assertTrue(mgr.ensurePluginLoaded(mockPlugKey));
    Plugin p = mgr.getPlugin(mockPlugKey);
    assertTrue(p.toString(), p instanceof MockPlugin);
    MockPlugin mpi = (MockPlugin)mgr.getPlugin(mockPlugKey);
    assertNotNull(mpi);
    assertEquals(1, mpi.getInitCtr());	// should have been inited once

    // second time shouldn't reload, reinstantiate, or reinitialize plugin
    assertTrue(mgr.ensurePluginLoaded(mockPlugKey));
    MockPlugin mpi2 = (MockPlugin)mgr.getPlugin(mockPlugKey);
    assertSame(mpi, mpi2);
    assertEquals(1, mpi.getInitCtr());
  }

  public void testInitPluginRegistry() {
    String n1 = "org.lockss.test.MockPlugin";
    String n2 = ThrowingMockPlugin.class.getName();
    assertEmpty(mgr.getRegisteredPlugins());
    ConfigurationUtil.setFromArgs(PluginManager.PARAM_PLUGIN_REGISTRY,
				  n1 + ";" + n2);
    Plugin p1 = mgr.getPlugin(mgr.pluginKeyFromName(n1));
    assertNotNull(p1);
    assertTrue(p1.toString(), p1 instanceof MockPlugin);
    Plugin p2 = mgr.getPlugin(mgr.pluginKeyFromName(n2));
    assertNotNull(p2);
    assertTrue(p2.toString(), p2 instanceof ThrowingMockPlugin);
    assertEquals(2, mgr.getRegisteredPlugins().size());
    ConfigurationUtil.setFromArgs(PluginManager.PARAM_PLUGIN_REGISTRY, n1);
    assertEquals(1, mgr.getRegisteredPlugins().size());
    assertNull(mgr.getPlugin(mgr.pluginKeyFromName(n2)));
    assertNotNull(mgr.getPlugin(mgr.pluginKeyFromName(n1)));
    assertTrue(mgr.getPlugin(mgr.pluginKeyFromName(n1)) instanceof MockPlugin);
  }

  public void testInitTitleDB() {
    Properties p = new Properties();
    p.put("org.lockss.title.1.foo", "foo1");
    p.put("org.lockss.title.1.bar", "bar1");
    p.put("org.lockss.title.2.bar", "bar2");
    ConfigurationUtil.setCurrentConfigFromProps(p);
  }

  public void testStop() throws Exception {
    doConfig();
    MockPlugin mpi = (MockPlugin)mgr.getPlugin(mockPlugKey);
    assertEquals(0, mpi.getStopCtr());
    mgr.stopService();
    assertEquals(1, mpi.getStopCtr());
  }

  public void testAuConfig() throws Exception {
    doConfig();
    MockPlugin mpi = (MockPlugin)mgr.getPlugin(mockPlugKey);
    // plugin should be registered
    assertNotNull(mpi);
    // should have been inited once
    assertEquals(1, mpi.getInitCtr());

    // get the two archival units
    ArchivalUnit au1 = mgr.getAuFromId(mauauid1);
    ArchivalUnit au2 = mgr.getAuFromId(mauauid2);

    // verify the plugin's set of all AUs is {au1, au2}
    Collection aus = mpi.getAllAus();
    assertEquals(SetUtil.set(au1, au2), new HashSet(mgr.getAllAus()));

    // verify au1's configuration
    assertEquals(mauauid1, au1.getAuId());
    MockArchivalUnit mau1 = (MockArchivalUnit)au1;
    Configuration c1 = mau1.getConfiguration();
    assertEquals("val1", c1.get(MockPlugin.CONFIG_PROP_1));
    assertEquals("val2", c1.get(MockPlugin.CONFIG_PROP_2));

    // verify au1's configuration
    assertEquals(mauauid2, au2.getAuId());
    MockArchivalUnit mau2 = (MockArchivalUnit)au2;
    Configuration c2 = mau2.getConfiguration();
    assertEquals("val1", c2.get(MockPlugin.CONFIG_PROP_1));
    assertEquals("va.l3", c2.get(MockPlugin.CONFIG_PROP_2));

    assertEquals(au1, mgr.getAuFromId(mauauid1));
  }

  public void testCreateAu() throws Exception {
    minimalConfig();
    String pid = new ThrowingMockPlugin().getPluginId();
    String key = PluginManager.pluginKeyFromId(pid);
    assertTrue(mgr.ensurePluginLoaded(key));
    ThrowingMockPlugin mpi = (ThrowingMockPlugin)mgr.getPlugin(key);
    assertNotNull(mpi);
    Configuration config = ConfigurationUtil.fromArgs("a", "b");
    ArchivalUnit au = mgr.createAu(mpi, config);

    // verify put in PluginManager map
    String auid = au.getAuId();
    ArchivalUnit aux = mgr.getAuFromId(auid);
    assertSame(au, aux);

    // verify got right config
    Configuration auConfig = au.getConfiguration();
    assertEquals("b", auConfig.get("a"));
    assertEquals(1, auConfig.keySet().size());
    assertEquals(mpi, au.getPlugin());

    // verify turns RuntimeException into ArchivalUnit.ConfigurationException
    mpi.setCfgEx(new ArchivalUnit.ConfigurationException("should be thrown"));
    try {
      ArchivalUnit au2 = mgr.createAu(mpi, config);
      fail("createAU should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }

    mpi.setRtEx(new ExpectedRuntimeException("Ok if in log"));
    try {
      ArchivalUnit au2 = mgr.createAu(mpi, config);
      fail("createAU should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
      // this is what's expected
    } catch (RuntimeException e) {
      fail("createAU threw RuntimeException");
    }

  }

  public void testConfigureAu() throws Exception {
    minimalConfig();
    String pid = new ThrowingMockPlugin().getPluginId();
    String key = PluginManager.pluginKeyFromId(pid);
    assertTrue(mgr.ensurePluginLoaded(key));
    ThrowingMockPlugin mpi = (ThrowingMockPlugin)mgr.getPlugin(key);
    assertNotNull(mpi);
    Configuration config = ConfigurationUtil.fromArgs("a", "b");
    ArchivalUnit au = mgr.createAu(mpi, config);

    String auid = au.getAuId();
    ArchivalUnit aux = mgr.getAuFromId(auid);
    assertSame(au, aux);

    // verify can reconfig
    mgr.configureAu(mpi, ConfigurationUtil.fromArgs("a", "c"), auid);
    Configuration auConfig = au.getConfiguration();
    assertEquals("c", auConfig.get("a"));
    assertEquals(1, auConfig.keySet().size());

    // verify turns RuntimeException into ArchivalUnit.ConfigurationException
    mpi.setCfgEx(new ArchivalUnit.ConfigurationException("should be thrown"));
    try {
      mgr.configureAu(mpi, config, auid);
      fail("configureAU should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }

    mpi.setRtEx(new ExpectedRuntimeException("Ok if in log"));
    try {
      mgr.configureAu(mpi, config, auid);
      fail("configureAU should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
      // this is what's expected
    } catch (RuntimeException e) {
      fail("createAU threw RuntimeException");
    }

  }

  static class ThrowingMockPlugin extends MockPlugin {
    RuntimeException rtEx;
    ArchivalUnit.ConfigurationException cfgEx;;

    public void setRtEx(RuntimeException rtEx) {
      this.rtEx = rtEx;
    }
    public void setCfgEx(ArchivalUnit.ConfigurationException cfgEx) {
      this.cfgEx = cfgEx;
    }
    public ArchivalUnit createAu(Configuration config)
	throws ArchivalUnit.ConfigurationException {
      if (rtEx != null) {
	throw rtEx;
      } else if (cfgEx != null) {
	throw cfgEx;
      } else {
	return super.createAu(config);
      }
    }
    public ArchivalUnit configureAu(Configuration config, ArchivalUnit au)
	throws ArchivalUnit.ConfigurationException {
      if (rtEx != null) {
	throw rtEx;
      } else if (cfgEx != null) {
	throw cfgEx;
      } else {
	return super.configureAu(config, au);
      }
    }
  }


  public void testFindCus() throws Exception {
    String url = "http://foo.bar/";
    String lower = "abc";
    String upper = "xyz";

    doConfig();
    MockPlugin mpi = (MockPlugin)mgr.getPlugin(mockPlugKey);

    // make a PollSpec with info from a manually created CUS, which should
    // match one of the registered AUs
    CachedUrlSet protoCus = makeCus(mpi, mauauid1, url, lower, upper);
    PollSpec ps1 = new PollSpec(protoCus);

    // verify PluginManager can make a CUS for the PollSpec
    CachedUrlSet cus = mgr.findCachedUrlSet(ps1);
    assertNotNull(cus);
    // verify the CUS's CUSS
    CachedUrlSetSpec cuss = cus.getSpec();
    assertEquals(url, cuss.getUrl());
    RangeCachedUrlSetSpec rcuss = (RangeCachedUrlSetSpec)cuss;
    assertEquals(lower, rcuss.getLowerBound());
    assertEquals(upper, rcuss.getUpperBound());

    assertEquals(mauauid1, cus.getArchivalUnit().getAuId());
    // can't test protoCus.getArchivalUnit() .equals( cus.getArchivalUnit() )
    // as we made a fake mock one to build PollSpec, and PluginManager will
    // have created & configured a real mock one.

    CachedUrlSet protoAuCus = makeAuCus(mpi, mauauid1);
    PollSpec ps2 = new PollSpec(protoAuCus);

    CachedUrlSet aucus = mgr.findCachedUrlSet(ps2);
    assertNotNull(aucus);
    CachedUrlSetSpec aucuss = aucus.getSpec();
    assertTrue(aucuss instanceof AuCachedUrlSetSpec);
  }

  public void testFindSingleNodeCus() throws Exception {
    String url = "http://foo.bar/";
    String lower = PollSpec.SINGLE_NODE_LWRBOUND;

    doConfig();
    MockPlugin mpi = (MockPlugin)mgr.getPlugin(mockPlugKey);

    // make a PollSpec with info from a manually created CUS, which should
    // match one of the registered AUs
    CachedUrlSet protoCus = makeCus(mpi, mauauid1, url, lower, null);
    PollSpec ps1 = new PollSpec(protoCus);

    // verify PluginManager can make a CUS for the PollSpec
    CachedUrlSet cus = mgr.findCachedUrlSet(ps1);
    assertNotNull(cus);
    // verify the CUS's CUSS
    CachedUrlSetSpec cuss = cus.getSpec();
    assertTrue(cuss instanceof SingleNodeCachedUrlSetSpec);
    assertEquals(url, cuss.getUrl());
  }

  public void testFindMostRecentCachedUrl() throws Exception {
    String prefix = "http://foo.bar/";
    String url1 = "http://foo.bar/baz";
    String url2 = "http://foo.bar/not";
    doConfig();
    MockPlugin mpi = (MockPlugin)mgr.getPlugin(mockPlugKey);

    // get the two archival units
    MockArchivalUnit au1 = (MockArchivalUnit)mgr.getAuFromId(mauauid1);
    ArchivalUnit au2 = mgr.getAuFromId(mauauid2);
    assertNull(mgr.findMostRecentCachedUrl(url1));
    CachedUrlSetSpec cuss = new MockCachedUrlSetSpec(prefix, null);
    MockCachedUrlSet mcuss = new MockCachedUrlSet(au1, cuss);
    mcuss.addUrl("foo", url1, true, true, null);
    au1.setAuCachedUrlSet(mcuss);
    CachedUrl cu = mgr.findMostRecentCachedUrl(url1);
    assertNotNull(cu);
    assertEquals(url1, cu.getUrl());
    assertNull(mgr.findMostRecentCachedUrl(url2));
  }

  public void testGenerateAuId() {
    Properties props = new Properties();
    props.setProperty("key&1", "val=1");
    props.setProperty("key2", "val 2");
    props.setProperty("key.3", "val:3");
    props.setProperty("key4", "val.4");
    String pluginId = "org|lockss|plugin|Blah";

    String actual = PluginManager.generateAuId(pluginId, props);
    String expected =
      pluginId+"&"+
      "key%261~val%3D1&"+
      "key%2E3~val%3A3&"+
      "key2~val+2&"+
      "key4~val%2E4";
    assertEquals(expected, actual);
  }


  public void testConfigKeyFromAuId() {
    String pluginId = "org|lockss|plugin|Blah";
    String auId = "base_url~foo&volume~123";

    String totalId = PluginManager.generateAuId(pluginId, auId);
    String expectedStr = pluginId + "." + auId;
    assertEquals(expectedStr, PluginManager.configKeyFromAuId(totalId));
  }

  public CachedUrlSet makeCus(Plugin plugin, String auid, String url,
			       String lower, String upper) {
    MockArchivalUnit au = new MockArchivalUnit();
    au.setAuId(auid);
    au.setPlugin(plugin);
    au.setPluginId(plugin.getPluginId());

    CachedUrlSet cus = new MockCachedUrlSet(au,
					    new RangeCachedUrlSetSpec(url,
								      lower,
								      upper));
    return cus;
  }

  public CachedUrlSet makeAuCus(Plugin plugin, String auid) {
    MockArchivalUnit au = new MockArchivalUnit();
    au.setAuId(auid);
    au.setPlugin(plugin);
    au.setPluginId(plugin.getPluginId());

    CachedUrlSet cus = new MockCachedUrlSet(au, new AuCachedUrlSetSpec());
    return cus;
  }

  private static String wkey = "org|lockss|plugin|wrapper|WrappedPlugin";

  public void testWrappedAu() {
    if (WrapperState.isUsingWrapping()) {
      try {
        mgr.startService();
        String localConfig = p1a1param + MockPlugin.CONFIG_PROP_1 + "=val1\n" +
            p1a1param + MockPlugin.CONFIG_PROP_2 + "=val2\n" +
            p1a1param + "reserved.wrapper=true\n" +
            LockssRepositoryImpl.PARAM_CACHE_LOCATION + "=" +
            getTempDir().getAbsolutePath() + File.separator + "\n";
        ConfigurationUtil.setCurrentConfigFromString(localConfig);
        ArchivalUnit wau = (ArchivalUnit) mgr.getAuFromId(
            mauauid1);
        Plugin wplug = (Plugin) wau.getPlugin();
        MockPlugin mock = (MockPlugin) WrapperState.getOriginal(wplug);
        assertSame(mock,wplug);
        MockArchivalUnit mau = (MockArchivalUnit) WrapperState.getOriginal(wau);
        assertSame(mock, mau.getPlugin());
        assertSame(mau,wau);
      } catch (IOException e) {
        fail(e.getMessage());
      } catch (ClassCastException e) {
        fail("WrappedArchivalUnit not found.");
      }
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(TestPluginManager.class);
    return suite;
  }


}
