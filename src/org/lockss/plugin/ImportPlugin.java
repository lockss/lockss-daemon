/*
 * $Id$
 */

/*

 Copyright (c) 2015 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.base.BasePlugin;
import org.lockss.util.Logger;
import org.lockss.util.urlconn.CacheSuccess;
import org.lockss.util.urlconn.HttpResultMap;

/**
 * The plugin used to import files into archival units.
 */
public class ImportPlugin extends BasePlugin {
  private static final Logger log = Logger.getLogger(ImportPlugin.class);

  public static final String PLUGIN_ID = "org.lockss.plugin.ImportPlugin";
  public static final String PLUGIN_KEY = "org|lockss|plugin|ImportPlugin";
  public static final String PREFIX = Configuration.PREFIX + "plugin.import.";

  // List of defining properties (only base_url for Import plugins).
  private static final List<ConfigParamDescr> configDescrs =
      new ArrayList<ConfigParamDescr>(Arrays.asList(ConfigParamDescr.BASE_URL));

  private String pluginName = "Import";
  private String currentVersion = "1";

  public ImportPlugin() {
  }

  protected ArchivalUnit createAu0(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    // create a new archival unit
    ImportArchivalUnit au = newImportArchivalUnit();

    // Now configure it.
    au.setConfiguration(auConfig);
    Configuration curConfig = ConfigManager.getCurrentConfig();
    au.setConfig(curConfig, ConfigManager.EMPTY_CONFIGURATION,
		 curConfig.differences(null));  // all differences
    return au;
  }

  protected ImportArchivalUnit newImportArchivalUnit() {
    return new ImportArchivalUnit(this);
  }

  /**
   * The global config has changed
   */
  protected void setConfig(Configuration newConfig,
			   Configuration prevConfig,
			   Configuration.Differences changedKeys) {
    if (changedKeys.contains(PREFIX)) {
      for (Iterator<ArchivalUnit> iter = getAllAus().iterator(); iter.hasNext();
	  ) {
	//  They should all be ImportArchivalUnits, but just in case...
	try {
	  ImportArchivalUnit au = (ImportArchivalUnit)iter.next();
	  au.setConfig(newConfig, prevConfig, changedKeys);
	} catch (Exception e) {
	  log.warning("setConfig: " + this, e);
	}
      }
    }
  }

  /**
   * ImportPlugin does not have a configuration. This is overridden to force no
   * implementation.
   */
  protected void setTitleConfigFromConfig(Configuration allTitles) {
    // No implementation.
  }

  public String getVersion() {
    return currentVersion;
  }

  public String getPluginName() {
    return pluginName;
  }

  /**
   * We only have one defining attribute, a base URL.
   */
  public List<ConfigParamDescr> getLocalAuConfigDescrs() {
    return configDescrs;
  }

  protected void initResultMap() {
    // Empty files are used to retract plugins; don't warn when collected.
    HttpResultMap hResultMap = new HttpResultMap();
    hResultMap.storeMapEntry(ContentValidationException.EmptyFile.class,
			     CacheSuccess.class);
    resultMap = hResultMap;
  }
}
