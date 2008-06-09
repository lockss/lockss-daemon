/*
 * $Id: TestConfigManager.java,v 1.29 2008-06-09 05:42:02 tlipkis Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.config;

import java.io.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.protocol.*;
import org.lockss.clockss.*;
import org.lockss.servlet.*;

/**
 * Test class for <code>org.lockss.config.ConfigManager</code>
 */

public class TestConfigManager extends LockssTestCase {

  ConfigManager mgr;

  public void setUp() throws Exception {
    super.setUp();
    mgr = ConfigManager.makeConfigManager();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  static Logger log = Logger.getLogger("TestConfig");

  private static final String c1 = "prop1=12\nprop2=foobar\nprop3=true\n" +
    "prop5=False\n";
  private static final String c1a = "prop2=xxx\nprop4=yyy\n";

  private static final String c2 =
    "timeint=14d\n" +
    "prop.p1=12\n" +
    "prop.p2=foobar\n" +
    "prop.p3.a=true\n" +
    "prop.p3.b=false\n" +
    "otherprop.p3.b=foo\n";

  private ConfigFile loadFCF(String url) throws IOException {
    FileConfigFile cf = new FileConfigFile(url);
    cf.reload();
    return cf;
  }

  public void testParam() throws IOException, Configuration.InvalidParam {
    Configuration config = ConfigManager.newConfiguration();
    config.load(loadFCF(FileTestUtil.urlOfString(c2)));
    mgr.setCurrentConfig(config);

    assertEquals("12", CurrentConfig.getParam("prop.p1"));
    assertEquals("foobar", CurrentConfig.getParam("prop.p2"));
    assertTrue(CurrentConfig.getBooleanParam("prop.p3.a", false));
    assertEquals(12, CurrentConfig.getIntParam("prop.p1"));
    assertEquals(554, CurrentConfig.getIntParam("propnot.p1", 554));
    assertEquals(2 * Constants.WEEK,
                 CurrentConfig.getTimeIntervalParam("timeint", 554));
    assertEquals(554, CurrentConfig.getTimeIntervalParam("noparam", 554));
  }

  boolean setCurrentConfigFromUrlList(List l) throws IOException {
    Configuration config = mgr.readConfig(l);
    return mgr.installConfig(config);
  }

  boolean setCurrentConfigFromString(String s)
      throws IOException {
    return setCurrentConfigFromUrlList(ListUtil.list(FileTestUtil.urlOfString(s)));
  }

  public void testCurrentConfig() throws IOException {
    assertTrue(setCurrentConfigFromUrlList(ListUtil.
					   list(FileTestUtil.urlOfString(c1),
						FileTestUtil.urlOfString(c1a))));
    assertEquals("12", CurrentConfig.getParam("prop1"));
    Configuration config = ConfigManager.getCurrentConfig();
    assertEquals("12", config.get("prop1"));
    assertEquals("12", config.get("prop1", "wrong"));
    assertEquals("xxx", config.get("prop2"));
    assertTrue(config.getBoolean("prop3", false));
    assertEquals("yyy", config.get("prop4"));
    assertEquals("def", config.get("noprop", "def"));
    assertEquals("def", CurrentConfig.getParam("noprop", "def"));
  }

  volatile Configuration.Differences cbDiffs = null;
  List configs;

  public void testCallbackWhenRegister() throws IOException {
    configs = new ArrayList();
    setCurrentConfigFromUrlList(ListUtil.list(FileTestUtil.urlOfString(c1),
					      FileTestUtil.urlOfString(c1a)));
    assertEquals(0, configs.size());
    Configuration config = ConfigManager.getCurrentConfig();
    mgr.registerConfigurationCallback(new Configuration.Callback() {
	public void configurationChanged(Configuration newConfig,
					 Configuration oldConfig,
					 Configuration.Differences diffs) {
	  assertNotNull(oldConfig);
	  configs.add(newConfig);
	  cbDiffs = diffs;
	}
      });
    assertEquals(1, configs.size());
    assertEquals(config, configs.get(0));
    assertTrue(cbDiffs.contains("everything"));
    assertNull(cbDiffs.getDifferenceSet());
  }

  public void testCallback() throws IOException {
    setCurrentConfigFromUrlList(ListUtil.list(FileTestUtil.urlOfString(c1),
					      FileTestUtil.urlOfString(c1a)));
    log.debug(ConfigManager.getCurrentConfig().toString());
    mgr.registerConfigurationCallback(new Configuration.Callback() {
	public void configurationChanged(Configuration newConfig,
					 Configuration oldConfig,
					 Configuration.Differences diffs) {
	  log.debug("Notify: " + diffs);
	  cbDiffs = diffs;
	}
      });
    assertTrue(setCurrentConfigFromUrlList(ListUtil.
					   list(FileTestUtil.urlOfString(c1a),
						FileTestUtil.urlOfString(c1))));
    assertTrue(cbDiffs.contains("prop2"));
    assertFalse(cbDiffs.contains("prop4"));
    assertEquals(SetUtil.set("prop2"), cbDiffs.getDifferenceSet());
    log.debug(ConfigManager.getCurrentConfig().toString());
    assertTrue(setCurrentConfigFromUrlList(ListUtil.
					   list(FileTestUtil.urlOfString(c1),
						FileTestUtil.urlOfString(c1))));
    assertTrue(cbDiffs.contains("prop4"));
    assertFalse(cbDiffs.contains("prop2"));
    assertEquals(SetUtil.set("prop4"), cbDiffs.getDifferenceSet());
    log.debug(ConfigManager.getCurrentConfig().toString());
    assertTrue(setCurrentConfigFromUrlList(ListUtil.
					   list(FileTestUtil.urlOfString(c1),
						FileTestUtil.urlOfString(c1a))));
    assertEquals(SetUtil.set("prop4", "prop2"), cbDiffs.getDifferenceSet());
    log.debug(ConfigManager.getCurrentConfig().toString());

  }

  public void testPlatformProps() throws Exception {
    Properties props = new Properties();
    props.put("org.lockss.platform.localIPAddress", "1.2.3.4");
    props.put("org.lockss.platform.v3.identity", "tcp:[1.2.3.4]:4321");
    props.put("org.lockss.platform.logdirectory", "/var/log/foo");
    props.put("org.lockss.platform.logfile", "bar");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    Configuration config = ConfigManager.getCurrentConfig();
    assertEquals("1.2.3.4", config.get("org.lockss.localIPAddress"));
    assertEquals("tcp:[1.2.3.4]:4321",
		 config.get("org.lockss.localV3Identity"));
    assertEquals(FileUtil.sysDepPath("/var/log/foo/bar"),
		 config.get(FileTarget.PARAM_FILE));
  }

  public void testPlatformConfig() throws Exception {
    Properties props = new Properties();
    props.put("org.lockss.platform.localIPAddress", "1.2.3.4");
    props.put("org.lockss.platform.v3.identity", "tcp:[1.2.3.4]:4321");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    Configuration config = ConfigManager.getPlatformConfig();
    assertEquals("1.2.3.4", config.get("org.lockss.localIPAddress"));
    assertEquals("tcp:[1.2.3.4]:4321",
		 config.get("org.lockss.localV3Identity"));
  }

  public void testPlatformClockss() throws Exception {
    Properties props = new Properties();
    ConfigurationUtil.setCurrentConfigFromProps(props);
    Configuration config = ConfigManager.getCurrentConfig();
    assertNull(config.get(ClockssParams.PARAM_CLOCKSS_SUBSCRIPTION_ADDR));
    assertNull(config.get(ClockssParams.PARAM_INSTITUTION_SUBSCRIPTION_ADDR));
    props.put("org.lockss.platform.localIPAddress", "1.1.1.1");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    config = ConfigManager.getCurrentConfig();
    assertEquals("1.1.1.1",
		 config.get(ClockssParams.PARAM_INSTITUTION_SUBSCRIPTION_ADDR));
    assertNull(config.get(ClockssParams.PARAM_CLOCKSS_SUBSCRIPTION_ADDR));

    props.put("org.lockss.platform.secondIP", "2.2.2.2");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    config = ConfigManager.getCurrentConfig();
    assertEquals("1.1.1.1",
		 config.get(ClockssParams.PARAM_INSTITUTION_SUBSCRIPTION_ADDR));
    assertEquals("2.2.2.2",
		 config.get(ClockssParams.PARAM_CLOCKSS_SUBSCRIPTION_ADDR));
  }

  public void testInitNewConfiguration() throws Exception {
    mgr =  new ConfigManager(ListUtil.list("foo"), "group1;GROUP2");
    Configuration config = mgr.initNewConfiguration();
    assertEquals("group1;group2", config.getPlatformGroups());
    assertEquals(ListUtil.list("group1", "group2"),
		 config.getPlatformGroupList());
  }

  public void testGroup() throws Exception {
    Properties props = new Properties();
    ConfigurationUtil.setCurrentConfigFromProps(props);
    assertEquals("nogroup", ConfigManager.getPlatformGroups());
    props.put(ConfigManager.PARAM_DAEMON_GROUPS, "foog");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    assertEquals("foog", ConfigManager.getPlatformGroups());
    assertEquals(ListUtil.list("foog"), ConfigManager.getPlatformGroupList());
    props.put(ConfigManager.PARAM_DAEMON_GROUPS, "foog;barg");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    assertEquals("foog;barg", ConfigManager.getPlatformGroups());
    assertEquals(ListUtil.list("foog", "barg"),
		 ConfigManager.getPlatformGroupList());
  }

  // platform access not set, ui and proxy access not set
  public void testPlatformAccess0() throws Exception {
    Properties props = new Properties();
    ConfigurationUtil.setCurrentConfigFromProps(props);
    Configuration config = ConfigManager.getCurrentConfig();
    assertFalse(config.containsKey("org.lockss.ui.access.ip.include"));
    assertFalse(config.containsKey("org.lockss.proxy.access.ip.include"));
  }

  // platform access not set, ui and proxy access set
  public void testPlatformAccess1() throws Exception {
    Properties props = new Properties();
    props.put("org.lockss.ui.access.ip.include", "1.2.3.0/22");
    props.put("org.lockss.proxy.access.ip.include", "1.2.3.0/21");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    Configuration config = ConfigManager.getCurrentConfig();
    assertEquals("1.2.3.0/22", config.get("org.lockss.ui.access.ip.include"));
    assertEquals("1.2.3.0/21",
		 config.get("org.lockss.proxy.access.ip.include"));
  }

  // platform access set, ui and proxy access not set
  public void testPlatformAccess2() throws Exception {
    Properties props = new Properties();
    props.put("org.lockss.platform.accesssubnet", "1.2.3.*");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    Configuration config = ConfigManager.getCurrentConfig();
    assertEquals("1.2.3.*", config.get("org.lockss.ui.access.ip.include"));
    assertEquals("1.2.3.*", config.get("org.lockss.proxy.access.ip.include"));
  }

  // platform access set, ui and proxy access set globally, not locally
  public void testPlatformAccess3() throws Exception {
    Properties props = new Properties();
    props.put("org.lockss.platform.accesssubnet", "1.2.3.*");
    props.put("org.lockss.ui.access.ip.include", "1.2.3.0/22");
    props.put("org.lockss.proxy.access.ip.include", "1.2.3.0/21");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    Configuration config = ConfigManager.getCurrentConfig();
    assertEquals("1.2.3.*;1.2.3.0/22",
		 config.get("org.lockss.ui.access.ip.include"));
    assertEquals("1.2.3.*;1.2.3.0/21",
		 config.get("org.lockss.proxy.access.ip.include"));
  }

  // platform access set, ui and proxy access set locally
  public void testPlatformAccess4() throws Exception {
    Properties props = new Properties();
    props.put("org.lockss.platform.accesssubnet", "1.2.3.*");
    props.put("org.lockss.ui.access.ip.include", "1.2.3.0/22");
    props.put("org.lockss.ui.access.ip.platformAccess", "1.2.3.*");
    props.put("org.lockss.proxy.access.ip.include", "1.2.3.0/21");
    props.put("org.lockss.proxy.access.ip.platformAccess", "3.2.1.0/22");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    Configuration config = ConfigManager.getCurrentConfig();
    assertEquals("1.2.3.0/22",
		 config.get("org.lockss.ui.access.ip.include"));
    assertEquals("1.2.3.*;1.2.3.0/21",
		 config.get("org.lockss.proxy.access.ip.include"));
  }

  // platform access set, ui and proxy access set locally
  public void testPlatformAccess5() throws Exception {
    Properties props = new Properties();
    props.put("org.lockss.platform.accesssubnet", "1.2.3.*;4.4.4.0/24");
    props.put("org.lockss.ui.access.ip.include", "1.2.3.0/22;1.2.3.*");
    props.put("org.lockss.proxy.access.ip.include", "1.2.3.0/21;5.5.0.0/18");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    Configuration config = ConfigManager.getCurrentConfig();
    assertEquals("1.2.3.*;4.4.4.0/24;1.2.3.0/22",
		 config.get("org.lockss.ui.access.ip.include"));
    assertEquals("1.2.3.*;4.4.4.0/24;1.2.3.0/21;5.5.0.0/18",
		 config.get("org.lockss.proxy.access.ip.include"));
  }

  public void testPlatformSpace1() throws Exception {
    Properties props = new Properties();
    props.put("org.lockss.platform.diskSpacePaths", "/foo/bar");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    Configuration config = ConfigManager.getCurrentConfig();
    assertEquals("/foo/bar", config.get("org.lockss.cache.location"));
    assertEquals("/foo/bar", config.get("org.lockss.history.location"));
  }

  public void testPlatformSpace2() throws Exception {
    Properties props = new Properties();
    props.put("org.lockss.platform.diskSpacePaths", "/a/b;/foo/bar");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    Configuration config = ConfigManager.getCurrentConfig();
    assertEquals("/a/b", config.get("org.lockss.cache.location"));
    assertEquals("/a/b", config.get("org.lockss.history.location"));
    assertEquals(FileUtil.sysDepPath("/a/b/iddb"),
		 config.get("org.lockss.id.database.dir"));
  }

  public void testPlatformSmtp() throws Exception {
    Properties props = new Properties();
    props.put("org.lockss.platform.smtphost", "smtp.example.com");
    props.put("org.lockss.platform.smtpport", "25");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    Configuration config = ConfigManager.getCurrentConfig();
    assertEquals("smtp.example.com",
		 config.get("org.lockss.mail.smtphost"));
    assertEquals("25",
		 config.get("org.lockss.mail.smtpport"));
  }

  public void testPlatformVersionConfig() throws Exception {
    Properties props = new Properties();
    props.put(ConfigManager.PARAM_PLATFORM_VERSION, "123");
    props.put("org.lockss.foo", "44");
    props.put("123.org.lockss.foo", "55");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    Configuration config = ConfigManager.getCurrentConfig();
    assertEquals("55", config.get("org.lockss.foo"));
  }

  public void testPlatformDifferentVersionConfig() throws Exception {
    Properties props = new Properties();
    props.put(ConfigManager.PARAM_PLATFORM_VERSION, "321");
    props.put("org.lockss.foo", "22");
    props.put("123.org.lockss.foo", "55");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    Configuration config = ConfigManager.getCurrentConfig();
    assertEquals("22", config.get("org.lockss.foo"));
  }

  public void testPlatformNoVersionConfig() throws Exception {
    Properties props = new Properties();
    props.put("org.lockss.foo", "11");
    props.put("123.org.lockss.foo", "55");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    Configuration config = ConfigManager.getCurrentConfig();
    assertEquals("11", config.get("org.lockss.foo"));
  }

  public void testPlatformConfigDirSetup() throws Exception {
    String tmpdir = getTempDir().toString();
    Properties props = new Properties();
    props.put(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tmpdir);
    ConfigurationUtil.setCurrentConfigFromProps(props);
    String relConfigPath =
      CurrentConfig.getParam(ConfigManager.PARAM_CONFIG_PATH,
                             ConfigManager.DEFAULT_CONFIG_PATH);
    File cdir = new File(tmpdir, relConfigPath);
    assertTrue(cdir.exists());
    Configuration config = CurrentConfig.getCurrentConfig();
  }

  public void testGetVersionString() throws Exception {
    Properties props = new Properties();
    props.put(ConfigManager.PARAM_PLATFORM_VERSION, "321");
    props.put(ConfigManager.PARAM_DAEMON_VERSION, "1.44.2");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    List pairs = StringUtil.breakAt(mgr.getVersionString(), ',');
    String release = BuildInfo.getBuildProperty(BuildInfo.BUILD_RELEASENAME);
    if (release != null) {
      assertEquals(SetUtil.set("groups=nogroup",
			       "platform=OpenBSD CD 321",
			       "daemon=" + release),
		   SetUtil.theSet(pairs));
    } else {
      assertEquals(SetUtil.set("groups=nogroup",
			       "platform=OpenBSD CD 321",
			       "daemon=1.44.2"),
		   SetUtil.theSet(pairs));
    }
    mgr.setGroups("grouper");
    ConfigurationUtil.addFromArgs(IdentityManager.PARAM_LOCAL_V3_IDENTITY,
				  "tcp:[111.32.14.5]:9876");
    pairs = StringUtil.breakAt(mgr.getVersionString(), ',');
    if (release != null) {
      assertEquals(SetUtil.set("groups=grouper",
			       "peerid=tcp:[111.32.14.5]:9876",
			       "platform=OpenBSD CD 321",
			       "daemon=" + release),
		   SetUtil.theSet(pairs));
    } else {
      assertEquals(SetUtil.set("groups=grouper",
			       "peerid=tcp:[111.32.14.5]:9876",
			       "platform=OpenBSD CD 321",
			       "daemon=1.44.2"),
		   SetUtil.theSet(pairs));
    }
  }

  public void testMiscTmpdir() throws Exception {
    ConfigurationUtil.setFromArgs(ConfigManager.PARAM_TMPDIR, "/tmp/unlikely");
    assertEquals("/tmp/unlikely", System.getProperty("java.io.tmpdir"));
  }

  public void testConfigVersionProp() {
    assertEquals("org.lockss.config.fileVersion.foo",
		 ConfigManager.configVersionProp("foo"));
  }

  public void testCompatibilityParams() {
    Configuration config = ConfigManager.getCurrentConfig();
    assertEquals(null, config.get(AdminServletManager.PARAM_CONTACT_ADDR));
    assertEquals(null, config.get(AdminServletManager.PARAM_HELP_URL));
    ConfigurationUtil.setFromArgs(ConfigManager.PARAM_OBS_ADMIN_CONTACT_EMAIL,
				  "Nicola@teslasociety.org",
				  ConfigManager.PARAM_OBS_ADMIN_HELP_URL,
				  "help://cause.I.need.somebody/");
    config = ConfigManager.getCurrentConfig();
    assertEquals("Nicola@teslasociety.org",
		 config.get(AdminServletManager.PARAM_CONTACT_ADDR));
    assertEquals("help://cause.I.need.somebody/",
		 config.get(AdminServletManager.PARAM_HELP_URL));
  }



  public void testWriteAndReadCacheConfigFile() throws Exception {
    String fname = "test-config";
    String tmpdir = getTempDir().toString();
    Properties props = new Properties();
    props.put(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tmpdir);
    ConfigurationUtil.setCurrentConfigFromProps(props);
    String relConfigPath =
      CurrentConfig.getParam(ConfigManager.PARAM_CONFIG_PATH,
                             ConfigManager.DEFAULT_CONFIG_PATH);
    File cdir = new File(tmpdir, relConfigPath);
    assertTrue(cdir.exists());
    Properties acprops = new Properties();
    acprops.put("foo.bar" , "12345");
    mgr.writeCacheConfigFile(acprops, fname, "this is a header");

    File acfile = new File(cdir, fname);
    assertTrue(acfile.exists());

    Configuration config2 = mgr.readCacheConfigFile(fname);
    assertEquals("12345", config2.get("foo.bar"));
    assertEquals("1", config2.get("org.lockss.config.fileVersion." + fname));
    assertEquals("wrong number of keys in written config file",
		 2, config2.keySet().size());
  }

  public void testCacheConfigFile() throws Exception {
    String tmpdir = getTempDir().toString();
    ConfigurationUtil.setFromArgs(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
				  tmpdir);
    String relConfigPath =
      CurrentConfig.getParam(ConfigManager.PARAM_CONFIG_PATH,
                             ConfigManager.DEFAULT_CONFIG_PATH);
    File cdir = new File(tmpdir, relConfigPath);
    assertTrue(cdir.exists());
    Properties acprops = new Properties();
    acprops.put("foo.bar" , "12345");
    mgr.writeCacheConfigFile(acprops, ConfigManager.CONFIG_FILE_UI_IP_ACCESS,
			     "this is a header");

    Configuration config = ConfigManager.getCurrentConfig();
    assertNull(config.get("foo.bar"));
    assertTrue(mgr.updateConfig());
    Configuration config2 = ConfigManager.getCurrentConfig();
    assertEquals("12345", config2.get("foo.bar"));
  }

  public void testUpdateAuConfig() throws Exception {
    String tmpdir = getTempDir().toString();
    // establish cache config dir
    ConfigurationUtil.setFromArgs(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
				  tmpdir);

    Properties p = new Properties();
    p.put("org.lockss.au.fooauid.foo", "111");
    p.put("org.lockss.au.fooauid.bar", "222");
    p.put("org.lockss.au.fooauid.baz", "333");

    Configuration config = CurrentConfig.getCurrentConfig();
    assertNull(config.get("org.lockss.au.auid.foo"));

    // should create file first time
    mgr.updateAuConfigFile(p, "org.lockss.au.fooauid");

    // next update should load au config file
    assertTrue(mgr.updateConfig());
    config = CurrentConfig.getCurrentConfig();
    assertEquals("111", config.get("org.lockss.au.fooauid.foo"));
    assertEquals("222", config.get("org.lockss.au.fooauid.bar"));
    assertEquals("333", config.get("org.lockss.au.fooauid.baz"));
    assertNull(config.get("org.lockss.au.auid.foo"));
    assertNull(config.get("org.lockss.au.auid.bar"));

    // add a different au, make sure they both get loaded
    p = new Properties();
    p.put("org.lockss.au.auid.foo", "11");
    p.put("org.lockss.au.auid.bar", "22");

    updateAuLastModified(TimeBase.nowMs() + Constants.SECOND);

    mgr.updateAuConfigFile(p, "org.lockss.au.auid");

    // next update should load au config file
    assertTrue(mgr.updateConfig());
    config = CurrentConfig.getCurrentConfig();
    assertEquals("111", config.get("org.lockss.au.fooauid.foo"));
    assertEquals("222", config.get("org.lockss.au.fooauid.bar"));
    assertEquals("333", config.get("org.lockss.au.fooauid.baz"));
    assertEquals("11", config.get("org.lockss.au.auid.foo"));
    assertEquals("22", config.get("org.lockss.au.auid.bar"));

    // update first au, removing a property, make sure it really gets deleted
    p = new Properties();
    p.put("org.lockss.au.fooauid.foo", "111");
    p.put("org.lockss.au.fooauid.bar", "222");

    mgr.updateAuConfigFile(p, "org.lockss.au.fooauid");

    assertTrue(mgr.updateConfig());
    config = CurrentConfig.getCurrentConfig();
    assertEquals("111", config.get("org.lockss.au.fooauid.foo"));
    assertEquals("222", config.get("org.lockss.au.fooauid.bar"));
    assertEquals(null, config.get("org.lockss.au.fooauid.baz"));
    assertEquals("11", config.get("org.lockss.au.auid.foo"));
    assertEquals("22", config.get("org.lockss.au.auid.bar"));
  }

  void updateAuLastModified(long time) {
    for (Iterator iter = mgr.getCacheConfigFiles().iterator();
	 iter.hasNext(); ) {
      File file = (File)iter.next();
      if (ConfigManager.CONFIG_FILE_AU_CONFIG.equals(file.getName())) {
	file.setLastModified(time);
      }
    }
  }

  public void testModifyCacheConfigFile() throws Exception {
    // Use some config file; for instance the ICP server config file
    final String FILE = ConfigManager.CONFIG_FILE_ICP_SERVER;

    String tmpdir = getTempDir().toString();
    ConfigurationUtil.setFromArgs(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
                                  tmpdir);
    assertNull(CurrentConfig.getParam("foo"));
    assertNull(CurrentConfig.getParam("bar"));
    assertNull(CurrentConfig.getParam("baz"));

    mgr.modifyCacheConfigFile(ConfigurationUtil.fromArgs("foo", "1", "bar", "2"),
                              FILE,
                              null);
    ConfigurationUtil.setFromArgs(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
                                  tmpdir); // force reload
    assertEquals("1", CurrentConfig.getParam("foo"));
    assertEquals("2", CurrentConfig.getParam("bar"));
    assertNull(CurrentConfig.getParam("baz"));

    mgr.modifyCacheConfigFile(ConfigurationUtil.fromArgs("foo", "111", "baz", "333"),
                              FILE,
                              null);
    ConfigurationUtil.setFromArgs(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
                                  tmpdir); // force reload
    assertEquals("111", CurrentConfig.getParam("foo"));
    assertEquals("2", CurrentConfig.getParam("bar"));
    assertEquals("333", CurrentConfig.getParam("baz"));

    mgr.modifyCacheConfigFile(ConfigurationUtil.fromArgs("bar", "222"),
                              SetUtil.set("foo"),
                              FILE,
                              null);
    ConfigurationUtil.setFromArgs(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
                                  tmpdir); // force reload
    assertFalse(CurrentConfig.getCurrentConfig().containsKey("foo"));
    assertEquals("222", CurrentConfig.getParam("bar"));
    assertEquals("333", CurrentConfig.getParam("baz"));

    try {
      mgr.modifyCacheConfigFile(ConfigurationUtil.fromArgs("foo", "1"),
                                SetUtil.set("foo"),
                                FILE,
                                null);
      fail("Failed to throw an IllegalArgumentException when a key was both in the update set and in the delete set");
    } catch (IllegalArgumentException iae) {
      // All is well
    }
  }

  public void testReadAuConfigFile() throws Exception {
    String tmpdir = getTempDir().toString();
    // establish cache config dir
    ConfigurationUtil.setFromArgs(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
				  tmpdir);

    Configuration c1 = mgr.readAuConfigFile();
    assertTrue(c1.isEmpty());

    Properties p = new Properties();
    p.put("org.lockss.au.fooauid.foo", "111");
    p.put("org.lockss.au.fooauid.bar", "222");
    p.put("org.lockss.au.fooauid.baz", "333");

    mgr.updateAuConfigFile(p, "org.lockss.au.fooauid");

    Configuration c2 = mgr.readAuConfigFile();
    assertFalse(c2.isEmpty());

    assertEquals("111", c2.get("org.lockss.au.fooauid.foo"));
    assertEquals("222", c2.get("org.lockss.au.fooauid.bar"));
    assertEquals("333", c2.get("org.lockss.au.fooauid.baz"));
  }

  public void testLoadTitleDb() throws IOException {
    List gens;

    String u2 = FileTestUtil.urlOfString("org.lockss.title.foo=bar");
    String u1 = FileTestUtil.urlOfString("a=1\norg.lockss.titleDbs="+u2);
    assertFalse(mgr.waitConfig(Deadline.EXPIRED));
    assertTrue(mgr.updateConfig(ListUtil.list(u1)));
    assertTrue(mgr.waitConfig(Deadline.EXPIRED));
    Configuration config = mgr.getCurrentConfig();
    assertEquals("bar", config.get("org.lockss.title.foo"));
    assertEquals("1", config.get("a"));
  }

  public void testFailedLoadDoesntSetHaveConfig() throws IOException {
    String u1 = "malformed://url/";
    assertFalse(mgr.waitConfig(Deadline.EXPIRED));
    assertFalse(mgr.updateConfig(ListUtil.list(u1)));
    assertFalse(mgr.waitConfig(Deadline.EXPIRED));
  }

  // Currently an illegal key prevents loading the entire file.  I'm not
  // sure that's desirable
  public void testLoadIllTitleDb() throws IOException {
    String u2 = FileTestUtil.urlOfString("org.lockss.notTitleDb.foo=bar\n" +
					 "org.lockss.title.x.foo=bar");
    String u1 = FileTestUtil.urlOfString("a=1\norg.lockss.titleDbs="+u2);
    assertTrue(mgr.updateConfig(ListUtil.list(u1)));
    Configuration config = mgr.getCurrentConfig();
    assertEquals(null, config.get("org.lockss.title.x.foo"));
    assertEquals(null, config.get("org.lockss.notTitleDb.foo"));
    assertEquals("1", config.get("a"));
  }

  public void testIsChanged() throws IOException {
    List gens;

    String u1 = FileTestUtil.urlOfString("a=1");
    String u2 = FileTestUtil.urlOfString("a=2");
    gens = mgr.getConfigGenerations(ListUtil.list(u1, u2), true, true, "test");
    assertTrue(mgr.isChanged(gens));
    mgr.updateGenerations(gens);
    assertFalse(mgr.isChanged(gens));
    FileConfigFile cf = (FileConfigFile)mgr.getConfigCache().find(u2);
    cf.storedConfig(newConfiguration());
    gens = mgr.getConfigGenerations(ListUtil.list(u1, u2), true, true, "test");
    assertTrue(mgr.isChanged(gens));
  }

  public void testLoadList() throws IOException {
    Configuration config = newConfiguration();
    List gens =
      mgr.getConfigGenerations(ListUtil.list(FileTestUtil.urlOfString(c1),
					     FileTestUtil.urlOfString(c1a)),
			       true, true, "props");
    mgr.loadList(config, gens);
    assertEquals("12", config.get("prop1"));
    assertEquals("xxx", config.get("prop2"));
    assertTrue(config.getBoolean("prop3", false));
    assertEquals("yyy", config.get("prop4"));
  }

  public void testConnPool() throws IOException {
    LockssUrlConnectionPool pool = mgr.getConnectionPool();
    Configuration config = ConfigManager.newConfiguration();
    config.put("bar", "false");
    MemoryConfigFile cf1 = new MemoryConfigFile("a", config, 1);
    MemoryConfigFile cf2 = new MemoryConfigFile("a", config, 1);
    
    List<ConfigFile.Generation> gens =
      mgr.getConfigGenerations(ListUtil.list(cf1, cf2), true, true, "props");
    for (ConfigFile.Generation gen : gens) {
      MemoryConfigFile cf = (MemoryConfigFile)gen.getConfigFile();
      assertSame(pool, cf.getConnectionPool());
    }
  }

  public void testXLockssInfo() throws IOException {
    TimeBase.setSimulated(1000);
    String u1 = FileTestUtil.urlOfString("org.lockss.foo=bar");
    assertTrue(mgr.updateConfig(ListUtil.list(u1)));
    BaseConfigFile cf = (BaseConfigFile)mgr.getConfigCache().find(u1);
    String info = (String)cf.m_props.get("X-Lockss-Info");
    assertMatchesRE("groups=nogroup", info);
    // official build will set daemon, unofficial will set built_on
    assertMatchesRE("daemon=|built_on=", info);
    cf.setNeedsReload();
    assertFalse(mgr.updateConfig(ListUtil.list(u1)));
    info = (String)cf.m_props.get("X-Lockss-Info");
    assertEquals(null, info);
    TimeBase.step(ConfigManager.DEFAULT_SEND_VERSION_EVERY + 1);
    cf.setNeedsReload();
    assertFalse(mgr.updateConfig(ListUtil.list(u1)));
    info = (String)cf.m_props.get("X-Lockss-Info");
    assertMatchesRE("groups=nogroup", info);
    // official build will set daemon, unofficial will set built_on
    assertMatchesRE("daemon=|built_on=", info);
  }

  public void testHasLocalCacheConfig() throws Exception {
    assertFalse(mgr.hasLocalCacheConfig());
    // set up local config dir
    String tmpdir = getTempDir().toString();
    Properties props = new Properties();
    props.put(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tmpdir);
    ConfigurationUtil.setCurrentConfigFromProps(props);
    String relConfigPath =
      CurrentConfig.getParam(ConfigManager.PARAM_CONFIG_PATH,
                             ConfigManager.DEFAULT_CONFIG_PATH);
    File cdir = new File(tmpdir, relConfigPath);
    assertTrue(cdir.exists());

    assertFalse(mgr.hasLocalCacheConfig());

    // loading local shouldn't set flag because no files
    mgr.getCacheConfigGenerations(true);
    assertFalse(mgr.hasLocalCacheConfig());

    // write a local config file
    mgr.writeCacheConfigFile(props, ConfigManager.CONFIG_FILE_AU_CONFIG,
			     "this is a header");

    assertFalse(mgr.hasLocalCacheConfig());

    // load it to set flag
    mgr.getCacheConfigGenerations(true);

    assertTrue(mgr.hasLocalCacheConfig());
  }

  public void testFromProperties() throws Exception {
    Properties props = new Properties();
    props.put("foo", "23");
    props.put("bar", "false");
    Configuration config = ConfigManager.fromProperties(props);
    assertEquals(2, config.keySet().size());
    assertEquals("23", config.get("foo"));
    assertEquals("false", config.get("bar"));
  }

  private Configuration newConfiguration() {
    return new ConfigurationPropTreeImpl();
  }
}
