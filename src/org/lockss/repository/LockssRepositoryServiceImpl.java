/*
 * $Id: LockssRepositoryServiceImpl.java,v 1.1 2003-03-04 00:16:12 aalto Exp $
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
  private static String cacheLocation = null;

  public LockssRepositoryServiceImpl() { }

  public void initService(LockssDaemon daemon) throws LockssDaemonException {
    if (theManager == null) {
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
    Configuration.registerConfigurationCallback(new Configuration.Callback() {
      public void configurationChanged(Configuration oldConfig,
                                       Configuration newConfig,
                                       Set changedKeys) {
        setConfig(newConfig, oldConfig);
      }
    });
  }

  public void stopService() {
    // checkpoint here
    stopAllRepositories();
    theManager = null;
  }

  private void setConfig(Configuration config, Configuration oldConfig) {
    cacheLocation = config.getParam(PARAM_CACHE_LOCATION);
    if (cacheLocation==null) {
      logger.error("Couldn't get "+PARAM_CACHE_LOCATION+" from Configuration");
      throw new LockssRepository.RepositoryStateException("Couldn't load param.");
    }
    cacheLocation = extendCacheLocation(cacheLocation);
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

  public synchronized LockssRepository addLockssRepository(ArchivalUnit au) {
    LockssRepository lockssRepo = (LockssRepository)auMap.get(au);
    if (lockssRepo==null) {
      if (cacheLocation==null) {
        cacheLocation = Configuration.getParam(PARAM_CACHE_LOCATION);
        if (cacheLocation==null) {
          logger.error("Couldn't get "+PARAM_CACHE_LOCATION+" from Configuration");
          throw new LockssRepository.RepositoryStateException("Couldn't load param.");
        }
        cacheLocation = extendCacheLocation(cacheLocation);
      }
      lockssRepo = new LockssRepositoryImpl(
            RepositoryLocationUtil.mapAuToFileLocation(cacheLocation, au), au);
      auMap.put(au, lockssRepo);
      lockssRepo.initService(theDaemon);
      lockssRepo.startService();
    }
    return lockssRepo;
  }
}
