/*
 * $Id: ConfigCache.java,v 1.7 2005-03-18 09:09:13 smorabito Exp $
 */

/*

Copyright (c) 2001-2003 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;

import org.apache.commons.collections.map.LinkedMap;
import org.lockss.util.*;

/**
 * A memory cache of files used to populate our Configuration.
 * Maintains two lists of files; remote and local.  This allows all
 * the remote files to be parsed before any of the local files.
 */
public class ConfigCache {
  // MUST pass in explicit log level to avoid recursive call back to
  // Configuration to get Config log level.  (Others should NOT do this.)
  private static Logger log =
    Logger.getLoggerWithInitialLevel("ConfigCache",
				     Logger.getInitialDefaultLevel());

  private LinkedMap m_configMap = new LinkedMap();


  /**
   * Retrieve or create the ConfigFile for the url or file, loading or
   * reloading it if necessary.
   */
  public ConfigFile get(String url) throws IOException {
    ensureLoaded(url);
    ConfigFile confFile = (ConfigFile)m_configMap.get(url);
    return confFile;
  }

  /**
   * Return the ConfigFile for the url or file, without reloading it.
   * @return the ConfigFile or null if no such
   */
  public ConfigFile justGet(String url) throws IOException {
    return (ConfigFile)m_configMap.get(url);
  }

  /**
   * Ensure that a file exists in the cache with the most recent
   * available version.
   */
  public void ensureLoaded(String url) throws IOException {
    ConfigFile cf = null;

    if (m_configMap.containsKey(url)) {
      // already exists, just reload
      log.debug2("Reloading " + url);
      ((ConfigFile)m_configMap.get(url)).reload();
    } else {
      // doesn't yet exist in the cache, attempt to add it.
      log.debug2("Adding " + url);
      try {
	if (UrlUtil.isHttpUrl(url)) {
	  cf = new HTTPConfigFile(url);
	} else if (UrlUtil.isJarUrl(url)) {
	  cf = new JarConfigFile(url);
	} else {
	  cf = new FileConfigFile(url);
	} 
	// ensure the file is loaded
	if (!cf.reload()) {
	  log.warning("Configuration file not loaded: " + url);
	}
	m_configMap.put(url, cf);
      } catch (IOException ex) {
	// If we catch any IO exception, remove the offending
	// file from the cache.  The daemon will try to reload it
	// at the next reload interval anyway.
	log.debug2("Error loading, not caching " + url + ": " + ex);
	remove(url);
	throw ex;
      }
    }
  }

  public synchronized void remove(String url) {
    m_configMap.remove(url);
  }
  
  /**
   * Return all config files.
   */
  public List getConfigFiles() {
    return new ArrayList(m_configMap.values());
  }

  public int size() {
    return m_configMap.size();
  }
}
