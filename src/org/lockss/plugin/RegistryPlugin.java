/*
 * $Id: RegistryPlugin.java,v 1.2 2004-08-18 00:14:57 tlipkis Exp $
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

import java.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.base.BasePlugin;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.ListUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.app.LockssDaemon;

/**
 * <p>RegistryPlugin: A plugin that archives other plugins.</p>
 * @version 1.0
 */

public class RegistryPlugin extends BasePlugin {
  private static String PLUGIN_NAME = "Registry";
  private static String CURRENT_VERSION = "1";

  // List of defining properties (only base_url for Registry plugins)
  private static final List m_auConfigDescrs =
    ListUtil.list(ConfigParamDescr.BASE_URL);

  public RegistryPlugin() {
  }

  public ArchivalUnit createAu(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    // create a new archival unit
    ArchivalUnit au = new RegistryArchivalUnit(this);

    // Now configure it.
    au.setConfiguration(auConfig);

    return au;
  }

  /**
   * RegistryPlugin does not have a configuration.  This is overridden
   * to force no implementation.
   */
  protected void setConfig(Configuration newConfig,
			   Configuration prevConfig,
			   Configuration.Differences changedKeys) {
    // No implementation.
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
  public List getAuConfigDescrs() {
    return m_auConfigDescrs;
  }
}
