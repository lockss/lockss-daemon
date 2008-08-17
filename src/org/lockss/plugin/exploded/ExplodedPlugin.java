/*
 * $Id: ExplodedPlugin.java,v 1.4 2008-08-17 08:45:41 tlipkis Exp $
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

package org.lockss.plugin.exploded;

import java.util.*;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.base.BasePlugin;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;

/**
 * <p>ExplodedPlugin: A plugin for AUs full of files ingested by
 * explosion rather than crawling.
 * @version 1.0
 * @author David Rosenthal
 */

public class ExplodedPlugin extends BasePlugin {
  protected static final Logger log = Logger.getLogger("ExplodedPlugin");

  private static String PLUGIN_NAME = "Exploded Plugin";
  private static String CURRENT_VERSION = "1";

  public static final String PREFIX =
    Configuration.PREFIX + "plugin.exploded.";

  // List of defining properties (only base_url for Exploded plugins)
  private static final List m_auConfigDescrs =
    Collections.singletonList(ConfigParamDescr.BASE_URL);

  public ExplodedPlugin() {
  }

  protected ArchivalUnit createAu0(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    // create a new archival unit
    ArchivalUnit au = new ExplodedArchivalUnit(this);

    // Now configure it.
    au.setConfiguration(auConfig);

    return au;
  }

  /**
   * ExplodedPlugin does not have a configuration.  This is overridden
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
  public List getLocalAuConfigDescrs() {
    return m_auConfigDescrs;
  }
}
