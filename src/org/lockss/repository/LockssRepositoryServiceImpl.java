/*
 * $Id: LockssRepositoryServiceImpl.java,v 1.12 2003-04-16 02:23:06 aalto Exp $
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

package org.lockss.repository;

import java.util.*;
import org.lockss.app.*;
import org.lockss.daemon.Configuration;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.lockss.util.StringUtil;
import java.io.*;

/**
 * Implementation of the NodeManagerService.
 */
public class LockssRepositoryServiceImpl implements LockssRepositoryService {
  /**
   * Configuration parameter name for Lockss cache location.
   */
  public static final String PARAM_CACHE_LOCATION =
    Configuration.PREFIX + "cache.location";
  /**
   * Name of top directory in which the urls are cached.
   */
  public static final String CACHE_ROOT_NAME = "cache";

  private static LockssDaemon theDaemon;
  private static LockssManager theManager = null;
  private HashMap auMap = new HashMap();
  private static Logger logger = Logger.getLogger("LockssRepositoryService");
  static String cacheLocation = null;

  // used for name mapping
  static HashMap nameMap = null;

  // starts with a '-' so no possibility of clashing with a URL
  static final String AU_ID_FILE = "#au_id_file";
  static final String AU_ID_PROP = "au.id";
  static final String PLUGIN_ID_PROP = "plugin.id";
  static String lastPluginDir = ""+(char)('a'-1);

  public LockssRepositoryServiceImpl() { }

  public void initService(LockssDaemon daemon) throws LockssDaemonException {
    if (theManager == null) {
      // blank the name map
      nameMap = null;

      theDaemon = daemon;
      theManager = this;
      cacheLocation = Configuration.getParam(PARAM_CACHE_LOCATION);
      if (cacheLocation==null) {
	logger.error("Couldn't get "+PARAM_CACHE_LOCATION+" from Configuration");
	throw new LockssRepository.RepositoryStateException("Couldn't load param.");
      }
      cacheLocation = extendCacheLocation(cacheLocation);
    } else {
      throw new LockssDaemonException("Multiple Instantiation.");
    }
  }

  public void startService() {
  }

  public void stopService() {
    // checkpoint here
    stopAllRepositories();
    theManager = null;
  }

  static String extendCacheLocation(String cacheDir) {
    StringBuffer buffer = new StringBuffer(cacheDir);
    if (!cacheDir.endsWith(File.separator)) {
      buffer.append(File.separator);
    }
    buffer.append(CACHE_ROOT_NAME);
    buffer.append(File.separator);
    return buffer.toString();
  }

  private void stopAllRepositories() {
    Iterator entries = auMap.entrySet().iterator();
    while (entries.hasNext()) {
      Map.Entry entry = (Map.Entry)entries.next();
      LockssRepository repo = (LockssRepository)entry.getValue();
      repo.stopService();
    }
  }

  public LockssRepository getLockssRepository(ArchivalUnit au) {
    LockssRepository lockssRepo = (LockssRepository)auMap.get(au);
    if (lockssRepo==null) {
      logger.error("LockssRepository not created for au: "+au);
      throw new IllegalArgumentException("LockssRepository not created for au.");
    }
    return lockssRepo;
  }

  public synchronized void addLockssRepository(ArchivalUnit au) {
    LockssRepository lockssRepo = (LockssRepository)auMap.get(au);
    if (lockssRepo==null) {
      lockssRepo = new LockssRepositoryImpl(
            LockssRepositoryServiceImpl.mapAuToFileLocation(cacheLocation, au),
            au);
      auMap.put(au, lockssRepo);
      logger.debug("Adding LockssRepository for au: "+au);
      lockssRepo.initService(theDaemon);
      lockssRepo.startService();
    }
  }

  /**
   * mapAuToFileLocation() is the method used to resolve {@link ArchivalUnit}s
   * into directory names. This maps a given au to directories, using the
   * cache root as the base.  Given an au with PluginId of 'plugin' and AuId
   * of 'au', it would return the string '<rootLocation>/plugin/au/'.
   * @param rootLocation the root for all ArchivalUnits
   * @param au the ArchivalUnit to resolve
   * @return the directory location
   */
  public static String mapAuToFileLocation(String rootLocation, ArchivalUnit au) {
    StringBuffer buffer = new StringBuffer(rootLocation);
    if (!rootLocation.endsWith(File.separator)) {
      buffer.append(File.separator);
    }
    getAuDir(au, buffer);
    buffer.append(File.separator);
    return buffer.toString();
  }

