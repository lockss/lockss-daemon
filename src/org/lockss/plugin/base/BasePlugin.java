/*
 * $Id: BasePlugin.java,v 1.11 2003-09-17 06:09:59 troberts Exp $
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
public abstract class BasePlugin implements Plugin {
  static Logger log = Logger.getLogger("BasePlugin");
  protected LockssDaemon theDaemon;
  protected Collection aus = new ArrayList();
  protected Map titleConfig;

  /**
   * Must invoke this constructor in plugin subclass.
   */
  protected BasePlugin() {
  }

  public void initPlugin(LockssDaemon daemon) {
    theDaemon = daemon;
  }

  public void stopPlugin() {
  }

  /**
   * Default implementation collects keys from titleConfig map.
   */
  public List getSupportedTitles() {
    if (titleConfig == null) {
      return Collections.EMPTY_LIST;
    }
    List res = new ArrayList(20);
    for (Iterator iter = titleConfig.keySet().iterator(); iter.hasNext(); ) {
      res.add((String)iter.next());
    }
    return res;
  }

  /**
   * Default implementation looks in titleConfig map.
   */
  public Configuration getConfigForTitle(String title) {
    if (titleConfig == null) {
      return null;
    }
    return (Configuration)titleConfig.get(title);
  }

  protected void setTitleConfig(Map titleConfig) {
    this.titleConfig = titleConfig;
  }

  /** Set title config map from array of arrays of
   * [title, key1, val1, keyn, valn]
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

  // for now use the plugin's class name
  // tk - this will have to change to account for versioning
  public String getPluginId() {
    return this.getClass().getName();
  }

  public Collection getAllAUs() {
    log.debug2("getAllAus: aus: " + aus);
    return aus;
  }

  public ArchivalUnit configureAU(Configuration config, ArchivalUnit au)
      throws ArchivalUnit.ConfigurationException {
    if (au != null) {
      au.setConfiguration(config);
    } else {
      au = createAU(config);
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
    return new GenericFileCachedUrlSet(owner, cuss);
  }

  public CachedUrl makeCachedUrl(CachedUrlSet owner, String url) {
    return new GenericFileCachedUrl(owner, url);
  }

  public UrlCacher makeUrlCacher(CachedUrlSet owner, String url) {
    return new GenericFileUrlCacher(owner, url);
  }
}
