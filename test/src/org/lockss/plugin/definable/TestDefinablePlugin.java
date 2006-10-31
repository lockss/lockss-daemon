/*
 * $Id: TestDefinablePlugin.java,v 1.12 2006-10-31 07:01:06 thib_gc Exp $
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
package org.lockss.plugin.definable;

import java.util.*;

import org.lockss.app.LockssDaemon;
import org.lockss.config.*;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.base.BaseArchivalUnit;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.HttpResultMap;

/**
 * <p>TestConfigurablePlugin: test case for ConfigurablePlugin</p>
 * @author Claire Griffin
 * @version 1.0
 */

public class TestDefinablePlugin extends LockssTestCase {
  static final String DEFAULT_PLUGIN_VERSION = "1";

  private DefinablePlugin definablePlugin = null;

  protected void setUp() throws Exception {
    super.setUp();
    definablePlugin = new DefinablePlugin();
  }

  protected void tearDown() throws Exception {
    definablePlugin = null;
    super.tearDown();
  }

  public void testCreateAu() throws ArchivalUnit.ConfigurationException {
    Properties p = new Properties();
    p.setProperty("TEST_KEY", "TEST_VALUE");
    p.setProperty(ConfigParamDescr.BASE_URL.getKey(), "http://www.example.com/");
     p.setProperty(BaseArchivalUnit.KEY_PAUSE_TIME,"10000");
    List rules = ListUtil.list("1,\"http://www.example.com\"");
    ExternalizableMap map = definablePlugin.getDefinitionMap();
    map.putString(DefinablePlugin.KEY_PLUGIN_NAME, "testplugin");
    map.putCollection(DefinablePlugin.KEY_PLUGIN_CONFIG_PROPS,
                      Collections.EMPTY_LIST);
    map.putCollection(DefinableArchivalUnit.KEY_AU_CRAWL_RULES,rules);
    map.putString("au_start_url", "http://www.example.com/");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    Configuration auConfig = CurrentConfig.getCurrentConfig();
    ArchivalUnit actualReturn = definablePlugin.createAu(auConfig);
    assertTrue(actualReturn instanceof DefinableArchivalUnit);
    assertEquals("configuration", auConfig, actualReturn.getConfiguration());
  }

  public void testGetAuConfigProperties() {
    Collection expectedReturn = ListUtil.list("Item1", "Item2");
    ExternalizableMap map = definablePlugin.getDefinitionMap();
    map.putCollection(DefinablePlugin.KEY_PLUGIN_CONFIG_PROPS,
                      expectedReturn);

    List actualReturn = definablePlugin.getLocalAuConfigDescrs();
    assertIsomorphic("return value", expectedReturn, actualReturn);
  }

  public void testGetConfigurationMap() {
    ExternalizableMap expectedReturn = definablePlugin.definitionMap;
    ExternalizableMap actualReturn = definablePlugin.getDefinitionMap();
    assertEquals("return value", expectedReturn, actualReturn);
  }

  public void testGetPluginName() {
    // no name set
    String expectedReturn = "DefinablePlugin";
    String actualReturn = definablePlugin.getPluginName();
    assertEquals("return value", expectedReturn, actualReturn);

    // set the name
    expectedReturn = "TestPlugin";
    ExternalizableMap map = definablePlugin.getDefinitionMap();
    map.putString(DefinablePlugin.KEY_PLUGIN_NAME, expectedReturn);
    actualReturn = definablePlugin.getPluginName();
    assertEquals("return value", expectedReturn, actualReturn);

  }

  public void testGetVersion() {
    // no version set
    String expectedReturn = DEFAULT_PLUGIN_VERSION;
    String actualReturn = definablePlugin.getVersion();
    assertEquals("return value", expectedReturn, actualReturn);

    // set the version
    expectedReturn = "Version 1.0";
    ExternalizableMap map = definablePlugin.getDefinitionMap();
    map.putString(DefinablePlugin.KEY_PLUGIN_VERSION, expectedReturn);
    actualReturn = definablePlugin.getVersion();
    assertEquals("return value", expectedReturn, actualReturn);

  }

  public void testGetPluginId() throws Exception {
    LockssDaemon daemon = getMockLockssDaemon();
    String extMapName = null;
    try {
      definablePlugin.initPlugin(daemon, extMapName);
      assertNull(definablePlugin.mapName);
    }
    catch (Exception npe) {
    }

    extMapName = "org.lockss.test.MockConfigurablePlugin";
    definablePlugin.initPlugin(daemon, extMapName);
    assertEquals("org.lockss.test.MockConfigurablePlugin",
                 definablePlugin.getPluginId());
  }

  public void testInitPlugin() throws Exception {
    LockssDaemon daemon = getMockLockssDaemon();
    String extMapName = null;
    try {
      definablePlugin.initPlugin(daemon, extMapName);
      assertNull(definablePlugin.mapName);
    }
    catch (NullPointerException npe) {
    }
    assertEquals("DefinablePlugin", definablePlugin.getPluginName());

    extMapName = "org.lockss.test.MockConfigurablePlugin";
    definablePlugin.initPlugin(daemon, extMapName);
    assertEquals("Absinthe Literary Review",
                 definablePlugin.getPluginName());
    assertEquals("1", definablePlugin.getVersion());

    // check some other field
    StringBuffer sb = new StringBuffer("\"%sarchives%02d.htm\", ");
    sb.append(ConfigParamDescr.BASE_URL.getKey());
    sb.append(", ");
    sb.append(ConfigParamDescr.YEAR.getKey());
    ExternalizableMap map = definablePlugin.getDefinitionMap();
    assertEquals(sb.toString(),
                 map.getString(DefinableArchivalUnit.KEY_AU_START_URL, null));

  }

  public void testInstallCacheExceptionHandler() {
    DefinablePlugin plugin = new DefinablePlugin();
    ExternalizableMap map = plugin.getDefinitionMap();
    String name = new MockHttpResultHandler().getClass().getName();
    // test using a special class
    map.putString(DefinablePlugin.KEY_EXCEPTION_HANDLER,name);
    plugin.initResultMap();
    assertTrue(plugin.getCacheResultHandler() instanceof MockHttpResultHandler);

  }

  public void testInstallCacheExceptionEntries() throws Exception {
    DefinablePlugin plugin = new DefinablePlugin();
    ExternalizableMap map = plugin.getDefinitionMap();
    plugin.initResultMap();
    // nothing installed should give the default
    String name = "org.lockss.util.urlconn.CacheException$NoRetryDeadLinkException";
    Class expected = Class.forName(name);
    Class found =( (HttpResultMap) plugin.getCacheResultMap()).getExceptionClass(404);
    assertEquals(expected, found);

    // test using a single entry
    name = "org.lockss.util.urlconn.CacheException$RetryDeadLinkException";
    map.putCollection(DefinablePlugin.KEY_EXCEPTION_LIST,
        ListUtil.list("404="+name));
    plugin.initResultMap();
    expected = Class.forName(name);
    found =( (HttpResultMap) plugin.getCacheResultMap()).getExceptionClass(404);
    assertEquals(expected, found);
  }

//   static public class MyMockHttpResultHandler implements CacheResultHandler {
//    public MyMockHttpResultHandler() {
//    }

//    public void init(CacheResultMap crmap) {
//      ((HttpResultMap)crmap).storeMapEntry(200, this.getClass());
//    }

//    public CacheException handleResult(int code,
//                                       LockssUrlConnection connection) {
//      return null;
//    }

//  }
}
