/*
 * $Id: SimulatedPlugin.java,v 1.1 2003-02-27 21:56:17 tal Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.simulated;

import java.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.util.*;
import org.lockss.test.*;

/**
 * SimulatedPlugin is a Plugin that simulates a website with
 * locally generated simulated content.
 */
public class SimulatedPlugin extends BasePlugin implements PluginTestable {
  static Logger log = Logger.getLogger("SimulatedPlugin");

  public static final String CONFIG_PROP_1 = "base_url";
  public static final String CONFIG_PROP_2 = "volume";

  private String pluginId = "SimulatedPlugin";
  private int initCtr = 0;
  private int stopCtr = 0;
  private Configuration auConfig;

  public SimulatedPlugin(){
  }

  /**
   * Called after plugin is loaded to give the plugin time to perform any
   * needed initializations
   */
  public void initPlugin() {
    initCtr++;
  }

  /**
   * Called when the application is stopping to allow the plugin to perform
   * any necessary tasks needed to cleanly halt
   */
  public void stopPlugin() {
    stopCtr++;
  }

  public String getPluginId() {
    return pluginId;
  }

  public void setPluginId(String id) {
    pluginId = id;
  }

  public String getVersion() {
    return "SimulatedVersion";
  }

  /**
   * Return the list of names of the Archival Units and volranges supported by
   * this plugin
   * @return a List of Strings
   */
  public List getSupportedAUNames() {
    return ListUtil.list("SimulatedSupportedTitle");
  }

  /**
   * Return the set of configuration properties required to configure
   * an archival unit for this plugin.
   * @return a List of strings which are the names of the properties for
   * which values are needed in order to configure an AU
   */
  public List getAUConfigProperties() {
    return ListUtil.list(CONFIG_PROP_1, CONFIG_PROP_2);
  }

  /**
   * Return the AU Id string for the ArchivalUnit handling the AU specified
   * by the given configuration. This must be completely determined by the
   * subset of the configuration info that's necessary to identify the AU.
   * @return the AUId string
   * @throws ArchivalUnit.ConfigurationException if the configuration is
   * illegal in any way.
   */
  public String getAUIdFromConfig(Configuration config) 
      throws ArchivalUnit.ConfigurationException {
    return config.get(CONFIG_PROP_1) + "|" + config.get(CONFIG_PROP_2);
  }

  /**
   * Create an ArchivalUnit for the AU specified by the configuration.
   * @param auConfig Configuration object with values for all properties
   * returned by {@link #getAUConfigProperties()}
   */
  public ArchivalUnit createAU(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    log.debug("createAU(" + auConfig + ")");
    ArchivalUnit au = new SimulatedArchivalUnit(this);
    au.setConfiguration(auConfig);
    return au;
  }

  // SimulatedPlugin methods, not part of Plugin interface

  public int getInitCtr() {
    return initCtr;
  }

  public int getStopCtr() {
    return stopCtr;
  }

  public void registerArchivalUnit(ArchivalUnit au) {
    auMap.put(au.getAUId(), au);
  }

  public void unregisterArchivalUnit(ArchivalUnit au) {
    auMap.remove(au.getAUId());
  }
}
