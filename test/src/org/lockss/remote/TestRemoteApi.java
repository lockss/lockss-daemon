/*
 * $Id: TestRemoteApi.java,v 1.6 2005-01-04 03:01:24 tlipkis Exp $
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

package org.lockss.remote;

import java.io.*;
import java.util.*;
import junit.framework.*;

import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.repository.*;

/**
 * Test class for org.lockss.remote.RemoteApi
 */
public class TestRemoteApi extends LockssTestCase {

  static final String AUID1 = "AUID_1";
  static final String PID1 = "PID_1";

  MockLockssDaemon daemon;
  MyMockPluginManager mpm;
  RemoteApi rapi;

  public void setUp() throws Exception {
    super.setUp();

    daemon = getMockLockssDaemon();
    mpm = new MyMockPluginManager();
    mpm.mockInit();
    daemon.setPluginManager(mpm);
    rapi = new RemoteApi();
    daemon.setRemoteApi(rapi);
    daemon.setDaemonInited(true);
    rapi.initService(daemon);
    rapi.startService();
  }

  public void tearDown() throws Exception {
    rapi.stopService();
    daemon.stopDaemon();
    super.tearDown();
  }

  public void testFindAuProxy() {
    MockArchivalUnit mau1 = new MockArchivalUnit();
    AuProxy aup = rapi.findAuProxy(mau1);
    assertNotNull(aup);
    assertSame(mau1, aup.getAu());
    assertSame(aup, rapi.findAuProxy(mau1));
    ArchivalUnit mau2 = mpm.getAuFromId(AUID1);
    assertNotNull(mau2);
    AuProxy aup2b = rapi.findAuProxy(mau2);
    AuProxy aup2a = rapi.findAuProxy(AUID1);
    assertNotNull(aup2a);
    assertSame(aup2a, aup2b);
  }

  public void testMapAus() {
    MockArchivalUnit mau1 = new MockArchivalUnit();
    ArchivalUnit mau2 = mpm.getAuFromId(AUID1);
    List mapped = rapi.mapAusToProxies(ListUtil.list(mau1, mau2));
    assertEquals(2, mapped.size());
    assertNotNull(mapped.get(0));
    assertNotNull(mapped.get(1));
    assertSame(rapi.findAuProxy(mau1), (AuProxy)mapped.get(0));
    assertSame(rapi.findAuProxy(mau2), (AuProxy)mapped.get(1));
  }

  public void testFindPluginProxy() throws Exception {
    MockPlugin mp1 = new MockPlugin();
    PluginProxy pp1 = rapi.findPluginProxy(mp1);
    assertNotNull(pp1);
    assertSame(mp1, pp1.getPlugin());
    assertSame(pp1, rapi.findPluginProxy(mp1));
    Plugin mp2 = mpm.getPlugin(mpm.pluginKeyFromId(PID1));
    assertNotNull(mp2);
    PluginProxy pp2b = rapi.findPluginProxy(mp2);
    PluginProxy pp2a = rapi.findPluginProxy(PID1);
    assertNotNull(pp2a);
    assertSame(pp2a, pp2b);
  }

  public void testMapPlugins() {
    MockPlugin mp1 = new MockPlugin();
    Plugin mp2 = mpm.getPlugin(mpm.pluginKeyFromId(PID1));
    List mapped = rapi.mapPluginsToProxies(ListUtil.list(mp1, mp2));
    assertEquals(2, mapped.size());
    assertNotNull(mapped.get(0));
    assertNotNull(mapped.get(1));
    assertSame(rapi.findPluginProxy(mp1), (PluginProxy)mapped.get(0));
    assertSame(rapi.findPluginProxy(mp2), (PluginProxy)mapped.get(1));
  }

  public void testCreateAndSaveAuConfiguration() throws Exception {
    ConfigParamDescr d1 = ConfigParamDescr.BASE_URL;
    MockPlugin mp1 = new MockPlugin();
    mp1.setAuConfigDescrs(ListUtil.list(d1));

    PluginProxy pp1 = rapi.findPluginProxy(mp1);
    Configuration config = ConfigurationUtil.fromArgs(d1.getKey(), "v1");
    AuProxy aup = rapi.createAndSaveAuConfiguration(pp1, config);
    Pair pair = (Pair)mpm.actions.get(0);
    assertEquals(mp1, pair.one);
    assertEquals(config, pair.two);
    assertEquals(pp1, aup.getPlugin());
    assertEquals(config, aup.getConfiguration());
  }