  /**
   * mapUrlToFileLocation() is the method used to resolve urls into file names.
   * This maps a given url to a file location, using the au top directory as
   * the base.  It creates directories which mirror the html string, so
   * 'http://www.journal.org/issue1/index.html' would be cached in the file:
   * <rootLocation>/www.journal.org/http/issue1/index.html
   * @param rootLocation the top directory for ArchivalUnit this URL is in
   * @param urlStr the url to translate
   * @return the url file location
   * @throws java.net.MalformedURLException
   */
  public static String mapUrlToFileLocation(String rootLocation, String urlStr) throws
      MalformedURLException {
    int totalLength = rootLocation.length() + urlStr.length();
    URL url = new URL(urlStr);
    StringBuffer buffer = new StringBuffer(totalLength);
    buffer.append(rootLocation);
    if (!rootLocation.endsWith(File.separator)) {
      buffer.append(File.separator);
    }
    buffer.append(url.getHost().toLowerCase());
    buffer.append(File.separator);
    buffer.append(url.getProtocol());
    buffer.append(StringUtil.replaceString(url.getPath(), "/", File.separator));
    return buffer.toString();
  }

  static void getAuDir(ArchivalUnit au, StringBuffer buffer) {
    if (nameMap == null) {
      loadNameMap(buffer.toString());
    }
    String auKey = getAuKey(au);
    String auDir = (String)nameMap.get(auKey);
    if (auDir == null) {
      logger.debug3("Creating new au directory for '" + auKey + "'.");
      while (true) {
        auDir = getNewPluginDir();
        File testDir = new File(buffer.toString() + auDir);
        if (!testDir.exists()) {
          break;
        } else {
          logger.debug3("Existing directory found at '"+auDir+
                        "'.  Creating another...");
        }
      }
      logger.debug3("New au directory: "+auDir);
      nameMap.put(auKey, auDir);
      String auLocation = buffer.toString() + auDir;
      Properties idProps = new Properties();
      idProps.setProperty(PLUGIN_ID_PROP, au.getPluginId());
      idProps.setProperty(AU_ID_PROP, au.getAUId());
      saveAuIdProperties(auLocation, idProps);
    }
    buffer.append(auDir);
  }

  static void loadNameMap(String rootLocation) {
    logger.debug3("Loading name map for '" + rootLocation + "'.");
    nameMap = new HashMap();
    File rootFile = new File(rootLocation);
    if (!rootFile.exists()) {
      rootFile.mkdirs();
      logger.debug3("Creating root directory at '" + rootLocation + "'.");
      return;
    }
    File[] pluginAus = rootFile.listFiles();
    for (int ii = 0; ii < pluginAus.length; ii++) {
      String dirName = pluginAus[ii].getName();
      if (dirName.compareTo(lastPluginDir) == 1) {
        lastPluginDir = dirName;
      }

      Properties idProps = getAuIdProperties(pluginAus[ii].getAbsolutePath());
      if (idProps==null) {
        // if no properties were found, just continue
        continue;
      }
      nameMap.put(makeAuKey(idProps.getProperty(PLUGIN_ID_PROP),
                            idProps.getProperty(AU_ID_PROP)), dirName);
    }
  }

  static String getNewPluginDir() {
    String newPluginDir = "";
    boolean charChanged = false;
    // go through and increment the first non-'z' char
    for (int ii=lastPluginDir.length()-1; ii>=0; ii--) {
      char curChar = lastPluginDir.charAt(ii);
      if (!charChanged) {
        if (curChar < 'z') {
          curChar++;
          charChanged = true;
          newPluginDir = curChar + newPluginDir;
        } else {
          newPluginDir += 'a';
        }
      } else {
        newPluginDir = curChar + newPluginDir;
      }
    }
    if (!charChanged) {
      newPluginDir += 'a';
    }
    lastPluginDir = newPluginDir;
    return newPluginDir;
  }

  static Properties getAuIdProperties(String location) {
    File propFile = new File(location + File.separator + AU_ID_FILE);
    try {
      InputStream is = new BufferedInputStream(new FileInputStream(propFile));
      Properties idProps = new Properties();
      idProps.load(is);
      is.close();
      return idProps;
    } catch (Exception e) {
      logger.warning("Error loading au id from " + propFile.getPath() + ".");
      return null;
    }
  }

  static void saveAuIdProperties(String location, Properties props) {
    File propDir = new File(location);
    if (!propDir.exists()) {
      logger.debug("Creating directory '"+propDir.getAbsolutePath()+"'");
      propDir.mkdirs();
    }
    File propFile = new File(propDir, AU_ID_FILE);
    try {
      logger.debug3("Saving au id properties at '" + location + "'.");
      OutputStream os = new BufferedOutputStream(new FileOutputStream(propFile));
      props.store(os, "ArchivalUnit id info");
      os.close();
      propFile.setReadOnly();
    } catch (IOException ioe) {
      logger.error("Couldn't write properties for " + propFile.getPath() + ".",
                   ioe);
      throw new LockssRepository.RepositoryStateException(
          "Couldn't write au id properties file.");
    }
  }

  static String getAuKey(ArchivalUnit au) {
    return makeAuKey(au.getPluginId(), au.getAUId());
  }

  static String makeAuKey(String pluginId, String auId) {
    return pluginId + ":" + auId;
  }

}
