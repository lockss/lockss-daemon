/*

Copyright (c) 2000-2022 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.test;

import java.io.*;
import java.util.*;

import org.apache.commons.lang3.tuple.*;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.*;
import org.lockss.state.*;
import org.lockss.extractor.*;

/** Performs basic well-formedness tests on one or more plugins.  The list
 * of plugins may be supplied as a semicolon-separated list in the System
 * property org.lockss.test.TestPluginNames or, if invoked directly (i.e.,
 * not as a junit test), on the command line.  If a plugin jar name is
 * supplied (with org.lockss.test.TestPluginNames or -pj on the command
 * line) it is loaded as a normal packaged plugin jar and the plugins are
 * assumed to be contained in it.
 */
public final class PluginWellformednessTests extends LockssTestCase {
  static Logger log = Logger.getLogger("PluginWellformednessTests");

  /** The System property under which this class expects to find a
   * semicolon-separated list of plugin names. */
  public static String PLUGIN_NAME_PROP = "org.lockss.test.TestPluginNames";
  public static String PLUGIN_JAR_PROP = "org.lockss.test.TestPluginJar";

  protected MockLockssDaemon daemon;
  protected MyPluginManager pluginMgr;
  protected String pluginName;
  protected Plugin plugin;
  protected boolean jarLoaded = false;

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();

    daemon = getMockLockssDaemon();
    daemon.suppressStartAuManagers(false);
    pluginMgr = new MyPluginManager();
    daemon.setPluginManager(pluginMgr);
    pluginMgr.initService(daemon);
    pluginMgr.startService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  protected MockLockssDaemon newMockLockssDaemon() {
    return new MyMockLockssDaemon();
  }

  public class MyMockLockssDaemon extends MockLockssDaemon {
    protected MyMockLockssDaemon() {
      super();
    }

    public NodeManager getNodeManager(ArchivalUnit au) {
      // try {
      // 	return super.getNodeManager(au);
      // } catch (IllegalArgumentException e) {
	return (NodeManager)newAuManager(LockssDaemon.NODE_MANAGER, au);
      // }
    }
  }

  protected Plugin getPlugin() throws IOException {
    if (plugin == null) {
      String jarProp = System.getProperty(PLUGIN_JAR_PROP);
      if (StringUtil.isNullString(jarProp)) {
	plugin = PluginTestUtil.findPlugin(pluginName);
      } else {
	if (!jarLoaded) {
	  loadJar(jarProp);
	  jarLoaded = true;
	}	  
	plugin = pluginMgr.getPluginFromId(pluginName);
      }
    }
    return plugin;
  }

  protected void loadJar(String jarName) throws IOException {
    Map infoMap = new HashMap();
    File jarFile = new File(jarName);
    String jarUrl = FileTestUtil.urlOfFile(jarName);
    MockCachedUrl mcu = new MockCachedUrl(jarUrl, jarName, false);
    MockArchivalUnit mau = new MockArchivalUnit();
    pluginMgr.loadPluginsFromJar(jarFile, jarUrl, mau, mcu, infoMap);
    pluginMgr.installPlugins(infoMap);
  }

  protected Configuration getSampleAuConfig() throws IOException {
    Configuration config = ConfigManager.newConfiguration();
    for (ConfigParamDescr descr : getPlugin().getAuConfigDescrs()) {
      config.put(descr.getKey(), descr.getSampleValue());
    }
    return config;
  }

  protected ArchivalUnit createAu()
      throws ArchivalUnit.ConfigurationException, IOException {
    ArchivalUnit au = PluginTestUtil.createAu(pluginName, getSampleAuConfig());
    daemon.setNodeManager(new MockNodeManager(), au);
    daemon.setLockssRepository(new MockLockssRepository(), au);
    return au;
  }

  protected static String URL1 = "http://example.com/foo/bar";

  protected List<String> getSupportedMimeTypes() {
    return ListUtil.list("text/html", "text/css");
  }

