/*
 * $Id: TestPluginManager.java,v 1.73 2006-09-16 22:58:33 tlipkis Exp $
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
import java.security.KeyStore;
import junit.framework.*;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.base.*;
import org.lockss.plugin.definable.*;
import org.lockss.poller.*;
import org.lockss.repository.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.repository.*;
import org.lockss.state.HistoryRepositoryImpl;

/**
 * Test class for org.lockss.plugin.PluginManager
 */
public class TestPluginManager extends LockssTestCase {
  private MyMockLockssDaemon theDaemon;

  static String mockPlugKey =
    PluginManager.pluginKeyFromName(MyMockPlugin.class.getName());
  static Properties props1 = new Properties();
  static Properties props2 = new Properties();
  static Properties props3 = new Properties();
  static {
    props1.setProperty(MockPlugin.CONFIG_PROP_1, "val1");
    props1.setProperty(MockPlugin.CONFIG_PROP_2, "val2");
    props2.setProperty(MockPlugin.CONFIG_PROP_1, "val1");
    props2.setProperty(MockPlugin.CONFIG_PROP_2, "va.l3");//auid contains a dot
    props3.setProperty(BaseArchivalUnit.NEW_CONTENT_CRAWL_KEY, "3d");
  }

  static String mauauidKey1 = PropUtil.propsToCanonicalEncodedString(props1);
  static String mauauid1 = mockPlugKey+"&"+ mauauidKey1;

  static String mauauidKey2 = PropUtil.propsToCanonicalEncodedString(props2);
  static String mauauid2 = mockPlugKey+"&"+mauauidKey2;

  static String mauauidKey3 = PropUtil.propsToCanonicalEncodedString(props3);
  static String mauauid3 = mockPlugKey+"&"+mauauidKey3;

  static String p1param =
    PluginManager.PARAM_AU_TREE + "." + mockPlugKey + ".";

  static String p1a1param = p1param + mauauidKey1 + ".";
  static String p1a2param = p1param + mauauidKey2 + ".";
  static String p1a3param = p1param + mauauidKey3 + ".";

  private String pluginJar;
  private String signAlias = "goodguy";
  private String pubKeystore = "org/lockss/test/public.keystore";
  private String password = "f00bar";

  private String tempDirPath;

  MyPluginManager mgr;

  public TestPluginManager(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();

    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    theDaemon = new MyMockLockssDaemon();
    mgr = new MyPluginManager();
    theDaemon.setPluginManager(mgr);
    theDaemon.setDaemonInited(true);

    // Prepare the loadable plugin directory property, which is
    // created by mgr.startService()
    Properties p = new Properties();
    p.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    p.setProperty(PluginManager.PARAM_PLUGIN_LOCATION, "plugins");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    mgr.setLoadablePluginsReady(true);
    mgr.initService(theDaemon);
    mgr.startService();
  }

