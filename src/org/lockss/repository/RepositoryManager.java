/*
 * $Id: RepositoryManager.java,v 1.3 2004-10-18 03:40:32 tlipkis Exp $
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

package org.lockss.repository;

import java.util.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;

/**
 * RepositoryManager is the center of the per AU repositories.  It manages
 * the repository config parameters.
 */
public class RepositoryManager
  extends BaseLockssDaemonManager implements ConfigurableManager {

  private static Logger log = Logger.getLogger("RepositoryManager");

  public static final String PREFIX = Configuration.PREFIX + "repository.";

  /** Maximum size of per-AU repository node cache */
  public static final String PARAM_MAX_PER_AU_CACHE_SIZE =
    PREFIX + "nodeCache.size";
  public static final int DEFAULT_MAX_PER_AU_CACHE_SIZE = 10;

  static final String GLOBAL_CACHE_PREFIX = PREFIX + "globalNodeCache.";
  public static final String PARAM_MAX_GLOBAL_CACHE_SIZE =
    GLOBAL_CACHE_PREFIX + "size";
  public static final int DEFAULT_MAX_GLOBAL_CACHE_SIZE = 500;

  public static final String PARAM_GLOBAL_CACHE_ENABLED =
    GLOBAL_CACHE_PREFIX + "enabled";
  public static final boolean DEFAULT_GLOBAL_CACHE_ENABLED = false;

  int paramNodeCacheSize = DEFAULT_MAX_PER_AU_CACHE_SIZE;
  boolean paramIsGlobalNodeCache = DEFAULT_GLOBAL_CACHE_ENABLED;
  int paramGlobalNodeCacheSize = DEFAULT_MAX_GLOBAL_CACHE_SIZE;
  UniqueRefLruCache globalNodeCache =
      new UniqueRefLruCache(DEFAULT_MAX_GLOBAL_CACHE_SIZE);

  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {
    if (changedKeys.contains(PARAM_MAX_PER_AU_CACHE_SIZE)) {
      paramNodeCacheSize = config.getInt(PARAM_MAX_PER_AU_CACHE_SIZE,
					 DEFAULT_MAX_PER_AU_CACHE_SIZE);
      for (Iterator iter = getDaemon().getAllLockssRepositories().iterator();
	   iter.hasNext(); ) {
	LockssRepository repo = (LockssRepository)iter.next();
	if (repo instanceof LockssRepositoryImpl) {
	  LockssRepositoryImpl repoImpl = (LockssRepositoryImpl)repo;
	  repoImpl.setNodeCacheSize(paramNodeCacheSize);
	}
      }
    }
    if (changedKeys.contains(GLOBAL_CACHE_PREFIX)) {
      paramIsGlobalNodeCache = config.getBoolean(PARAM_GLOBAL_CACHE_ENABLED,
						 DEFAULT_GLOBAL_CACHE_ENABLED);
      if (paramIsGlobalNodeCache) {
	paramGlobalNodeCacheSize = config.getInt(PARAM_MAX_GLOBAL_CACHE_SIZE,
						 DEFAULT_MAX_GLOBAL_CACHE_SIZE);
	log.debug("global node cache size: " + paramGlobalNodeCacheSize);
	globalNodeCache.setMaxSize(paramGlobalNodeCacheSize);
      }
    }
  }

  public boolean isGlobalNodeCache() {
    return paramIsGlobalNodeCache;
  }

  public UniqueRefLruCache getGlobalNodeCache() {
    return globalNodeCache;
  }
}
