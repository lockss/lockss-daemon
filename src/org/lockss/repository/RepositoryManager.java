/*
 * $Id: RepositoryManager.java,v 1.9 2007-02-20 01:35:05 tlipkis Exp $
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

import java.net.*;
import java.util.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.config.*;
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

  /** Max percent of time size calculation thread may run. */
  public static final String PARAM_SIZE_CALC_MAX_LOAD =
    PREFIX + "sizeCalcMaxLoad";
  public static final float DEFAULT_SIZE_CALC_MAX_LOAD = 0.5F;

  static final String WDOG_PARAM_SIZE_CALC = "SizeCalc";
  static final long WDOG_DEFAULT_SIZE_CALC = Constants.DAY;

  static final String PRIORITY_PARAM_SIZE_CALC = "SizeCalc";
  static final int PRIORITY_DEFAULT_SIZE_CALC = Thread.NORM_PRIORITY - 1;

  private PlatformUtil platInfo = PlatformUtil.getInstance();
  private List repoList = Collections.EMPTY_LIST;
  int paramNodeCacheSize = DEFAULT_MAX_PER_AU_CACHE_SIZE;
  boolean paramIsGlobalNodeCache = DEFAULT_GLOBAL_CACHE_ENABLED;
  int paramGlobalNodeCacheSize = DEFAULT_MAX_GLOBAL_CACHE_SIZE;
  UniqueRefLruCache globalNodeCache =
      new UniqueRefLruCache(DEFAULT_MAX_GLOBAL_CACHE_SIZE);
  Map localRepos = new HashMap();

  private float sizeCalcMaxLoad = DEFAULT_SIZE_CALC_MAX_LOAD;

  public void startService() {
    super.startService();
    localRepos = new HashMap();
  }

  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {
    //  Build list of repositories from list of disk (fs) paths).  Needs to
    //  be generalized if ever another repository implementation.
    if (changedKeys.contains(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST)) {
      List lst = new ArrayList();
      String dspace =
	config.get(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, "");
      List paths = StringUtil.breakAt(dspace, ';');
      if (paths != null) {
	for (Iterator iter = paths.iterator(); iter.hasNext(); ) {
	  lst.add("local:" + (String)iter.next());
	}
      }
      repoList = lst;
    }
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

  /** Return list of known repository names.  Needs a registration
   * mechanism if ever another repository implementation. */
  public List getRepositoryList() {
    return repoList;
  }

  public PlatformUtil.DF getRepositoryDF(String repoName) {
    String path = LockssRepositoryImpl.getLocalRepositoryPath(repoName);
    log.debug("path: " + path);
    try {
      return platInfo.getDF(path);
    } catch (PlatformUtil.UnsupportedException e) {
      return null;
    }
  }

  public List findExistingRepositoriesFor(String auid) {
    List res = null;
    for (Iterator iter = getRepositoryList().iterator(); iter.hasNext(); ) {
      String repoName = (String)iter.next();
      String path = LockssRepositoryImpl.getLocalRepositoryPath(repoName);
      if (LockssRepositoryImpl.doesAuDirExist(auid, path)) {
	if (res == null) {
	  res = new ArrayList();
	}
	res.add(repoName);
      }
    }
    return res == null ? Collections.EMPTY_LIST : res;
  }

  // hack only local
  public synchronized LockssRepositoryImpl getRepositoryFromPath(String path) {
    LockssRepositoryImpl repo = (LockssRepositoryImpl)localRepos.get(path);
    if (repo == null) {
      repo =  new LockssRepositoryImpl(path);
      repo.initService(getDaemon());
      repo.startService();
      localRepos.put(path, repo);
    }
    return repo;
  }

  /**
   * Return the disk space used by the AU, including all overhead,
   * optionally calculating it if necessary.
   * @param repoAuPath the full path to an AU dir in a LockssRepositoryImpl
   * @param calcIfUnknown if true, size will calculated if unknown (time
   * consumeing)
   * @return the AU's disk usage in bytes, or -1 if unknown
   */
  public long getRepoDiskUsage(String repoAuPath, boolean calcIfUnknown) {
    LockssRepository repo = getRepositoryFromPath(repoAuPath);
    if (repo != null) {
      try {
	RepositoryNode repoNode = repo.getNode(AuCachedUrlSetSpec.URL);
	if (repoNode instanceof AuNodeImpl) {
	  return ((AuNodeImpl)repoNode).getDiskUsage(calcIfUnknown);
	}
      } catch (MalformedURLException ignore) {
      }
    }
    return -1;
  }

  public synchronized void setRepositoryForPath(String path,
						LockssRepositoryImpl repo) {
    localRepos.put(path, repo);
  }

  public boolean isGlobalNodeCache() {
    return paramIsGlobalNodeCache;
  }

  public UniqueRefLruCache getGlobalNodeCache() {
    return globalNodeCache;
  }

  // Background thread to (re)calculate AU size and disk usage.

  private Set sizeCalcQueue = new HashSet();
  private BinarySemaphore sizeCalcSem = new BinarySemaphore();
  private SizeCalcThread sizeCalcThread;

  /** engqueue a size calculation for the AU */
  public void queueSizeCalc(ArchivalUnit au) {
    queueSizeCalc(AuUtil.getAuRepoNode(au));
  }

  /** engqueue a size calculation for the node */
  public void queueSizeCalc(RepositoryNode node) {
    synchronized (sizeCalcQueue) {
      if (sizeCalcQueue.add(node)) {
	log.debug2("Queue size calc: " + node);
	startOrKickThread();
      }
    }
  }

  public int sizeCalcQueueLen() {
    synchronized (sizeCalcQueue) {
      return sizeCalcQueue.size();
    }
  }

  void startOrKickThread() {
    if (sizeCalcThread == null) {
      log.debug2("Starting thread");
      sizeCalcThread = new SizeCalcThread();
      sizeCalcThread.start();
      sizeCalcThread.waitRunning();
    }
    sizeCalcSem.give();
  }

  void stopThread() {
    if (sizeCalcThread != null) {
      log.debug2("Stopping thread");
      sizeCalcThread.stopSizeCalc();
      sizeCalcThread = null;
    }
  }

  void doSizeCalc(RepositoryNode node) {
    node.getTreeContentSize(null, true);
    if (node instanceof AuNodeImpl) {
      ((AuNodeImpl)node).getDiskUsage(true);
    }
  }

  long sleepTimeToAchieveLoad(long runDuration, float maxLoad) {
    return Math.round(((double)runDuration / maxLoad) - runDuration);
  }

  private class SizeCalcThread extends LockssThread {
    private volatile boolean goOn = true;

    private SizeCalcThread() {
      super("SizeCalc");
    }

    public void lockssRun() {
      setPriority(PRIORITY_PARAM_SIZE_CALC, PRIORITY_DEFAULT_SIZE_CALC);
      startWDog(WDOG_PARAM_SIZE_CALC, WDOG_DEFAULT_SIZE_CALC);
      triggerWDogOnExit(true);
      nowRunning();

      while (goOn) {
	try {
	  pokeWDog();
	  if (sizeCalcQueue.isEmpty()) {
	    Deadline timeout = Deadline.in(Constants.HOUR);
	    sizeCalcSem.take(timeout);
	  }
	  RepositoryNode node;
	  synchronized (sizeCalcQueue) {
	    node = (RepositoryNode)CollectionUtil.getAnElement(sizeCalcQueue);
	  }
	  if (node != null) {
	    long start = TimeBase.nowMs();
	    log.debug2("CalcSize start: " + node);
	    long dur = 0;
	    try {
	      doSizeCalc(node);
	      dur = TimeBase.nowMs() - start;
	      log.debug2("CalcSize finish (" +
			 StringUtil.timeIntervalToString(dur) + "): " + node);
	    } catch (RuntimeException e) {
	      log.warning("doSizeCalc: " + node, e);
	    }
	    synchronized (sizeCalcQueue) {
	      sizeCalcQueue.remove(node);
	    }
	    pokeWDog();
	    long sleep = sleepTimeToAchieveLoad(dur, sizeCalcMaxLoad);
	    Deadline.in(sleep).sleep();
	  }
	} catch (InterruptedException e) {
	  // just wakeup and check for exit
	}
      }
      if (!goOn) {
	triggerWDogOnExit(false);
      }
    }

    private void stopSizeCalc() {
      goOn = false;
      interrupt();
    }
  }

}
