/*
 * $Id: ConfigCache.java,v 1.1 2004-06-14 03:08:43 smorabito Exp $
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

package org.lockss.daemon;

import java.util.*;
import java.io.*;
import java.net.*;

import org.lockss.util.*;

/**
 * A memory cache of files used to populate our Configuration.
 * Maintains two lists of files; remote and local.  This allows all
 * the remote files to be parsed before any of the local files.
 */
public class ConfigCache {

  private static HashMap m_remoteFileMap = new HashMap();
  private static HashMap m_localFileMap = new HashMap();
  // When (if) we migrate to a Java >1.4 environment, these will no
  // longer be necessary -- they are only here to impose
  // insertion-order on the HashMaps.  In 1.4 we could use a
  // LinkedHashMap
  private static List m_remoteFileList = new ArrayList();
  private static List m_localFileList = new ArrayList();

  private static Logger log = Logger.getLogger("ConfigCache");

  /**
   * Retrieve a configuration file from the cache.
   */
  public static ConfigFile get(String url) throws IOException {
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
  public static void load(String url) throws IOException {
    try {
      // Store the configuration in the right hashmap
      if (UrlUtil.isHttpUrl(url)) {
	put(m_remoteFileMap, m_remoteFileList, url);
      } else {
	put(m_localFileMap, m_localFileList, url);
      }
    } catch (IOException ex) {
      // If we catch any IO exception, remove the offending
      // file from the cache.  The daemon will try to reload it
      // at the next reload interval anyway.
      remove(url);
      throw ex;
    }
  }

  public static synchronized void remove(String url) {
    try {
      if (UrlUtil.isHttpUrl(url)) {
	m_remoteFileMap.remove(url);
      } else {
	m_localFileMap.remove(url);
      }
    } catch (Exception ex) {
      // Log this, but otherwise do nothing
      log.warning("Failed to remove file from the configuration cache (" +
		  url + "): " + ex);
    }
  }

  /**
   * Utility method to create a ConfigFile and put it into the right
   * cache.  If the ConfigFile is already in the cache, just ask it to
   * reload its content.
   */
  private static synchronized void put(Map map, List list, String url)
      throws IOException {
    if (map.containsKey(url)) {
      log.debug2("put: cache hit, reloading.");
      ((ConfigFile)map.get(url)).reload();
    } else {
      log.debug2("put: cache miss, adding new file.");
      ConfigFile cf = new ConfigFile(url);
      map.put(url, cf);
      list.add(cf);
    }
  }

  /**
   * Return all remotely-loaded config files.
   */
  public static List getRemoteConfigFiles() {
    return m_remoteFileList;
  }

  /**
   * Return all locally-loaded config files.
   */
  public static List getLocalConfigFiles() {
    return m_localFileList;
  }

  /**
   * Return all config files.
   */
  public static List getConfigFiles() {
    List allConfigFiles = m_remoteFileList;
    allConfigFiles.addAll(m_localFileList);
    return allConfigFiles;
  }

  /**
   * Reset the cache - used by unit testing.
   */
  public static synchronized void reset() {
    m_localFileMap.clear();
    m_localFileList.clear();
    m_remoteFileMap.clear();
    m_remoteFileList.clear();
  }

  public static int size() {
    return m_localFileList.size() + m_remoteFileList.size();
  }
}
