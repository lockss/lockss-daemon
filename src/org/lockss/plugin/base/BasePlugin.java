/*
 * $Id: BasePlugin.java,v 1.14 2003-11-07 04:11:59 clairegriffin Exp $
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

package org.lockss.plugin.base;

import java.util.*;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * Abstract base class for Plugins.  Plugins are encouraged to extend this
 * class to get some common Plugin functionality.
 */
public abstract class BasePlugin
    implements Plugin {
  static Logger log = Logger.getLogger("BasePlugin");

  static final protected String CM_NAME_KEY = "plugin_name";
  static final protected String CM_VERSION_KEY = "plugin_version";
  static final protected String CM_TITLE_SPEC_KEY = "title_spec";
  static final protected String CM_CONFIG_PROPS_KEY = "au_config_props";
  static final protected String CM_DEFINING_CONFIG_PROPS_KEY =
      "au_defining_props";
  protected LockssDaemon theDaemon;
  protected Collection aus = new ArrayList();
  protected Map titleConfig;
  protected ExternalizableMap configurationMap = new ExternalizableMap();

  /**
   * Must invoke this constructor in plugin subclass.
   */
  protected BasePlugin() {
  }

  public void initPlugin(LockssDaemon daemon) {
    theDaemon = daemon;
    String[][] titles = (String[][]) configurationMap.getMapElement(
        CM_TITLE_SPEC_KEY);
    if (titles != null) {
      setTitleConfig(titles);
    }
  }

  public void stopPlugin() {
  }

  public String getPluginName() {
    return configurationMap.getString(CM_NAME_KEY, "NO_NAME");
  }

  public String getVersion() {
    return configurationMap.getString(CM_VERSION_KEY, "UNKNOWN VERSION");
  }

  public List getAuConfigProperties() {
    return (List) configurationMap.getCollection(CM_CONFIG_PROPS_KEY, null);
  }

  public Collection getDefiningConfigKeys() {
    return configurationMap.getCollection(CM_DEFINING_CONFIG_PROPS_KEY, null);
  }

  /**
   * Default implementation collects keys from titleConfig map.
   * @return a List
   */
  public List getSupportedTitles() {
    if (titleConfig == null) {
      return Collections.EMPTY_LIST;
    }
    List res = new ArrayList(20);
    for (Iterator iter = titleConfig.keySet().iterator(); iter.hasNext(); ) {
      res.add( (String) iter.next());
    }
    return res;
  }

  /**
   * Default implementation looks in titleConfig map.
   * @param title the title String
   * @return a Configuration (null if none)
   */
  public Configuration getConfigForTitle(String title) {
    if (titleConfig == null) {
      return null;
    }
    return (Configuration) titleConfig.get(title);
  }

  protected void setTitleConfig(Map titleConfig) {
    this.titleConfig = titleConfig;
  }

  /** Set title config map from array of arrays of
   * [title, key1, val1, keyn, valn]
   * @param titleSpecs the array of arrays
   */
  protected void setTitleConfig(String titleSpecs[][]) {
    Map map = new HashMap();
    for (int tix = 0; tix < titleSpecs.length; tix++) {
      String titleSpec[] = titleSpecs[tix];
      String title = titleSpec[0];
      Configuration config = ConfigManager.newConfiguration();
      for (int pix = 1; pix < titleSpec.length; pix += 2) {
        String key = titleSpec[pix];
        String val = titleSpec[pix + 1];
        config.put(key, val);
      }
      map.put(title, config);
    }
    setTitleConfig(map);
  }

  protected ExternalizableMap getConfigurationMap() {
    return configurationMap;
  }

  // for now use the plugin's class name
  // tk - this will have to change to account for versioning
  public String getPluginId() {
    return this.getClass().getName();
  }

  public Collection getAllAus() {
    log.debug2("getAllAus: aus: " + aus);
    return aus;
  }

  public ArchivalUnit configureAu(Configuration config, ArchivalUnit au) throws
      ArchivalUnit.ConfigurationException {
    if (au != null) {
      au.setConfiguration(config);
    }
    else {
      au = createAu(config);
      aus.add(au);
    }
    return au;
  }

  /**
   * Return the LockssDaemon instance
   * @return the LockssDaemon instance
   */
  public LockssDaemon getDaemon() {
    return theDaemon;
  }

  public CachedUrlSet makeCachedUrlSet(ArchivalUnit owner,
                                       CachedUrlSetSpec cuss) {
    return new BaseCachedUrlSet(owner, cuss);
  }

  public CachedUrl makeCachedUrl(CachedUrlSet owner, String url) {
    return new BaseCachedUrl(owner, url);
  }

  public UrlCacher makeUrlCacher(CachedUrlSet owner, String url) {
    return new BaseUrlCacher(owner, url);
  }
}