/*
 * $Id: ConfigCache.java,v 1.2 2004-10-01 17:50:09 smorabito Exp $
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

import org.apache.commons.collections.*;
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

  private SequencedHashMap m_remoteFileMap = new SequencedHashMap();
  private SequencedHashMap m_localFileMap = new SequencedHashMap();


  /**
   * Retrieve a configuration file from the cache.
   */
  public ConfigFile get(String url) {
    ConfigFile confFile = null;

    if (UrlUtil.isHttpUrl(url)) {
      confFile = (ConfigFile)m_remoteFileMap.get(url);
    } else {
      confFile = (ConfigFile)m_localFileMap.get(url);
    }

    return confFile;
  }


  /**
   * Insert a configuration file into the cache.  If the file
   * is already in the cache, it will be reloaded if it has changed.
   */
  public void load(String url) throws IOException {
    try {
      // Store the configuration in the right hashmap
      if (UrlUtil.isHttpUrl(url)) {
	put(m_remoteFileMap, url);
      } else {
	put(m_localFileMap, url);
      }
    } catch (IOException ex) {
      // If we catch any IO exception, remove the offending
      // file from the cache.  The daemon will try to reload it
      // at the next reload interval anyway.
      log.debug2("Unable to load file, not caching.");
      remove(url);
      throw ex;
    }
  }

  public synchronized void remove(String url) {
    if (UrlUtil.isHttpUrl(url)) {
      m_remoteFileMap.remove(url);
    } else {
      m_localFileMap.remove(url);
    }
  }

  /**
   * Utility method to create a ConfigFile and put it into the right
   * cache.  If the ConfigFile is already in the cache, just ask it to
   * reload its content.
   */
  private synchronized void put(Map map, String url)
      throws IOException {
    if (map.containsKey(url)) {
      log.debug2("put: cache hit, reloading.");
      ((ConfigFile)map.get(url)).reload();
    } else {
      log.debug2("put: cache miss, adding new file.");
      ConfigFile cf = new ConfigFile(url);
      map.put(url, cf);
    }
  }

  /**
   * Return all remotely-loaded config files.
   */
  public List getRemoteConfigFiles() {
    return new ArrayList(m_remoteFileMap.values());
  }

  /**
   * Return all locally-loaded config files.
   */
  public List getLocalConfigFiles() {
    return new ArrayList(m_localFileMap.values());
  }

  /**
   * Return all config files.
   */
  public List getConfigFiles() {
    List allConfigFiles = new ArrayList(m_localFileMap.values());
    allConfigFiles.addAll(m_remoteFileMap.values());
    return allConfigFiles;
  }

  public int size() {
    return m_localFileMap.size() + m_remoteFileMap.size();
  }
}