  public void testSetAndSaveAuConfiguration() throws Exception {
    ConfigParamDescr d1 = ConfigParamDescr.BASE_URL;
    MockPlugin mp1 = new MockPlugin();
    mp1.setAuConfigDescrs(ListUtil.list(d1));
    MockArchivalUnit mau1 = new MockArchivalUnit();
    AuProxy aup = rapi.findAuProxy(mau1);

    PluginProxy pp1 = rapi.findPluginProxy(mp1);
    Configuration config = ConfigurationUtil.fromArgs(d1.getKey(), "v1");
    rapi.setAndSaveAuConfiguration(aup, config);
    Pair pair = (Pair)mpm.actions.get(0);
    assertEquals(mau1, pair.one);
    assertEquals(config, pair.two);
  }

  public void testDeleteAu() throws Exception {
    MockArchivalUnit mau1 = new MockArchivalUnit();
    AuProxy aup = rapi.findAuProxy(mau1);
    rapi.deleteAu(aup);
    Pair pair = (Pair)mpm.actions.get(0);
    assertEquals("Delete", pair.one);
    assertEquals(mau1, pair.two);
  }

  public void testDeactivateAu() throws Exception {
    MockArchivalUnit mau1 = new MockArchivalUnit();
    AuProxy aup = rapi.findAuProxy(mau1);
    rapi.deactivateAu(aup);
    Pair pair = (Pair)mpm.actions.get(0);
    assertEquals("Deactivate", pair.one);
    assertEquals(mau1, pair.two);
  }

  public void testGetStoredAuConfiguration() throws Exception {
    MockArchivalUnit mau1 = new MockArchivalUnit();
    String id = "auid3";
    Configuration config = ConfigurationUtil.fromArgs("k1", "v1");
    mau1.setAuId(id);
    mpm.setStoredConfig(id, config);
    AuProxy aup = rapi.findAuProxy(mau1);
    assertEquals(config, rapi.getStoredAuConfiguration(aup));
  }

  public void testGetCurrentAuConfiguration() throws Exception {
    MockArchivalUnit mau1 = new MockArchivalUnit();
    String id = "auid3";
    Configuration config = ConfigurationUtil.fromArgs("k1", "v1");
    mau1.setAuId(id);
    mpm.setCurrentConfig(id, config);
    AuProxy aup = rapi.findAuProxy(mau1);
    assertEquals(config, rapi.getCurrentAuConfiguration(aup));
  }

  public void testGetAllAus() throws Exception {
    MockArchivalUnit mau1 = new MockArchivalUnit();
    MockArchivalUnit mau2 = new MockArchivalUnit();
    mpm.setAllAus(ListUtil.list(mau1, mau2));
    assertEquals(ListUtil.list(rapi.findAuProxy(mau1), rapi.findAuProxy(mau2)),
		 rapi.getAllAus());
  }

  public void testGetInactiveAus() throws Exception {
    String id1 = "xxx1";
    String id2 = "xxx2";
    mpm.setStoredConfig(id1, ConfigManager.EMPTY_CONFIGURATION);
    mpm.setStoredConfig(id2, ConfigManager.EMPTY_CONFIGURATION);
    mpm.setInactiveAuIds(ListUtil.list(id1, id2));
    assertEquals(ListUtil.list(rapi.findInactiveAuProxy(id1),
			       rapi.findInactiveAuProxy(id2)),
		 rapi.getInactiveAus());
  }

  public void testGetRepositoryDF () throws Exception {
    PlatformInfo.DF df = rapi.getRepositoryDF("local:.");
    assertNotNull(df);
  }

  void writeAuConfigFile(String s) throws IOException {
    writeCacheConfigFile(ConfigManager.CONFIG_FILE_AU_CONFIG, s);
  }

  void writeCacheConfigFile(String cfileName, String s) throws IOException {
    String tmpdir = getTempDir().toString();
    ConfigurationUtil.setFromArgs(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
				  tmpdir);
    String relConfigPath =
      Configuration.getParam(ConfigManager.PARAM_CONFIG_PATH,
			     ConfigManager.DEFAULT_CONFIG_PATH);
    File cdir = new File(tmpdir, relConfigPath);
    File configFile = new File(cdir, cfileName);
    FileTestUtil.writeFile(configFile, s);
  }

