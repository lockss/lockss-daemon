/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.base.BasePlugin;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/**
 * <p>RegistryPlugin: A plugin that archives other plugins.</p>
 * @version 1.0
 */

public class RegistryPlugin extends BasePlugin {
  protected static final Logger log = Logger.getLogger("RegistryPlugin");

  private static String PLUGIN_NAME = "Plugin Registry Plugin";
  private static String CURRENT_VERSION = "2";

  public static final String PLUGIN_ID = "org.lockss.plugin.RegistryPlugin";
  public static final String PREFIX =
    Configuration.PREFIX + "plugin.registries.";

  // List of defining properties (only base_url for Registry plugins)
  private static final List<ConfigParamDescr> m_auConfigDescrs =
    ListUtil.list(ConfigParamDescr.BASE_URL);

  public RegistryPlugin() {
  }

  protected ArchivalUnit createAu0(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    // create a new archival unit
    RegistryArchivalUnit au = newRegistryArchivalUnit();

    // Now configure it.
    au.setConfiguration(auConfig);
    Configuration curConfig = ConfigManager.getCurrentConfig();
    au.setConfig(curConfig, ConfigManager.EMPTY_CONFIGURATION,
		 curConfig.differences(null));  // all differences
    return au;
  }

  protected RegistryArchivalUnit newRegistryArchivalUnit() {
    return new RegistryArchivalUnit(this);
  }

  /**
   * The global config has changed
   */
  protected void setConfig(Configuration newConfig,
			   Configuration prevConfig,
			   Configuration.Differences changedKeys) {
    if (changedKeys.contains(PREFIX)) {
      for (Iterator iter = getAllAus().iterator(); iter.hasNext(); ) {
	//  They should all be RegistryArchivalUnits, but just in case...
	try {
	  RegistryArchivalUnit au = (RegistryArchivalUnit)iter.next();
	  au.setConfig(newConfig, prevConfig, changedKeys);
	} catch (Exception e) {
	  log.warning("setConfig: " + this, e);
	}
      }
    }
  }

  /**
   * RegistryPlugin does not have a configuration.  This is overridden
   * to force no implementation.
   */
  protected void setTitleConfigFromConfig(Configuration allTitles) {
    // No implementation.
  }

  public String getVersion() {
    return CURRENT_VERSION;
  }

  public String getPluginName() {
    return PLUGIN_NAME;
  }

  /**
   * We only have one defining attribute, a base URL.
   */
  public List<ConfigParamDescr> getLocalAuConfigDescrs() {
    return m_auConfigDescrs;
  }

  protected void initResultMap() {
    // Empty files are used to retract plugins; don't warn when collected.
    HttpResultMap hResultMap = new HttpResultMap();
    hResultMap.storeMapEntry(ContentValidationException.EmptyFile.class,
			     CacheSuccess.class);
    resultMap = hResultMap;
  }
}
