/*
 * $Id: TestConfigurablePlugin.java,v 1.6 2004-01-31 22:54:29 tlipkis Exp $
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
package org.lockss.plugin.configurable;

import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.daemon.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.plugin.base.*;

/**
 * <p>TestConfigurablePlugin: test case for ConfigurablePlugin</p>
 * @author Claire Griffin
 * @version 1.0
 */

public class TestConfigurablePlugin extends LockssTestCase {
  static final String DEFAULT_PLUGIN_VERSION = "1";

  private ConfigurablePlugin configurablePlugin = null;

  protected void setUp() throws Exception {
    super.setUp();
    configurablePlugin = new ConfigurablePlugin();
  }

  protected void tearDown() throws Exception {
    configurablePlugin = null;
    super.tearDown();
  }

  public void testCreateAu() throws ArchivalUnit.ConfigurationException {
    Properties p = new Properties();
    p.setProperty("TEST_KEY", "TEST_VALUE");
    p.setProperty(ConfigParamDescr.BASE_URL.getKey(), "http://www.example.com/");
     p.setProperty(BaseArchivalUnit.PAUSE_TIME_KEY,"10000");
    List rules = ListUtil.list("1\nhttp://www.example.com");
    ExternalizableMap map = configurablePlugin.getConfigurationMap();
    map.putCollection(ConfigurableArchivalUnit.CM_AU_RULES_KEY,rules);
    map.putString("au_start_url", "http://www.example.com/");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    Configuration auConfig = Configuration.getCurrentConfig();
    ArchivalUnit actualReturn = configurablePlugin.createAu(auConfig);
    assertTrue(actualReturn instanceof ConfigurableArchivalUnit);
    assertEquals("configuration", auConfig, actualReturn.getConfiguration());
  }

  public void testGetAuConfigProperties() {
    Collection expectedReturn = ListUtil.list("Item1", "Item2");
    ExternalizableMap map = configurablePlugin.getConfigurationMap();
    map.putCollection(ConfigurablePlugin.CM_CONFIG_PROPS_KEY,
                      expectedReturn);

    List actualReturn = configurablePlugin.getAuConfigDescrs();
    assertIsomorphic("return value", expectedReturn, actualReturn);
  }

  public void testGetConfigurationMap() {
    ExternalizableMap expectedReturn = configurablePlugin.configurationMap;
    ExternalizableMap actualReturn = configurablePlugin.getConfigurationMap();
    assertEquals("return value", expectedReturn, actualReturn);
  }

  public void testGetPluginName() {
    // no name set
    String expectedReturn = "ConfigurablePlugin";
    String actualReturn = configurablePlugin.getPluginName();
    assertEquals("return value", expectedReturn, actualReturn);

    // set the name
    expectedReturn = "TestPlugin";
    ExternalizableMap map = configurablePlugin.getConfigurationMap();
    map.putString(ConfigurablePlugin.CM_NAME_KEY, expectedReturn);
    actualReturn = configurablePlugin.getPluginName();
    assertEquals("return value", expectedReturn, actualReturn);

  }

  public void testGetVersion() {
    // no version set
    String expectedReturn = DEFAULT_PLUGIN_VERSION;
    String actualReturn = configurablePlugin.getVersion();
    assertEquals("return value", expectedReturn, actualReturn);

    // set the version
    expectedReturn = "Version 1.0";
    ExternalizableMap map = configurablePlugin.getConfigurationMap();
    map.putString(ConfigurablePlugin.CM_VERSION_KEY, expectedReturn);
    actualReturn = configurablePlugin.getVersion();
    assertEquals("return value", expectedReturn, actualReturn);

  }

  public void testGetPluginId() {
    LockssDaemon daemon = getMockLockssDaemon();
    String extMapName = null;
    try {
      configurablePlugin.initPlugin(daemon, extMapName);
      assertNull(configurablePlugin.mapName);
    }
    catch (NullPointerException npe) {
    }
    assertEquals("org.lockss.plugin.configurable.ConfigurablePlugin",
                 configurablePlugin.getPluginId());

    extMapName = "org.lockss.plugin.absinthe.AbsinthePlugin";
    configurablePlugin.initPlugin(daemon, extMapName);
    assertEquals("org.lockss.plugin.absinthe.AbsinthePlugin",
                 configurablePlugin.getPluginId());
  }

  public void testInitPlugin() {
    LockssDaemon daemon = getMockLockssDaemon();
    String extMapName = null;
    try {
      configurablePlugin.initPlugin(daemon, extMapName);
      assertNull(configurablePlugin.mapName);
    }
    catch (NullPointerException npe) {
    }
    assertEquals("ConfigurablePlugin", configurablePlugin.getPluginName());

    extMapName = "org.lockss.plugin.configurable.AbsinthePlugin";
    configurablePlugin.initPlugin(daemon, extMapName);
    assertEquals("Absinthe Literary Review",
                 configurablePlugin.getPluginName());
    assertEquals("Pre-release", configurablePlugin.getVersion());

    // check some other field
    StringBuffer sb = new StringBuffer("%sarchives%02d.htm\n");
    sb.append(ConfigParamDescr.BASE_URL.getKey());
    sb.append("\n");
    sb.append(ConfigParamDescr.YEAR.getKey());
    ExternalizableMap map = configurablePlugin.getConfigurationMap();
    assertEquals(sb.toString(),
                 map.getString(ConfigurableArchivalUnit.CM_AU_START_URL_KEY, null));

  }

}
