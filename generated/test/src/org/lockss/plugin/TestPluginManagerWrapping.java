/*
 * $Id: TestPluginManagerWrapping.java,v 1.1 2003-09-04 23:11:16 tyronen Exp $
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

import org.lockss.daemon.*;
import org.lockss.plugin.wrapper.*;
import org.lockss.poller.*;
import org.lockss.repository.*;
import org.lockss.test.*;
import org.lockss.util.*;
import junit.framework.*;

/**
 * Test class for org.lockss.plugin.PluginManager
 */

public class TestPluginManagerWrapping extends LockssTestCase {
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
    p1a1param + "reserved.wrapper=true\n" +
    p1a2param + "reserved.wrapper=true\n" +
    p1a2param + MockPlugin.CONFIG_PROP_1 + "=val1\n" +
    p1a2param + MockPlugin.CONFIG_PROP_2 + "=va.l3\n" + // value contains a dot
// needed to allow PluginManager to register AUs
// leave value blank to allow 'doConfig()' to fill it in dynamically
    LockssRepositoryImpl.PARAM_CACHE_LOCATION + "=";


  PluginManager mgr;

  public TestPluginManagerWrapping(String msg) {
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

  WrappedPlugin getWrappedPlugin() throws Exception {
    assertTrue(WrapperState.isUsingWrapping());
    WrappedPlugin wplug = (WrappedPlugin)
        mgr.getPlugin(mockPlugKey);
    assertNotNull(wplug.getOriginal());
    return wplug;
  }

  public void testStop() throws Exception {
    doConfig();
    WrappedPlugin mpi = getWrappedPlugin();
    assertEquals(0, ((MockPlugin)mpi.getOriginal()).getStopCtr());
    mgr.stopService();
    assertEquals(1, ((MockPlugin)mpi.getOriginal()).getStopCtr());
  }

  public void testAUConfig() throws Exception {
    doConfig();
    WrappedPlugin mpi = getWrappedPlugin();
    // plugin should be registered
    assertNotNull(mpi);
    // should have been inited once
    assertEquals(1, ((MockPlugin)mpi.getOriginal()).getInitCtr());

    // get the two archival units
    WrappedArchivalUnit au1 = (WrappedArchivalUnit)mgr.getAuFromId(mauauid1);
    WrappedArchivalUnit au2 = (WrappedArchivalUnit)mgr.getAuFromId(mauauid2);

    // verify the plugin's set of all AUs is {au1, au2}
    Collection aus = mpi.getAllAUs();
    assertEquals(SetUtil.set(au1, au2), new HashSet(mgr.getAllAUs()));

    // verify au1's configuration
    assertEquals(mauauid1, au1.getAUId());
    Configuration c1 = au1.getConfiguration();
    assertEquals("val1", c1.get(MockPlugin.CONFIG_PROP_1));
    assertEquals("val2", c1.get(MockPlugin.CONFIG_PROP_2));

    // verify au1's configuration
    assertEquals(mauauid2, au2.getAUId());
    Configuration c2 = au2.getConfiguration();
    assertEquals("val1", c2.get(MockPlugin.CONFIG_PROP_1));
    assertEquals("va.l3", c2.get(MockPlugin.CONFIG_PROP_2));

    assertEquals(au1, mgr.getAuFromId(mauauid1));
  }

  public void testCreateAU() throws Exception {
    minimalConfig();
    Plugin plug = new ThrowingMockPlugin();
    String pid = plug.getPluginId();
    String key = PluginManager.pluginKeyFromId(pid);
    WrappedPlugin mpi = (WrappedPlugin)WrapperState.getWrapper(plug);
    Configuration config = ConfigurationUtil.fromArgs("a", "b");
    WrappedArchivalUnit au = (WrappedArchivalUnit)mgr.createAu(mpi, config);

    // verify put in PluginManager map
    String auid = au.getAUId();

    WrappedArchivalUnit aux = (WrappedArchivalUnit)mgr.getAuFromId(auid);
    assertSame(au, aux);

    // verify got right config
    Configuration auConfig = au.getConfiguration();
    assertEquals("b", auConfig.get("a"));
    assertEquals(1, auConfig.keySet().size());
    assertEquals(mpi, au.getPlugin());

    // verify turns RuntimeException into ArchivalUnit.ConfigurationException
    ((ThrowingMockPlugin)mpi.getOriginal()).setCfgEx(
        new ArchivalUnit.ConfigurationException("should be thrown"));
    try {
      ArchivalUnit au2 = mgr.createAu(mpi, config);
      fail("createAu should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }

    ((ThrowingMockPlugin)mpi.getOriginal()).setRtEx(
        new NullPointerException("Should be caught"));
    try {
      ArchivalUnit au2 = mgr.createAu(mpi, config);
      fail("createAu should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
      // this is what's expected
    } catch (RuntimeException e) {
      fail("createAu threw RuntimeException");
    }

  }

  public void testConfigureAU() throws Exception {
    minimalConfig();
    Plugin plug = new ThrowingMockPlugin();
    String pid = plug.getPluginId();
    String key = PluginManager.pluginKeyFromId(pid);
    mgr.ensurePluginLoaded(key);
    WrappedPlugin mpi = (WrappedPlugin)WrapperState.getWrapper(plug);
    assertNotNull(mpi);
    WrapperState.pointWrappedPlugin(mpi,pid);
    Configuration config = ConfigurationUtil.fromArgs("a", "b");
    WrappedArchivalUnit au = (WrappedArchivalUnit)mgr.createAu(mpi, config);

    String auid = au.getAUId();
    WrappedArchivalUnit aux = (WrappedArchivalUnit)mgr.getAuFromId(auid);
    assertSame(au, aux);

    // verify can reconfig
    mgr.configureAu(mpi, ConfigurationUtil.fromArgs("a", "c"), auid);
    Configuration auConfig = au.getConfiguration();
    assertEquals("c", auConfig.get("a"));
    assertEquals(1, auConfig.keySet().size());

    // verify turns RuntimeException into ArchivalUnit.ConfigurationException
    ((ThrowingMockPlugin)mpi.getOriginal()).setCfgEx(
        new ArchivalUnit.ConfigurationException("should be thrown"));
    try {
      mgr.configureAu(mpi, config, auid);
      fail("configureAu should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }

    ((ThrowingMockPlugin)mpi.getOriginal()).setRtEx(new NullPointerException("Should be caught"));
    try {
      mgr.configureAu(mpi, config, auid);
      fail("configureAu should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
      // this is what's expected
    } catch (RuntimeException e) {
      fail("createAu threw RuntimeException");
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
    public ArchivalUnit createAU(Configuration config)
	throws ArchivalUnit.ConfigurationException {
      if (rtEx != null) {
	throw rtEx;
      } else if (cfgEx != null) {
	throw cfgEx;
      } else {
	return super.createAU(config);
      }
    }
    public ArchivalUnit configureAU(Configuration config, ArchivalUnit au)
	throws ArchivalUnit.ConfigurationException {
      if (rtEx != null) {
	throw rtEx;
      } else if (cfgEx != null) {
	throw cfgEx;
      } else {
	return super.configureAU(config, au);
      }
    }
  }


  public void testFindCUS() throws Exception {
    String url = "http://foo.bar/";
    String lower = "abc";
    String upper = "xyz";

    doConfig();
    WrappedPlugin mpi = getWrappedPlugin();

    // make a PollSpec with info from a manually created CUS, which should
    // match one of the registered AUs
    WrappedCachedUrlSet protoCus = makeCUS(mpi, mauauid1, url, lower, upper);
    PollSpec ps1 = new PollSpec(protoCus);

    // verify PluginManager can make a CUS for the PollSpec
    WrappedCachedUrlSet cus = (WrappedCachedUrlSet)mgr.findCachedUrlSet(ps1);
    assertNotNull(cus);
    // verify the CUS's CUSS
    CachedUrlSetSpec cuss = cus.getSpec();
    assertEquals(url, cuss.getUrl());
    RangeCachedUrlSetSpec rcuss = (RangeCachedUrlSetSpec)cuss;
    assertEquals(lower, rcuss.getLowerBound());
    assertEquals(upper, rcuss.getUpperBound());

    assertEquals(mauauid1, cus.getArchivalUnit().getAUId());
    // can't test protoCus.getArchivalUnit() .equals( cus.getArchivalUnit() )
    // as we made a fake mock one to build PollSpec, and PluginManager will
    // have created & configured a real mock one.

    WrappedCachedUrlSet protoAuCus = makeAUCUS(mpi, mauauid1);
    PollSpec ps2 = new PollSpec(protoAuCus);

    WrappedCachedUrlSet aucus = (WrappedCachedUrlSet)mgr.findCachedUrlSet(ps2);
    assertNotNull(aucus);
    CachedUrlSetSpec aucuss = aucus.getSpec();
    assertTrue(aucuss instanceof AUCachedUrlSetSpec);
  }

  public void testFindSingleNodeCUS() throws Exception {
    String url = "http://foo.bar/";
    String lower = PollSpec.SINGLE_NODE_LWRBOUND;

    doConfig();
    WrappedPlugin mpi = getWrappedPlugin();

    // make a PollSpec with info from a manually created CUS, which should
    // match one of the registered AUs
    CachedUrlSet protoCus = makeCUS(mpi, mauauid1, url, lower, null);
    PollSpec ps1 = new PollSpec(protoCus);

    // verify PluginManager can make a CUS for the PollSpec
    WrappedCachedUrlSet cus = (WrappedCachedUrlSet)mgr.findCachedUrlSet(ps1);
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
    WrappedPlugin mpi = getWrappedPlugin();

    // get the two archival units
    WrappedArchivalUnit au1 = (WrappedArchivalUnit)mgr.getAuFromId(mauauid1);
    WrappedArchivalUnit au2 = (WrappedArchivalUnit)mgr.getAuFromId(mauauid2);
    assertNull(mgr.findMostRecentCachedUrl(url1));
    CachedUrlSetSpec cuss = new MockCachedUrlSetSpec(prefix, null);
    WrappedCachedUrlSet mcuss = (WrappedCachedUrlSet)WrapperState.getWrapper(
        new MockCachedUrlSet((MockArchivalUnit)au1.getOriginal(), cuss));
    ((MockCachedUrlSet)mcuss.getOriginal()).addUrl(
        "foo", url1, true, true, null);
    ((MockArchivalUnit)au1.getOriginal()).setAUCachedUrlSet(mcuss);
    WrappedCachedUrl cu = (WrappedCachedUrl)mgr.findMostRecentCachedUrl(url1);
    assertNotNull(cu);
    assertEquals(url1, cu.getUrl());
    assertNull(mgr.findMostRecentCachedUrl(url2));
  }

  WrappedCachedUrlSet makeCUS(Plugin plugin, String auid, String url,
			       String lower, String upper) {
    MockArchivalUnit au = new MockArchivalUnit();
    au.setAuId(auid);
    au.setPlugin(plugin);
    au.setPluginId(plugin.getPluginId());

    CachedUrlSet cus = new MockCachedUrlSet(au,
					    new RangeCachedUrlSetSpec(url,
								      lower,
								      upper));
    return (WrappedCachedUrlSet)WrapperState.getWrapper(cus);
  }

  WrappedCachedUrlSet makeAUCUS(Plugin plugin, String auid) {
    MockArchivalUnit au = new MockArchivalUnit();
    au.setAuId(auid);
    au.setPlugin(plugin);
    au.setPluginId(plugin.getPluginId());

    CachedUrlSet cus = new MockCachedUrlSet(au, new AUCachedUrlSetSpec());
    return (WrappedCachedUrlSet)WrapperState.getWrapper(cus);
  }


  public void testWrappedAU() throws Exception {
    try {
      doConfig();
      WrappedArchivalUnit wau = (WrappedArchivalUnit) mgr.getAuFromId(mauauid1);
      assertNotNull(wau);
      WrappedPlugin wplug = (WrappedPlugin)wau.getPlugin();
      MockPlugin mock = (MockPlugin)wplug.getOriginal();
      MockArchivalUnit mau = (MockArchivalUnit)wau.getOriginal();
      assertSame(mock,mau.getPlugin());
    } catch (IOException e) {
      fail(e.getMessage());
    } catch (ClassCastException e) {
      fail("WrappedArchivalUnit not found.");
    }
  }

  public void testWrapping() throws Exception {
    ConfigurationUtil.setCurrentConfigFromProps(makeMockProperties(true,true));
    WrappedPlugin wplug = (WrappedPlugin)mgr.getPlugin(MockPlugin.KEY);
    assertNotNull(wplug);
  }

  private ArchivalUnit getSingleAu(Plugin plugin) {
    Collection coll = plugin.getAllAUs();
    Iterator it = coll.iterator();
    if (it.hasNext()) {
      return (ArchivalUnit) it.next();
    } else {
      return null;
    }
  }

  private Properties makeProperties(String key, Map definingKeys,
      Map otherKeys, boolean wrapped) throws Exception {
    StringBuffer buf = new StringBuffer(PluginManager.PARAM_AU_TREE);
    buf.append('.');
    buf.append(key);
    buf.append('.');
    Iterator it = definingKeys.keySet().iterator();
    while (it.hasNext()) {
      String defKey = (String)it.next();
      buf.append(defKey);
      buf.append('~');
      buf.append((String)definingKeys.get(defKey));
      buf.append('&');
    }
    buf.delete(buf.length()-1,buf.length());
    Properties props = new Properties();
    String prefix = buf.toString() + '.';
    it = definingKeys.keySet().iterator();
    while (it.hasNext()) {
      String defKey = (String)it.next();
      props.setProperty(prefix + defKey,(String)definingKeys.get(defKey));
    }
    if (wrapped) {
      props.setProperty(prefix + "reserved.wrapper", "true");
    }
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION,
                      getTempDir().getAbsolutePath() + File.separator);
    if (otherKeys!=null) {
      it = otherKeys.keySet().iterator();
      while (it.hasNext()) {
        String defKey = (String) it.next();
        props.setProperty(prefix + defKey, (String) otherKeys.get(defKey));
      }
    }
    return props;
  }

  public static class Mock2Plugin extends MockPlugin {
     static final String KEY =
         PluginManager.pluginKeyFromName(
        "org|lockss|plugin|TestPluginManagerWrapping$Mock2Plugin");
  }

  private Properties makeMockProperties(boolean orig, boolean wrapped)
      throws Exception {
    Map map = new HashMap();
    map.put(MockPlugin.CONFIG_PROP_1, "home");
    map.put(MockPlugin.CONFIG_PROP_2, "val1");
    Map other = new HashMap();
    if (orig) {
      return makeProperties(MockPlugin.KEY, map, null, wrapped);
    } else {
      return makeProperties(Mock2Plugin.KEY,map,null,wrapped);
    }
  }

  public void testDoesntPermitUnwrappedIfWrappedExists() throws Exception {
    ConfigurationUtil.setCurrentConfigFromProps(makeMockProperties(true,true));
    WrappedPlugin wmock = (WrappedPlugin) mgr.getPlugin(MockPlugin.KEY);
    assertNotNull(wmock);
    ConfigurationUtil.setCurrentConfigFromProps(makeMockProperties(true,false));
    assertSame(wmock,mgr.getPlugin(MockPlugin.KEY));
  }

  public void testDoesntPermitWrappedIfUnwrappedExists() throws Exception {
    ConfigurationUtil.setCurrentConfigFromProps(makeMockProperties(true,false));
    MockPlugin mock = (MockPlugin)mgr.getPlugin(MockPlugin.KEY);
    assertNotNull(mock);
    ConfigurationUtil.setCurrentConfigFromProps(makeMockProperties(true,true));
    assertSame(mock,mgr.getPlugin(MockPlugin.KEY));
  }

  public void testTwoPlugins() throws Exception {
    ConfigurationUtil.setCurrentConfigFromProps(makeMockProperties(true,true));
    WrappedPlugin wmock = (WrappedPlugin) mgr.getPlugin(MockPlugin.KEY);
    assertNotNull(wmock);
    ConfigurationUtil.setCurrentConfigFromProps(makeMockProperties(false, true));
    WrappedPlugin wmock2 = (WrappedPlugin) mgr.getPlugin(Mock2Plugin.KEY);
    assertNotNull(wmock2);
    assertNotSame(wmock, wmock2);
    assertNotSame(wmock.getOriginal(), wmock2.getOriginal());
    WrappedArchivalUnit wau = (WrappedArchivalUnit) getSingleAu(wmock);
    WrappedArchivalUnit wau2 = (WrappedArchivalUnit) getSingleAu(wmock2);
    assertNotNull(wau);
    assertNotNull(wau2);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite();
    //suite.addTest(TestSuite.createTest(TestPluginManagerWrapping.class,"testWrappedAU"));
    suite.addTestSuite(TestPluginManagerWrapping.class);
    return suite;
  }


}
