/*
 * $Id: TestConfigManager.java,v 1.13 2004-08-18 00:14:52 tlipkis Exp $
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
import java.io.*;
import java.net.*;
import org.lockss.util.*;
import org.lockss.test.*;

/**
 * Test class for <code>org.lockss.util.ConfigManager</code>
 */

public class TestConfigManager extends LockssTestCase {
//   public static Class testedClasses[] = {
//     org.lockss.daemon.ConfigManager.class
//   };

  ConfigManager mgr;

  public void setUp() throws Exception {
    super.setUp();
    mgr =  ConfigManager.makeConfigManager();
  }

  public void tearDown() throws Exception {
    ConfigManager.resetForTesting();
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

  public void testParam() throws IOException, Configuration.InvalidParam {
    Configuration config = mgr.newConfiguration();
    config.load(new ConfigFile(FileTestUtil.urlOfString(c2)));
    mgr.setCurrentConfig(config);
    assertEquals("12", ConfigManager.getParam("prop.p1"));
    assertEquals("foobar", ConfigManager.getParam("prop.p2"));
    assertTrue(ConfigManager.getBooleanParam("prop.p3.a", false));
    assertEquals(12, ConfigManager.getIntParam("prop.p1"));
    assertEquals(554, ConfigManager.getIntParam("propnot.p1", 554));
    assertEquals(2 * Constants.WEEK,
		 ConfigManager.getTimeIntervalParam("timeint", 554));
    assertEquals(554, ConfigManager.getTimeIntervalParam("noparam", 554));

    // these should go once static param methods are removed from Configuration
    assertEquals("12", Configuration.getParam("prop.p1"));
    assertEquals("foobar", Configuration.getParam("prop.p2"));
    assertTrue(Configuration.getBooleanParam("prop.p3.a", false));
    assertEquals(12, Configuration.getIntParam("prop.p1"));
    assertEquals(554, Configuration.getIntParam("propnot.p1", 554));
    assertEquals(2 * Constants.WEEK,
		 Configuration.getTimeIntervalParam("timeint", 554));
    assertEquals(554, Configuration.getTimeIntervalParam("noparam", 554));
  }

  boolean setCurrentConfigFromUrlList(List l) {
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
    assertEquals("12", ConfigManager.getParam("prop1"));
    Configuration config = ConfigManager.getCurrentConfig();
    assertEquals("12", config.get("prop1"));
    assertEquals("12", config.get("prop1", "wrong"));
    assertEquals("xxx", config.get("prop2"));
    assertTrue(config.getBoolean("prop3", false));
    assertEquals("yyy", config.get("prop4"));
    assertEquals("def", config.get("noprop", "def"));
    assertEquals("def", ConfigManager.getParam("noprop", "def"));
  }

  volatile Set diffSet = null;
  List configs;

  public void testCallback() throws IOException {
    configs = new ArrayList();
    setCurrentConfigFromUrlList(ListUtil.list(FileTestUtil.urlOfString(c1),
					      FileTestUtil.urlOfString(c1a)));
    assertEquals(0, configs.size());
    mgr.registerConfigurationCallback(new Configuration.Callback() {
	public void configurationChanged(Configuration newConfig,
					 Configuration oldConfig,
					 Configuration.Differences diffs) {
	  assertNotNull(oldConfig);
	  configs.add(newConfig);
	}
      });
    assertEquals(1, configs.size());
  }

  public void testCallbackDiffs() throws IOException {
    setCurrentConfigFromUrlList(ListUtil.list(FileTestUtil.urlOfString(c1),
					      FileTestUtil.urlOfString(c1a)));
    System.out.println(mgr.getCurrentConfig().toString());
    mgr.registerConfigurationCallback(new Configuration.Callback() {
	public void configurationChanged(Configuration newConfig,
					 Configuration oldConfig,
					 Configuration.Differences diffs) {
	  System.out.println("Notify: " + diffs);
	  diffSet = diffs.getDifferenceSet();
	}
      });
    assertTrue(setCurrentConfigFromUrlList(ListUtil.
					   list(FileTestUtil.urlOfString(c1a),
						FileTestUtil.urlOfString(c1))));
    assertEquals(SetUtil.set("prop2"), diffSet);
    System.out.println(mgr.getCurrentConfig().toString());
    assertTrue(setCurrentConfigFromUrlList(ListUtil.
					   list(FileTestUtil.urlOfString(c1),
						FileTestUtil.urlOfString(c1))));
    assertEquals(SetUtil.set("prop4"), diffSet);
    System.out.println(mgr.getCurrentConfig().toString());
    assertTrue(setCurrentConfigFromUrlList(ListUtil.
					   list(FileTestUtil.urlOfString(c1),
						FileTestUtil.urlOfString(c1a))));
    assertEquals(SetUtil.set("prop4", "prop2"), diffSet);
    System.out.println(mgr.getCurrentConfig().toString());

  }

  public void testPlatformProps() throws Exception {
    Properties props = new Properties();
    props.put("org.lockss.platform.localIPAddress", "1.2.3.4");
    props.put("org.lockss.platform.logdirectory", "/var/log/foo");
    props.put("org.lockss.platform.logfile", "bar");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    Configuration config = mgr.getCurrentConfig();
    assertEquals("1.2.3.4", config.get("org.lockss.localIPAddress"));
    assertEquals(FileUtil.sysDepPath("/var/log/foo/bar"),
                 config.get(FileTarget.PARAM_FILE));
  }

  public void testPlatformAccess1() throws Exception {
    // platform access set, ui and proxy access not set
    Properties props = new Properties();
    props.put("org.lockss.platform.accesssubnet", "1.2.3.*");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    Configuration config = mgr.getCurrentConfig();
    assertEquals("1.2.3.*", config.get("org.lockss.ui.access.ip.include"));
    assertEquals("1.2.3.*", config.get("org.lockss.proxy.access.ip.include"));
  }

  public void testPlatformAccess2() throws Exception {
    // platform access not set, ui and proxy access set
    Properties props = new Properties();
    props.put("org.lockss.ui.access.ip.include", "1.2.3.0/22");
    props.put("org.lockss.proxy.access.ip.include", "1.2.3.0/21");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    Configuration config = mgr.getCurrentConfig();
    assertEquals("1.2.3.0/22", config.get("org.lockss.ui.access.ip.include"));
    assertEquals("1.2.3.0/21",
		 config.get("org.lockss.proxy.access.ip.include"));
  }

  public void testPlatformAccess3() throws Exception {
    // platform access set, ui and proxy access set
    Properties props = new Properties();
    props.put("org.lockss.platform.accesssubnet", "1.2.3.*");
    props.put("org.lockss.ui.access.ip.include", "1.2.3.0/22");
    props.put("org.lockss.proxy.access.ip.include", "1.2.3.0/21");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    Configuration config = mgr.getCurrentConfig();
    assertEquals("1.2.3.*;1.2.3.0/22",
		 config.get("org.lockss.ui.access.ip.include"));
    assertEquals("1.2.3.*;1.2.3.0/21",
		 config.get("org.lockss.proxy.access.ip.include"));
  }

  public void testGetRepositoryList() throws Exception {
    assertEmpty(mgr.getRepositoryList());
    ConfigurationUtil.setFromArgs("org.lockss.platform.diskSpacePaths",
				  "/foo/bar");
    assertEquals(ListUtil.list("local:/foo/bar"), mgr.getRepositoryList());
    ConfigurationUtil.setFromArgs("org.lockss.platform.diskSpacePaths",
				  "/foo/bar;/cache2");
    assertEquals(ListUtil.list("local:/foo/bar", "local:/cache2"),
		 mgr.getRepositoryList());
  }

  public void testPlatformSpace1() throws Exception {
    Properties props = new Properties();
    props.put("org.lockss.platform.diskSpacePaths", "/foo/bar");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    Configuration config = mgr.getCurrentConfig();
    assertEquals("/foo/bar", config.get("org.lockss.cache.location"));
    assertEquals("/foo/bar", config.get("org.lockss.history.location"));
  }

  public void testPlatformSpace2() throws Exception {
    Properties props = new Properties();
    props.put("org.lockss.platform.diskSpacePaths", "/a/b;/foo/bar");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    Configuration config = mgr.getCurrentConfig();
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
    Configuration config = mgr.getCurrentConfig();
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
    Configuration config = mgr.getCurrentConfig();
    assertEquals("55", config.get("org.lockss.foo"));
  }

  public void testPlatformDifferentVersionConfig() throws Exception {
    Properties props = new Properties();
    props.put(ConfigManager.PARAM_PLATFORM_VERSION, "321");
    props.put("org.lockss.foo", "22");
    props.put("123.org.lockss.foo", "55");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    Configuration config = mgr.getCurrentConfig();
    assertEquals("22", config.get("org.lockss.foo"));
  }

  public void testPlatformNoVersionConfig() throws Exception {
    Properties props = new Properties();
    props.put("org.lockss.foo", "11");
    props.put("123.org.lockss.foo", "55");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    Configuration config = mgr.getCurrentConfig();
    assertEquals("11", config.get("org.lockss.foo"));
  }

  public void testPlatformConfigDirSetup() throws Exception {
    String tmpdir = getTempDir().toString();
    Properties props = new Properties();
    props.put(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tmpdir);
    ConfigurationUtil.setCurrentConfigFromProps(props);
    String relConfigPath =
      Configuration.getParam(ConfigManager.PARAM_CONFIG_PATH,
			     ConfigManager.DEFAULT_CONFIG_PATH);
    File cdir = new File(tmpdir, relConfigPath);
    assertTrue(cdir.exists());
    Configuration config = Configuration.getCurrentConfig();
  }

  public void testConfigVersionProp() {
    assertEquals("org.lockss.config.fileVersion.foo",
		 ConfigManager.configVersionProp("foo"));
  }

  public void testWriteAndReadCacheConfigFile() throws Exception {
    String fname = "test-config";
    String tmpdir = getTempDir().toString();
    Properties props = new Properties();
    props.put(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tmpdir);
    ConfigurationUtil.setCurrentConfigFromProps(props);
    String relConfigPath =
      Configuration.getParam(ConfigManager.PARAM_CONFIG_PATH,
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
      Configuration.getParam(ConfigManager.PARAM_CONFIG_PATH,
			     ConfigManager.DEFAULT_CONFIG_PATH);
    File cdir = new File(tmpdir, relConfigPath);
    assertTrue(cdir.exists());
    Properties acprops = new Properties();
    acprops.put("foo.bar" , "12345");
    mgr.writeCacheConfigFile(acprops, ConfigManager.CONFIG_FILE_UI_IP_ACCESS,
			     "this is a header");

    Configuration config = Configuration.getCurrentConfig();
    assertNull(config.get("foo.bar"));
    ConfigurationUtil.setFromArgs(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
				  tmpdir);
    Configuration config2 = Configuration.getCurrentConfig();
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

    Configuration config = Configuration.getCurrentConfig();
    assertNull(config.get("org.lockss.au.auid.foo"));

    // should create file first time
    mgr.updateAuConfigFile(p, "org.lockss.au.fooauid");

    // reinstall should load au config file
    ConfigurationUtil.setFromArgs(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
				  tmpdir);
    config = Configuration.getCurrentConfig();
    assertEquals("111", config.get("org.lockss.au.fooauid.foo"));
    assertEquals("222", config.get("org.lockss.au.fooauid.bar"));
    assertEquals("333", config.get("org.lockss.au.fooauid.baz"));
    assertNull(config.get("org.lockss.au.auid.foo"));
    assertNull(config.get("org.lockss.au.auid.bar"));

    // add a different au, make sure they both get loaded
    p = new Properties();
    p.put("org.lockss.au.auid.foo", "11");
    p.put("org.lockss.au.auid.bar", "22");
    mgr.updateAuConfigFile(p, "org.lockss.au.auid");

    ConfigurationUtil.setFromArgs(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
				  tmpdir);
    config = Configuration.getCurrentConfig();
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

    ConfigurationUtil.setFromArgs(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
				  tmpdir);
    config = Configuration.getCurrentConfig();
    assertEquals("111", config.get("org.lockss.au.fooauid.foo"));
    assertEquals("222", config.get("org.lockss.au.fooauid.bar"));
    assertEquals(null, config.get("org.lockss.au.fooauid.baz"));
    assertEquals("11", config.get("org.lockss.au.auid.foo"));
    assertEquals("22", config.get("org.lockss.au.auid.bar"));
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

  public void testFromProperties() throws Exception {
    Properties props = new Properties();
    props.put("foo", "23");
    props.put("bar", "false");
    Configuration config = mgr.fromProperties(props);
    assertEquals(2, config.keySet().size());
    assertEquals("23", config.get("foo"));
    assertEquals("false", config.get("bar"));
  }
}