  /** This test expects to find a semicolon-separated list in the System
   * property org.lockss.test.TestPluginNames .  It runs {@link
   * #testWellFormed(String)} on each one. */
  public void testPlugins() throws Exception {
    String args = System.getProperty(PLUGIN_NAME_PROP);
    if (StringUtil.isNullString(args)) {
      return;
    }
    List<Pair<String,String>> failed = new ArrayList<Pair<String,String>>();
    for (String pluginName : (List<String>)StringUtil.breakAt(args, ";")) {
      try {
 	System.err.println("Testing plugin: " + pluginName);
	resetAndTest(pluginName);
      } catch (PluginFailedToLoadException e) {
	log.error("Plugin " + pluginName + " failed");
	failed.add(new ImmutablePair(pluginName, e.toString()));
      } catch (Exception | ExceptionInInitializerError e) {
	log.error("Plugin " + pluginName + " failed", e);
	failed.add(new ImmutablePair(pluginName, e.getMessage()));
      }
    }
    if (!failed.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      sb.append(StringUtil.numberOfUnits(failed.size(), "plugin") + " failed:");
      for (Pair<String,String> f : failed) {
	sb.append("\n  ");
	sb.append(f.getLeft());
	sb.append("\n    ");
	sb.append(f.getRight());
      }
      fail(sb.toString());
    }
  }

  // Hack to reset the local state in order to test all the plugins in one
  // junit invocation (which is 20-50 times faster than invoking junit for
  // each one).

  void resetAndTest(String pluginName) throws Exception {
    this.pluginName = pluginName;
    plugin = null;
    testWellFormed(pluginName);
  }

  /** Load the named plugin, create an AU using sample parameters and
   * access all of its elements to ensure all the patterns are well formed
   * and the factories are loadable and runnable.
   */
  public void testWellFormed(String pluginName) throws Exception {
    Plugin plugin = getPlugin();
    if (plugin == null) {
      throw new PluginFailedToLoadException();
    }
    validatePlugin(plugin);
  }

  public void validatePlugin(Plugin plugin) throws Exception {
    PluginValidator pv = new PluginValidator(daemon, pluginName, plugin);
    pv.validatePlugin();
  }

  private class MyPluginManager extends PluginManager {
    CachedUrl cu;
    protected void processOneRegistryJar(CachedUrl cu, String url,
					 ArchivalUnit au, Map tmpMap) {
      super.processOneRegistryJar(cu, url, au, tmpMap);
    }

    protected void loadPluginsFromJar(File jarFile, String url,
				      ArchivalUnit au, CachedUrl cu,
				      Map tmpMap) {
      super.loadPluginsFromJar(jarFile, url, au, cu, tmpMap);
    }
    
    void installPlugins(Map<String,PluginInfo> map) {
      for (Map.Entry<String,PluginInfo> entry : map.entrySet()) {
	String key = entry.getKey();
	log.debug2("Adding to plugin map: " + key);
	PluginInfo info = entry.getValue();
	Plugin newPlug = info.getPlugin();
	setPlugin(key, newPlug);
      }
    }      
  }

  public static class PluginFailedToLoadException extends Exception {
  }

  public static void main(String[] argv) {
    if (argv.length > 0) {
      int ix = 0;
      try {
	for (ix = 0; ix < argv.length; ix++) {
	  String arg = argv[ix];
	  if (!arg.startsWith("-")) {
	    break;
	  }
	  if (arg.equals("-pj")) {
	    String jarName = argv[++ix];
	    System.setProperty(PLUGIN_JAR_PROP, jarName);
	  } else {
	    usage();
	  }
	}
      } catch (ArrayIndexOutOfBoundsException e) {
	usage();
      }
      String pluginNames =
	StringUtil.separatedString(Arrays.copyOfRange(argv, ix, argv.length-1),
				   ";");
      System.setProperty(PLUGIN_NAME_PROP, pluginNames);

    }
    junit.textui.TestRunner.main(new String[] {
	PluginWellformednessTests.class.getName() });
  }

  private static void usage() {
    PrintStream o = System.out;
    o.println("Usage: java PluginWellformednessTests " +
	      " [-pj plugin_jar] plugin_id_1 plugin_id_2 ...");
    o.println("   -pj plugin_jar     packaged plugin jar");
    System.exit(2);
  }
}
