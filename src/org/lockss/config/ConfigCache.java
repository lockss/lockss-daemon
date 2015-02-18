/*
 * $Id$
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.config;

import java.util.*;

import org.lockss.util.*;

/**
 * A memory cache of files used to populate our Configuration.
 */
public class ConfigCache {
  // MUST pass in explicit log level to avoid recursive call back to
  // Configuration to get Config log level.  (Others should NOT do this.)
  private static Logger log =
    Logger.getLoggerWithInitialLevel("ConfigCache",
				     Logger.getInitialDefaultLevel());

  private ConfigManager configMgr;
  private Map m_configMap = new HashMap();

  public ConfigCache(ConfigManager configMgr) {
    this.configMgr = configMgr;
  }

  /**
   * Return the existing ConfigFile for the url or file, if any, else null
   * @return the ConfigFile or null if no such
   */
  public ConfigFile get(String url) {
    return (ConfigFile)m_configMap.get(url);
  }

  /**
   * Find or create a ConfigFile for the url
   * @return an exisiting or new ConfigFile of the type appropriate for the
   * URL
   */
  public synchronized ConfigFile find(String url) {
    ConfigFile cf = get(url);
    if (cf == null) {
      // doesn't yet exist in the cache, add it.
      log.debug2("Adding " + url);
      BaseConfigFile bcf;
      if (UrlUtil.isHttpUrl(url)) {
	bcf = new HTTPConfigFile(url);
      } else if (UrlUtil.isJarUrl(url)) {
	bcf = new JarConfigFile(url);
      } else {
	bcf = new FileConfigFile(url);
      }
      bcf.setConfigManager(configMgr);
      m_configMap.put(url, bcf);
      cf = bcf;
    }
    return cf;
  }

//   public synchronized void remove(String url) {
//     m_configMap.remove(url);
//   }

  int size() {
    return m_configMap.size();
  }
}