  public void testGetAuConfigBackupStream () throws Exception {
    writeAuConfigFile("org.lockss.au.FooPlugin.k~v.k=v\n");
    InputStream is = rapi.getAuConfigBackupStream("machine_foo");
    String pat = "# AU Configuration saved .* from machine_foo\norg.lockss.au.FooPlugin.k~v.k=v";
    assertMatchesRE(pat, StringUtil.fromInputStream(is));
  }

  public void testCheckLegalBackupFile() throws Exception {
    Properties p = new Properties();
    p.setProperty("org.lockss.au.foobar", "17");
    p.setProperty("org.lockss.config.fileVersion.au", "1");
    assertEquals(1,
		 rapi.checkLegalBackupFile(ConfigManager.fromProperties(p)));
    p.setProperty("org.lockss.other.prop", "foo");
    try {
      rapi.checkLegalBackupFile(ConfigManager.fromProperties(p));
      fail("checkLegalBackupFile() allowed non-AU prop");
    } catch (RemoteApi.InvalidAuConfigBackupFile e) {
    }
    p.remove("org.lockss.other.prop");
    rapi.checkLegalBackupFile(ConfigManager.fromProperties(p));
    p.setProperty("org.lockss.au", "xx");
    try {
      rapi.checkLegalBackupFile(ConfigManager.fromProperties(p));
      fail("checkLegalBackupFile() allowed non-AU prop");
    } catch (RemoteApi.InvalidAuConfigBackupFile e) {
    }
    p.remove("org.lockss.au");
    rapi.checkLegalBackupFile(ConfigManager.fromProperties(p));
    p.setProperty("org.lockss.config.fileVersion.au", "0");
    try {
      rapi.checkLegalBackupFile(ConfigManager.fromProperties(p));
      fail("checkLegalBackupFile() allowed non-AU prop");
    } catch (RemoteApi.InvalidAuConfigBackupFile e) {
    }
  }

  class Pair {
    Object one, two;
    Pair(Object one, Object two) {
      this.one = one;
      this.two = two;
    }
  }

  // Fake PluginManager, should be completely mock, throwing on any
  // unimplemented mock method, but must extend PluginManager because
  // there's no separate interface
  class MyMockPluginManager extends PluginManager {
    List actions = new ArrayList();
    List allAus;
    List inactiveAuIds;

    Map storedConfigs = new HashMap();
    Map currentConfigs = new HashMap();

    void mockInit() {
      MockArchivalUnit mau1 = new MockArchivalUnit();
      mau1.setAuId(AUID1);
      putAuInMap(mau1);
      MockPlugin mp1 = new MockPlugin();
      mp1.setPluginId(PID1);
      setPlugin(pluginKeyFromId(mp1.getPluginId()), mp1);
    }

    void setStoredConfig(String auid, Configuration config) {
      storedConfigs.put(auid, config);
    }

    void setCurrentConfig(String auid, Configuration config) {
      currentConfigs.put(auid, config);
    }

    void setAllAus(List allAus) {
      this.allAus = allAus;
    }

    void setInactiveAuIds(List inactiveAuIds) {
      this.inactiveAuIds = inactiveAuIds;
    }


    public ArchivalUnit createAndSaveAuConfiguration(Plugin plugin,
						     Configuration auConf)
    throws ArchivalUnit.ConfigurationException {
      actions.add(new Pair(plugin, auConf));
      MockArchivalUnit mau = new MockArchivalUnit();
      mau.setPlugin(plugin);
      mau.setAuId(PluginManager.generateAuId(plugin, auConf));
      mau.setConfiguration(auConf);
      return mau;
    }

    public void setAndSaveAuConfiguration(ArchivalUnit au,
					  Configuration auConf)
	throws ArchivalUnit.ConfigurationException {
      actions.add(new Pair(au, auConf));
      au.setConfiguration(auConf);
    }

    public void deleteAuConfiguration(String auid) {
      actions.add(new Pair("Delete", auid));
    }

    public void deleteAuConfiguration(ArchivalUnit au) {
      actions.add(new Pair("Delete", au));
    }

    public void deactivateAuConfiguration(ArchivalUnit au) {
      actions.add(new Pair("Deactivate", au));
    }

    public Configuration getStoredAuConfiguration(String auid) {
      return (Configuration)storedConfigs.get(auid);
    }

    public Configuration getCurrentAuConfiguration(String auid) {
      return (Configuration)currentConfigs.get(auid);
    }

    public List getAllAus() {
      return allAus;
    }

    public Collection getInactiveAuIds() {
      return inactiveAuIds;
    }
  }
}