  public void tearDown() throws Exception {
    mgr.stopService();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  private void doConfig() throws Exception {
    doConfig(new Properties());
  }

  private void doConfig(Properties p) throws Exception {
    // String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    p.setProperty(p1a1param+MockPlugin.CONFIG_PROP_1, "val1");
    p.setProperty(p1a1param+MockPlugin.CONFIG_PROP_2, "val2");
    p.setProperty(p1a2param+MockPlugin.CONFIG_PROP_1, "val1");
    p.setProperty(p1a2param+MockPlugin.CONFIG_PROP_2, "va.l3");
    p.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    p.setProperty(HistoryRepositoryImpl.PARAM_HISTORY_LOCATION, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(p);
  }

  private void minimalConfig() throws Exception {
    // String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    ConfigurationUtil.setFromArgs(LockssRepositoryImpl.PARAM_CACHE_LOCATION,
				  tempDirPath,
				  HistoryRepositoryImpl.PARAM_HISTORY_LOCATION,
				  tempDirPath);
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

  public void testGetPreferredPluginType() throws Exception {
    // Prefer XML plugins.
    ConfigurationUtil.setFromArgs(PluginManager.PARAM_PREFERRED_PLUGIN_TYPE,
				  "xml");
    assertEquals(PluginManager.PREFER_XML_PLUGIN,
		 mgr.getPreferredPluginType());

    // Prefer CLASS plugins.
    ConfigurationUtil.setFromArgs(PluginManager.PARAM_PREFERRED_PLUGIN_TYPE,
				  "class");
    assertEquals(PluginManager.PREFER_CLASS_PLUGIN,
		 mgr.getPreferredPluginType());

    // Illegal type.
    ConfigurationUtil.setFromArgs(PluginManager.PARAM_PREFERRED_PLUGIN_TYPE,
				  "foo");
    assertEquals(PluginManager.PREFER_XML_PLUGIN,
		 mgr.getPreferredPluginType());

    // No type specified.
    ConfigurationUtil.setFromArgs("foo", "bar");
    assertEquals(PluginManager.PREFER_XML_PLUGIN,
		 mgr.getPreferredPluginType());
  }

  public void testEnsurePluginLoaded() throws Exception {
    // non-existent class shouldn't load
    String key = "org|lockss|NoSuchClass";
    // OK if this logs FileNotFoundException in the log.
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

  public void testEnsurePluginLoadedCheckDaemonVersion()
      throws Exception {
    String key = PluginManager.pluginKeyFromName(VerPlugin.class.getName());
    // with insufficient daemon version,
    ConfigurationUtil.addFromArgs(ConfigManager.PARAM_DAEMON_VERSION, "1.1.1");
    // plugin requiring 1.10.0 should not load
    assertFalse(mgr.ensurePluginLoaded(key));
    // with sufficient daemon version,
    ConfigurationUtil.addFromArgs(ConfigManager.PARAM_DAEMON_VERSION, "11.1.1");
    // it should load.
    assertTrue(mgr.ensurePluginLoaded(key));
  }

  public void testInitPluginRegistry() {
    String n1 = "org.lockss.test.MockPlugin";
    String n2 = ThrowingMockPlugin.class.getName();
    assertEmpty(mgr.getRegisteredPlugins());
    Properties p = new Properties();
    p.setProperty(PluginManager.PARAM_PLUGIN_REGISTRY, n1 + ";" + n2);
    ConfigurationUtil.setCurrentConfigFromProps(p);
    Plugin p1 = mgr.getPlugin(PluginManager.pluginKeyFromName(n1));
    assertNotNull(p1);
    assertTrue(p1.toString(), p1 instanceof MockPlugin);
    Plugin p2 = mgr.getPlugin(PluginManager.pluginKeyFromName(n2));
    assertNotNull(p2);
    assertTrue(p2.toString(), p2 instanceof ThrowingMockPlugin);
    assertEquals(SetUtil.set(p1, p2),
		 SetUtil.theSet(mgr.getRegisteredPlugins()));
    p.setProperty(PluginManager.PARAM_PLUGIN_REGISTRY, n1);
    ConfigurationUtil.setCurrentConfigFromProps(p);
    assertEquals(SetUtil.set(p1, p2),
		 SetUtil.theSet(mgr.getRegisteredPlugins()));
    p.setProperty(PluginManager.PARAM_PLUGIN_REGISTRY, n1 + ";" + n2);
    ConfigurationUtil.setCurrentConfigFromProps(p);
    assertEquals(SetUtil.set(p1, p2),
		 SetUtil.theSet(mgr.getRegisteredPlugins()));
    p.setProperty(PluginManager.PARAM_PLUGIN_REGISTRY, n1 + ";" + n2);
    p.setProperty(PluginManager.PARAM_PLUGIN_RETRACT, n2);
    ConfigurationUtil.setCurrentConfigFromProps(p);
    assertEquals(SetUtil.set(p1),
		 SetUtil.theSet(mgr.getRegisteredPlugins()));
    assertNull(mgr.getPlugin(PluginManager.pluginKeyFromName(n2)));
    assertSame(p1, mgr.getPlugin(PluginManager.pluginKeyFromName(n1)));
    p.setProperty(PluginManager.PARAM_PLUGIN_REGISTRY, n1 + ";" + n2);
    p.setProperty(PluginManager.PARAM_PLUGIN_RETRACT, "");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    p2 = mgr.getPlugin(PluginManager.pluginKeyFromName(n2));
    assertNotNull(p2);
    assertTrue(p2.toString(), p2 instanceof ThrowingMockPlugin);
  }

  public void testEnsurePluginLoadedXml() throws Exception {
    String pname = "org.lockss.test.TestXmlPlugin";
    String key = PluginManager.pluginKeyFromId(pname);
    assertTrue(mgr.ensurePluginLoaded(key));
    Plugin p = mgr.getPlugin(key);
    assertTrue(p.toString(), p instanceof DefinablePlugin);
    MyMockConfigurablePlugin mcpi = (MyMockConfigurablePlugin)mgr.getPlugin(key);
    assertNotNull(mcpi);
    List initArgs = mcpi.getInitArgs();
    assertEquals(1, initArgs.size());
    List args = (List)initArgs.get(0);
    assertEquals(3, args.size());
    assertEquals(pname, args.get(1));
  }

  public void testEnsurePluginLoadedXmlCheckDaemonVersion()
      throws Exception {
    // with insufficient daemon version,
    ConfigurationUtil.addFromArgs(ConfigManager.PARAM_DAEMON_VERSION, "1.1.1");
    // plugin requiring 1.10.0 should not load
    String pname = "org.lockss.test.TestXmlPlugin";
    String key = PluginManager.pluginKeyFromId(pname);
    assertFalse(mgr.ensurePluginLoaded(key));
    // with sufficient daemon version,
    ConfigurationUtil.addFromArgs(ConfigManager.PARAM_DAEMON_VERSION, "11.1.1");
    // it should load.
    assertTrue(mgr.ensurePluginLoaded(key));
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

  public void testAuConfigWithGlobalEntryForNonExistentAU() throws Exception {
    Properties p = new Properties();
    p.setProperty(p1a3param+BaseArchivalUnit.NEW_CONTENT_CRAWL_KEY, "2d");
    doConfig(p);
    MockPlugin mpi = (MockPlugin)mgr.getPlugin(mockPlugKey);
    // plugin should be registered
    assertNotNull(mpi);
    // should have been inited once
    assertEquals(1, mpi.getInitCtr());

    // get the two archival units
    ArchivalUnit au1 = mgr.getAuFromId(mauauid1);
    ArchivalUnit au2 = mgr.getAuFromId(mauauid2);

    assertNotNull("AU1 didn't get created", au1);
    assertNotNull("AU2 didn't get created", au2);

    // verify the plugin's set of all AUs is {au1, au2}
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

  List createEvents = new ArrayList();
  List deleteEvents = new ArrayList();
  List reconfigEvents = new ArrayList();

  class MyAuEventHandler extends AuEventHandler.Base {
    public void auCreated(ArchivalUnit au) {
      createEvents.add(au);
    }
    public void auDeleted(ArchivalUnit au) {
      deleteEvents.add(au);
    }
    public void auReconfigured(ArchivalUnit au, Configuration oldAuConf) {
      reconfigEvents.add(ListUtil.list(au, oldAuConf));
    }
  }

  public void testCreateAu() throws Exception {
    minimalConfig();
    String pid = new ThrowingMockPlugin().getPluginId();
    String key = PluginManager.pluginKeyFromId(pid);
    assertTrue(mgr.ensurePluginLoaded(key));
    ThrowingMockPlugin mpi = (ThrowingMockPlugin)mgr.getPlugin(key);
    assertNotNull(mpi);
    mgr.registerAuEventHandler(new MyAuEventHandler());
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

    // verify event handler run
    assertEquals(ListUtil.list(au), createEvents);
    assertEmpty(deleteEvents);
    assertEmpty(reconfigEvents);

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
      fail("createAU threw RuntimeException", e);
    }

  }

  public void testConfigureAu() throws Exception {
    minimalConfig();
    String pid = new ThrowingMockPlugin().getPluginId();
    String key = PluginManager.pluginKeyFromId(pid);
    assertTrue(mgr.ensurePluginLoaded(key));
    ThrowingMockPlugin mpi = (ThrowingMockPlugin)mgr.getPlugin(key);
    assertNotNull(mpi);
    mgr.registerAuEventHandler(new MyAuEventHandler());
    Configuration config = ConfigurationUtil.fromArgs("a", "b");
    ArchivalUnit au = mgr.createAu(mpi, config);

    String auid = au.getAuId();
    ArchivalUnit aux = mgr.getAuFromId(auid);
    assertSame(au, aux);

    // verify event handler run
    assertEquals(ListUtil.list(au), createEvents);
    assertEmpty(deleteEvents);
    assertEmpty(reconfigEvents);

    // verify can reconfig
    mgr.configureAu(mpi, ConfigurationUtil.fromArgs("a", "c"), auid);
    Configuration auConfig = au.getConfiguration();
    assertEquals("c", auConfig.get("a"));
    assertEquals(1, auConfig.keySet().size());

    // verify event handler run
    assertEquals(ListUtil.list(au), createEvents);
    assertEmpty(deleteEvents);
    assertEquals(ListUtil.list(ListUtil.list(au, config)), reconfigEvents);

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
      fail("createAU threw RuntimeException", e);
    }

  }

  public void testConfigureAuWithBogusAuid() throws Exception {
    minimalConfig();
    String pid = new ThrowingMockPlugin().getPluginId();
    String key = PluginManager.pluginKeyFromId(pid);
    assertTrue(mgr.ensurePluginLoaded(key));
    ThrowingMockPlugin mpi = (ThrowingMockPlugin)mgr.getPlugin(key);
    assertNotNull(mpi);
    // verify doesn't get created if auid doesn't match config
    try {
      mgr.configureAu(mpi, ConfigurationUtil.fromArgs("a", "c"), "bogos_auid");
      fail("Should have thrown ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
      log.debug(e.toString());
    }
    assertEmpty(theDaemon.getAuMgrsStarted());
  }

  public void testDeactivateAu() throws Exception {
    minimalConfig();
    String pid = new ThrowingMockPlugin().getPluginId();
    String key = PluginManager.pluginKeyFromId(pid);

    assertTrue(mgr.ensurePluginLoaded(key));
    ThrowingMockPlugin mpi = (ThrowingMockPlugin)mgr.getPlugin(key);
    assertNotNull(mpi);
    Configuration config = ConfigurationUtil.fromArgs("a", "b");
    ArchivalUnit au = mgr.createAu(mpi, config);
    assertNotNull(au);
    String auId = au.getAuId();
    mgr.configureAu(mpi, config, auId);

    // should not throw.
    try {
      assertFalse(mgr.getInactiveAuIds().contains(auId));
      mgr.registerAuEventHandler(new MyAuEventHandler());
      mgr.deactivateAu(au);
      assertTrue(mgr.getInactiveAuIds().contains(auId));
      // verify event handler run
      assertEmpty(createEvents);
      assertEquals(ListUtil.list(au), deleteEvents);
      assertEmpty(reconfigEvents);
    } catch (Exception ex) {
      fail("Deactivating au should not have thrown", ex);
    }

  }

  public void testCreateAndSaveAndDeleteAuConfiguration() throws Exception {
    minimalConfig();
    String pid = new ThrowingMockPlugin().getPluginId();
    String key = PluginManager.pluginKeyFromId(pid);

    assertTrue(mgr.ensurePluginLoaded(key));
    ThrowingMockPlugin mpi = (ThrowingMockPlugin)mgr.getPlugin(key);
    assertNotNull(mpi);

    Properties props = new Properties();
    props.put("a", "b");

    // Test creating and deleting by au reference.
    ArchivalUnit au1 = mgr.createAndSaveAuConfiguration(mpi, props);
    assertNotNull(au1);
    try {
      mgr.deleteAuConfiguration(au1);
    } catch (Exception e) {
      fail("Deleting au config by AU reference should not have thrown", e);
    }

    // Test creating and deleting by au ID.
    ArchivalUnit au2 = mgr.createAndSaveAuConfiguration(mpi, props);
    assertNotNull(au2);
    try {
      mgr.deleteAuConfiguration(au2.getAuId());
    } catch (Exception e) {
      fail("Deleting au config by AU ID should not have thrown", e);
    }

    // Test setAndSaveAuConfiguration
    ArchivalUnit au3 = mgr.createAu(mpi,
				    ConfigurationUtil.fromArgs("foo", "bar"));
    try {
      mgr.setAndSaveAuConfiguration(au3, props);

      mgr.deleteAu(au3);
    } catch (Exception e) {
      fail("Deleting AU should not have thrown", e);
    }
  }

  // ensure getAllAus() returns AUs in title sorted order
  public void testGetAllAus() throws Exception {
    MockArchivalUnit mau1 = new MockArchivalUnit();
    MockArchivalUnit mau2 = new MockArchivalUnit();
    MockArchivalUnit mau3 = new MockArchivalUnit();
    MockArchivalUnit mau4 = new MockArchivalUnit();
    MockArchivalUnit mau5 = new MockArchivalUnit();
    // make sure it sorts by name, not auid
    mau1.setName("foobarm1"); mau1.setAuId("5");
    mau2.setName("foom2"); mau2.setAuId("3");
    mau3.setName("gunt"); mau3.setAuId("4");
    mau4.setName("m4"); mau4.setAuId("1");
    mau5.setName("zzyzx"); mau5.setAuId("2");
    mgr.putAuInMap(mau5);
    mgr.putAuInMap(mau4);
    mgr.putAuInMap(mau2);
    mgr.putAuInMap(mau3);
    mgr.putAuInMap(mau1);
    assertEquals(ListUtil.list(mau1, mau2, mau3, mau4, mau5), mgr.getAllAus());
  }

  public void testgetRandomizedAus() throws Exception {
    MockArchivalUnit mau1 = new MockArchivalUnit();
    MockArchivalUnit mau2 = new MockArchivalUnit();
    MockArchivalUnit mau3 = new MockArchivalUnit();
    MockArchivalUnit mau4 = new MockArchivalUnit();
    MockArchivalUnit mau5 = new MockArchivalUnit();
    mgr.putAuInMap(mau5);
    mgr.putAuInMap(mau4);
    mgr.putAuInMap(mau2);
    mgr.putAuInMap(mau3);
    mgr.putAuInMap(mau1);
    Set aus = SetUtil.theSet(mgr.getRandomizedAus());
    assertEquals(SetUtil.set(mau1, mau2, mau3, mau4, mau5), aus);
  }

  public void testTitleSets() throws Exception {
    String ts1p = PluginManager.PARAM_TITLE_SETS + ".s1.";
    String ts2p = PluginManager.PARAM_TITLE_SETS + ".s2.";
    String title1 = "Title Set 1";
    String title2 = "Set of Titles";
    String path1 = "[journalTitle='Dog Journal']";
    String path2 = "[journalTitle=\"Dog Journal\" or pluginName=\"plug2\"]";
    Properties p = new Properties();
    p.setProperty(ts1p+"class", "xpath");
    p.setProperty(ts1p+"name", title1);
    p.setProperty(ts1p+"xpath", path1);
    p.setProperty(ts2p+"class", "xpath");
    p.setProperty(ts2p+"name", title2);
    p.setProperty(ts2p+"xpath", path2);
    ConfigurationUtil.setCurrentConfigFromProps(p);
    Map map = mgr.getTitleSetMap();
    assertEquals(2, map.size());
    assertEquals(new TitleSetXpath(theDaemon, title1, path1), map.get(title1));
    assertEquals(new TitleSetXpath(theDaemon, title2, path2), map.get(title2));
  }

  public void testIllTitleSets() throws Exception {
    String ts1p = PluginManager.PARAM_TITLE_SETS + ".s1.";
    String ts2p = PluginManager.PARAM_TITLE_SETS + ".s2.";
    String title1 = "Title Set 1";
    String title2 = "Set of Titles";
    // illegal xpath
    String path1 = "[journalTitle='Dog Journal']]";
    String path2 = "[journalTitle=\"Dog Journal\" or pluginName=\"plug2\"]";
    Properties p = new Properties();
    p.setProperty(ts1p+"class", "xpath");
    p.setProperty(ts1p+"name", title1);
    p.setProperty(ts1p+"xpath", path1);
    p.setProperty(ts2p+"class", "xpath");
    p.setProperty(ts2p+"name", title2);
    p.setProperty(ts2p+"xpath", path2);
    ConfigurationUtil.setCurrentConfigFromProps(p);
    Map map = mgr.getTitleSetMap();
    assertEquals(1, map.size());
    assertEquals(new TitleSetXpath(theDaemon, title2, path2), map.get(title2));
  }

  static class MyMockLockssDaemon extends MockLockssDaemon {
    List auMgrsStarted = new ArrayList();

    public void startOrReconfigureAuManagers(ArchivalUnit au,
					     Configuration auConfig)
	throws Exception {
      auMgrsStarted.add(au);
    }
    List getAuMgrsStarted() {
      return auMgrsStarted;
    }

    // For testLoadLoadablePlugins -- need a mock repository
    public LockssRepository getLockssRepository(ArchivalUnit au) {
      return new MyMockLockssRepository();
    }
  }

  static class MyMockLockssRepository extends MockLockssRepository {
    public RepositoryNode getNode(String url) {
      return new MyMockRepositoryNode();
    }
  }

  static class MyMockRepositoryNode extends MockRepositoryNode {
    public int getCurrentVersion() {
      return 1;
    }
  }

  static class MyPluginManager extends PluginManager {
    private ArchivalUnit processOneRegistryAuThrowIf = null;
    private String processOneRegistryJarThrowIf = null;

    protected String getConfigurablePluginName() {
      return MyMockConfigurablePlugin.class.getName();
    }
    protected void processOneRegistryAu(ArchivalUnit au, Map tmpMap) {
      if (au == processOneRegistryAuThrowIf) {
	throw new ExpectedRuntimeException("fake error for " + au);
      }
      super.processOneRegistryAu(au, tmpMap);
    }
    protected void processOneRegistryJar(CachedUrl cu, String url,
					 ArchivalUnit au, Map tmpMap) {
      if (url.equals(processOneRegistryJarThrowIf)) {
	throw new ExpectedRuntimeException("fake error for " + url);
      }
      super.processOneRegistryJar(cu, url, au, tmpMap);
    }
    void processOneRegistryAuThrowIf(ArchivalUnit au) {
      processOneRegistryAuThrowIf = au;
    }
    void processOneRegistryJarThrowIf(String url) {
      processOneRegistryJarThrowIf = url;
    }

    protected void possiblyStartRegistryAuCrawl(ArchivalUnit au,
						String url,
						PluginManager.InitialRegistryCallback cb) {
      cb.crawlCompleted(url);
    }
  }

  static class MyMockConfigurablePlugin extends DefinablePlugin {
    private List initArgs = new ArrayList();

    public void initPlugin(LockssDaemon daemon, String extMapName, ClassLoader loader)
	throws FileNotFoundException {
      initArgs.add(ListUtil.list(daemon, extMapName, loader));
      super.initPlugin(daemon, extMapName, loader);
    }

    List getInitArgs() {
      return initArgs;
    }
  }

  static class ThrowingMockPlugin extends MockPlugin {
    RuntimeException rtEx;
    ArchivalUnit.ConfigurationException cfgEx;

    public void setRtEx(RuntimeException rtEx) {
      this.rtEx = rtEx;
    }
    public void setCfgEx(ArchivalUnit.ConfigurationException cfgEx) {
      this.cfgEx = cfgEx;
    }
    public ArchivalUnit createAu0(Configuration config)
	throws ArchivalUnit.ConfigurationException {
      if (rtEx != null) {
	throw rtEx;
      } else if (cfgEx != null) {
	throw cfgEx;
      } else {
	return super.createAu0(config);
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
    PollSpec ps1 = new PollSpec(protoCus, Poll.V1_CONTENT_POLL);

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
    PollSpec ps2 = new PollSpec(protoAuCus, Poll.V1_CONTENT_POLL);

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
    PollSpec ps1 = new PollSpec(protoCus, Poll.V1_CONTENT_POLL);

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
    String url1a = "http://foo.bar:80/baz";
    String url1b = "http://FOO.BAR:80/baz";
    String url2 = "http://foo.bar/not";
    doConfig();
    MockPlugin mpi = (MockPlugin)mgr.getPlugin(mockPlugKey);

    // get the two archival units
    MockArchivalUnit au1 = (MockArchivalUnit)mgr.getAuFromId(mauauid1);
//     ArchivalUnit au2 = mgr.getAuFromId(mauauid2);
    assertNull(mgr.findMostRecentCachedUrl(url1));
    CachedUrlSetSpec cuss = new MockCachedUrlSetSpec(prefix, null);
    MockCachedUrlSet mcuss = new MockCachedUrlSet(au1, cuss);
    au1.addUrl(url1, true, true, null);
    au1.setAuCachedUrlSet(mcuss);
    CachedUrl cu = mgr.findMostRecentCachedUrl(url1);
    assertNotNull(cu);
    assertEquals(url1, cu.getUrl());
    cu = mgr.findMostRecentCachedUrl(url1a);
    assertNotNull(cu);
    assertEquals(url1, cu.getUrl());
    cu = mgr.findMostRecentCachedUrl(url1b);
    assertNotNull(cu);
    assertEquals(url1, cu.getUrl());
    assertNull(mgr.findMostRecentCachedUrl(url2));
  }

  public void testFindMostRecentCachedUrlWithNormalization()
      throws Exception {
    final String prefix = "http://foo.bar/"; // pseudo crawl rule prefix
    String url0 = "http://foo.bar/xxx/baz"; // normal form of test url
    String url1 = "http://foo.bar/SESSION/xxx/baz"; // should normalize to url0
    String url1a = "http://foo.bar:80/SESSION/xxx/baz"; // same
    String url1b = "http://FOO.BAR/SESSION/xxx/baz"; // same
    String url2 = "http://foo.bar/SESSION/not";	// should not
    // manually create necessary pieces, no config
    MockPlugin mpi = new MockPlugin();
    MockArchivalUnit mau = new MyMockArchivalUnit() {
	// shouldBeCached() is true of anything starting with prefix
	public boolean shouldBeCached(String url) {
	  return StringUtil.startsWithIgnoreCase(url, prefix);
	}
	// siteNormalizeUrl() removes "SESSION/" from url
	public String siteNormalizeUrl(String url) {
	  return StringUtil.replaceString(url, "SESSION/", "");
	}
      };
    mau.setPlugin(mpi);
    mau.setAuId("mauauidddd");
    mgr.putAuInMap(mau);
    // neither url is found
    assertNull(mgr.findMostRecentCachedUrl(url1));
    assertNull(mgr.findMostRecentCachedUrl(url2));
    // create mock structure so that url0 exists with content
    CachedUrlSetSpec cuss = new MockCachedUrlSetSpec(prefix, null);
    MockCachedUrlSet mcuss = new MockCachedUrlSet(mau, cuss);
    mau.addUrl(url0, true, true, null);
    mau.setAuCachedUrlSet(mcuss);
    // url1 should now be found, as url0
    CachedUrl cu = mgr.findMostRecentCachedUrl(url1);
    assertNotNull(cu);
    assertEquals(url0, cu.getUrl());
    cu = mgr.findMostRecentCachedUrl(url1a);
    assertNotNull(cu);
    assertEquals(url0, cu.getUrl());
    cu = mgr.findMostRecentCachedUrl(url1b);
    assertNotNull(cu);
    assertEquals(url0, cu.getUrl());
    // url2 still not found
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

  private KeyStore getKeystoreResource(String name, String pass)
      throws Exception {
    KeyStore ks = KeyStore.getInstance("JKS", "SUN");
    ks.load(ClassLoader.getSystemClassLoader().
	    getResourceAsStream(name), pass.toCharArray());
    return ks;
  }
  
  private File copyKeystoreFileToKnownLocation(String source) throws Exception {
    File keystoreFile = new File(tempDirPath, "copied.keystore");
    InputStream in = ClassLoader.getSystemClassLoader().
                     getResourceAsStream(source);
    StreamUtil.copy(in, new FileOutputStream(keystoreFile)); 
    return keystoreFile;
  }
  
  public void testInitKeystoreAsFile() throws Exception {
    minimalConfig();
    File keystoreFile =
      copyKeystoreFileToKnownLocation("org/lockss/test/public.keystore");
    KeyStore ks = mgr.initKeystore(keystoreFile.getAbsolutePath(),
                                   "f00bar");
    assertNotNull(ks);
    assertTrue(ks.containsAlias("goodguy"));
  }
  
  public void testInitKeystoreAsFileBadPasswordFails() throws Exception {
    minimalConfig();
    File keystoreFile =
      copyKeystoreFileToKnownLocation("org/lockss/test/public.keystore");
    KeyStore ks = mgr.initKeystore(keystoreFile.getAbsolutePath(),
                                   "f00barrrr"); // bad password
    assertNull(ks);
  }
  
  public void testInitKeystoreAsResource() throws Exception {
    minimalConfig();
    KeyStore ks = mgr.initKeystore("org/lockss/test/public.keystore",
                                   "f00bar");
    assertNotNull(ks);
    assertTrue(ks.containsAlias("goodguy"));
  }
  
  public void testInitKeystoreAsResourceBadPasswordFails() throws Exception {
    minimalConfig();
    KeyStore ks = mgr.initKeystore("org/lockss/test/public.keystore",
                                   "f00barrrrr");
    assertNull(ks);
  }
  
  public void testInitKeystoreAsURL() throws Exception {
    minimalConfig();
    File keystoreFile =
      copyKeystoreFileToKnownLocation("org/lockss/test/public.keystore");
    String keystoreUrl = "file://" + keystoreFile.getAbsolutePath();
    KeyStore ks = mgr.initKeystore(keystoreUrl, "f00bar");
    assertNotNull(ks);
    assertTrue(ks.containsAlias("goodguy"));
  }

  public void testInitKeystoreAsURLBadPasswordFails() throws Exception {
    minimalConfig();
    File keystoreFile =
      copyKeystoreFileToKnownLocation("org/lockss/test/public.keystore");
    String keystoreUrl = "file://" + keystoreFile.getAbsolutePath();
    KeyStore ks = mgr.initKeystore(keystoreUrl, "f00barrrrr");
    assertNull(ks);
  }
  
  public void testEmptyInitialRegistryCallback() throws Exception {
    BinarySemaphore bs = new BinarySemaphore();
    PluginManager.InitialRegistryCallback cb =
      new PluginManager.InitialRegistryCallback(Collections.EMPTY_LIST, bs);
    assertTrue(bs.take(Deadline.in(0)));
  }

  public void testInitialRegistryCallback() throws Exception {
    BinarySemaphore bs = new BinarySemaphore();
    PluginManager.InitialRegistryCallback cb =
      new PluginManager.InitialRegistryCallback(ListUtil.list("foo", "bar"),
						bs);
    assertFalse(bs.take(Deadline.in(0)));
    cb.crawlCompleted("foo");
    cb.crawlCompleted("bletch");
    assertFalse(bs.take(Deadline.in(0)));
    cb.crawlCompleted("bar");
    assertTrue(bs.take(Deadline.in(0)));
  }

  private void prepareLoadablePluginTests(Properties p) throws Exception {
    pluginJar = "org/lockss/test/good-plugin.jar";
    if (p == null) {
      p = new Properties();
    }
    p.setProperty(PluginManager.PARAM_KEYSTORE_LOCATION,
		  pubKeystore);
    p.setProperty(PluginManager.PARAM_KEYSTORE_PASSWORD,
		  password);
    p.setProperty(PluginManager.PARAM_PLUGIN_REGISTRIES,
		  "");
    ConfigurationUtil.setCurrentConfigFromProps(p);
  }


  public void testInitLoadablePluginRegistries() throws Exception {
    Properties p = new Properties();
    prepareLoadablePluginTests(p);
    List urls = ListUtil.list("http://plug1.example.com/blueplugs/",
			      "http://plug1.example.com/redplugs/");
    List urls2 = ListUtil.list("http://plug2.example.com/blueplugs/");
    assertEmpty(mgr.getAllRegistryAus());
    assertEmpty(mgr.getAllAus());
    mgr.initLoadablePluginRegistries(urls);
    assertEquals(SetUtil.theSet(mgr.getAllAus()),
		 SetUtil.theSet(mgr.getAllRegistryAus()));
    assertEquals(2, mgr.getAllRegistryAus().size());
    // ensure that a second call to initLoadablePluginRegistries() adds to
    // the same list of AUs (i.e., uses the same plugin)
    mgr.initLoadablePluginRegistries(urls2);
    assertEquals(SetUtil.theSet(mgr.getAllAus()),
		 SetUtil.theSet(mgr.getAllRegistryAus()));
    assertEquals(3, mgr.getAllRegistryAus().size());
  }


  /** Test loading a loadable plugin. */
  public void testLoadLoadablePlugin(boolean preferLoadable) throws Exception {
    Properties p = new Properties();
    p.setProperty(PluginManager.PARAM_PREFER_LOADABLE_PLUGIN,
		  "" + preferLoadable);
    prepareLoadablePluginTests(p);
    String pluginKey = "org|lockss|test|MockConfigurablePlugin";
    // Set up a MyMockRegistryArchivalUnit with the right data.
    List plugins =
      ListUtil.list(pluginJar);
    MyMockRegistryArchivalUnit mmau = new MyMockRegistryArchivalUnit(plugins);
    List registryAus = ListUtil.list(mmau);
    assertNull(mgr.getPlugin(pluginKey));
    mgr.processRegistryAus(registryAus);
    Plugin mockPlugin = mgr.getPlugin(pluginKey);
    assertNotNull(mockPlugin);
    assertEquals("1", mockPlugin.getVersion());
    PluginManager.PluginInfo info = mgr.getLoadablePluginInfo(mockPlugin);
    assertEquals(mmau.getNthUrl(1), info.getCuUrl());
    log.debug("isLoadable: " + info.isOnLoadablePath());
    assertEquals(preferLoadable, info.isOnLoadablePath());
    assertSame(mockPlugin, info.getPlugin());
  }

  /** Load a loadable plugin, preferring the loadable version. */
  public void testLoadLoadablePluginPreferLoadable() throws Exception {
    testLoadLoadablePlugin(true);
  }

  /** Load a loadable plugin, preferring the library jar version. */
  public void testLoadLoadablePluginPreferLibJar() throws Exception {
    testLoadLoadablePlugin(false);
  }

  /** Runtime errors loading plugins should be caught. */
  public void testErrorProcessingRegistryAu() throws Exception {
    String badplug = "org/lockss/test/bad-plugin.jar";
    Properties p = new Properties();
    p.setProperty(PluginManager.PARAM_PREFER_LOADABLE_PLUGIN, "true");
    prepareLoadablePluginTests(p);
    String pluginKey = "org|lockss|test|MockConfigurablePlugin";
    // Set up a MyMockRegistryArchivalUnit with the right data.
    MyMockRegistryArchivalUnit mmau1 =
      new MyMockRegistryArchivalUnit(ListUtil.list(badplug));
    MyMockRegistryArchivalUnit mmau2 =
      new MyMockRegistryArchivalUnit(ListUtil.list(badplug, pluginJar));
    // Make processOneRegistryAu throw on the first au
    mgr.processOneRegistryAuThrowIf(mmau1);
    // Make processOneRegistryJar throw on the first jar in the second au
    mgr.processOneRegistryJarThrowIf(mmau2.getNthUrl(1));
    assertNull(mgr.getPlugin(pluginKey));
    mgr.processRegistryAus(ListUtil.list(mmau1, mmau2));
    // ensure that the one plugin was still loaded
    Plugin mockPlugin = mgr.getPlugin(pluginKey);
    assertNotNull(mockPlugin);
    assertEquals("1", mockPlugin.getVersion());
    PluginManager.PluginInfo info = mgr.getLoadablePluginInfo(mockPlugin);
    assertEquals(mmau2.getNthUrl(2), info.getCuUrl());
    assertTrue(info.isOnLoadablePath());
    assertSame(mockPlugin, info.getPlugin());
  }

  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(TestPluginManager.class);
    return suite;
  }

  /**
   * a mock Archival Unit used for testing loadable plugin loading.
   */
  private static class MyMockRegistryArchivalUnit extends MockArchivalUnit {
    private MyMockRegistryCachedUrlSet cus;

    public MyMockRegistryArchivalUnit(List jarFiles) {
      super((Plugin)null);
      cus = new MyMockRegistryCachedUrlSet();
      int n = 0;
      for (Iterator iter = jarFiles.iterator(); iter.hasNext(); ) {
	n++;
	cus.addCu(new MockCachedUrl(getNthUrl(n),
				    (String)iter.next(), true));
      }
    }

    public String getNthUrl(int n) {
      return "http://foo.bar/test" + n + ".jar";
    }

    public CachedUrlSet getAuCachedUrlSet() {
      return cus;
    }
  }

  private static class MyMockArchivalUnit extends MockArchivalUnit {
    public Collection getUrlStems() {
      return ListUtil.list("http://foo.bar/");
    }
  }

  private static class MyMockPlugin extends MockPlugin {
    public MyMockPlugin(){
      super();
    }

    protected MockArchivalUnit newMockArchivalUnit() {
      return new MyMockArchivalUnit();
    }
  }

  private static class VerPlugin extends MockPlugin {
    public VerPlugin(){
      super();
    }

    public String getRequiredDaemonVersion() {
      return "1.10.0";
    }
  }

  /**
   * a mock CachedUrlSet used for testing loadable plugin loading.
   */
  private static class MyMockRegistryCachedUrlSet extends MockCachedUrlSet {
    List cuList;

    public MyMockRegistryCachedUrlSet() {
      cuList = new ArrayList();
    }

    public void addCu(MockCachedUrl cu) {
      cuList.add(cu);
    }

    public Iterator contentHashIterator() {
      return cuList.iterator();
    }
  }
}
