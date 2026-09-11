/*

Copyright (c) 2000-2024 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.nio.file.*;
import java.net.*;
import java.util.*;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.io.FileUtils;

import org.lockss.app.*;
import org.lockss.laaws.MigrationManager;
import org.lockss.servlet.MigrateContent;
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

  /*
   * This needs to be a small multiple of the number of simultaneous
   * polls (poller and voter), as there is a cache entry per active AU.
   * Each poll will have one active AU at a time.
   */
  public static final String PARAM_MAX_SUSPECT_VERSIONS_CACHE_SIZE =
    PREFIX + "suspectVersionsCache.size";
  public static final int DEFAULT_MAX_SUSPECT_VERSIONS_CACHE_SIZE = 10;

  static final String GLOBAL_CACHE_PREFIX = PREFIX + "globalNodeCache.";
  public static final String PARAM_MAX_GLOBAL_CACHE_SIZE =
    GLOBAL_CACHE_PREFIX + "size";
  public static final int DEFAULT_MAX_GLOBAL_CACHE_SIZE = 500;

  public static final String PARAM_GLOBAL_CACHE_ENABLED =
    GLOBAL_CACHE_PREFIX + "enabled";
  public static final boolean DEFAULT_GLOBAL_CACHE_ENABLED = false;

  /** Max times to loop looking for unused AU directory. */
  public static final String PARAM_MAX_UNUSED_DIR_SEARCH =
    PREFIX + "maxUnusedDirSearch";
  public static final int DEFAULT_MAX_UNUSED_DIR_SEARCH = 30000;

  /** If true, LocalRepository keeps track of next subdir name to probe. */
  public static final String PARAM_IS_STATEFUL_UNUSED_DIR_SEARCH =
    PREFIX + "enableStatefulUnusedDirSearch";
  public static final boolean DEFAULT_IS_STATEFUL_UNUSED_DIR_SEARCH = true;

  /** Max percent of time size calculation thread may run. */
  public static final String PARAM_SIZE_CALC_MAX_LOAD =
    PREFIX + "sizeCalcMaxLoad";
  public static final float DEFAULT_SIZE_CALC_MAX_LOAD = 0.5F;

  /** If true, path components longer than the maximum filesystem path
   * component are encodes are multiple levels of directories */
  public static final String PARAM_ENABLE_LONG_COMPONENTS =
    PREFIX + "enableLongComponents";
  public static final boolean DEFAULT_ENABLE_LONG_COMPONENTS = true;

  /** Prior to 1.61.6, when long component support is enabled, backslahes
   * were normalized to %5c instead of %5C.  This is harmless if
   * checkUnnormalized is set to Fix, as they'll be normalized.  Otherwise,
   * this can be set true for compatibility with old repositories.  Setting
   * checkUnnormalized to Fix is preferred. */
  public static final String PARAM_ENABLE_LONG_COMPONENTS_COMPATIBILITY =
    PREFIX + "enableLongComponentsCompatibility";
  public static final boolean DEFAULT_ENABLE_LONG_COMPONENTS_COMPATIBILITY =
    false;

  /** Maximum length of a filesystem path component. */
  public static final String PARAM_MAX_COMPONENT_LENGTH =
    PREFIX + "maxComponentLength";
  public static final int DEFAULT_MAX_COMPONENT_LENGTH = 255;


  /** @see #PARAM_CHECK_UNNORMALIZED */
  public enum CheckUnnormalizedMode {No, Log, Fix};

  /** Check for existing nodes with unnormalized names (created by very old
   * daemon that didn't normalize): None, Log, Fix */
  public static final String PARAM_CHECK_UNNORMALIZED =
    PREFIX + "checkUnnormalized";
  public static final CheckUnnormalizedMode DEFAULT_CHECK_UNNORMALIZED =
    CheckUnnormalizedMode.Log;

  /** If set, the contents of deleted AUs will be moved to a directory
   * below this.  Must be a path relative to the parent of .../cache. */
  public static final String PARAM_MOVE_DELETED_AUS_TO =
    PREFIX + "moveDeletedAusTo";
  public static final String DEFAULT_MOVE_DELETED_AUS_TO = null;
  /** How often the delete threads rescan the "deleted AUs" dirs when
   * they have nothing to do.  This is only a backstop: an idle thread
   * is woken immediately whenever an AU dir is moved into a "deleted
   * AUs" dir.  It's also how long a dir that failed to delete is
   * skipped before being retried. */
  public static final String PARAM_DELETEAUS_INTERVAL =
      PREFIX + "deleteAusInterval";
  public static final long DEFAULT_DELETEAUS_INTERVAL = Constants.HOUR;

  /** Number of threads deleting AU dirs in parallel.  Each runs an
   * 'rm -rf'; a handful is needed to keep up with the migrator. */
  public static final String PARAM_DELETEAUS_THREADS =
      PREFIX + "deleteAusThreads";
  public static final int DEFAULT_DELETEAUS_THREADS = 6;
  static final String PRIORITY_PARAM_DELETEAUS_THREAD = "DeleteAusThread";
  static final int PRIORITY_DEFAULT_DELETEAUS_THREAD = Thread.NORM_PRIORITY + 2;

  static final String WDOG_PARAM_SIZE_CALC = "SizeCalc";
  static final long WDOG_DEFAULT_SIZE_CALC = Constants.DAY;

  static final String PRIORITY_PARAM_SIZE_CALC = "SizeCalc";
  static final int PRIORITY_DEFAULT_SIZE_CALC = Thread.NORM_PRIORITY - 1;

  static final String DISK_PREFIX = PREFIX + "diskSpace.";
  

  static final String PARAM_DISK_WARN_FRRE_MB = DISK_PREFIX + "warn.freeMB";
  static final int DEFAULT_DISK_WARN_FRRE_MB = 5000;
  static final String PARAM_DISK_FULL_FRRE_MB = DISK_PREFIX + "full.freeMB";
  static final int DEFAULT_DISK_FULL_FRRE_MB = 100;
  static final String PARAM_DISK_WARN_FRRE_PERCENT =
    DISK_PREFIX + "warn.freePercent";
  static final double DEFAULT_DISK_WARN_FRRE_PERCENT = .02;
  static final String PARAM_DISK_FULL_FRRE_PERCENT =
    DISK_PREFIX + "full.freePercent";
  static final double DEFAULT_DISK_FULL_FRRE_PERCENT = .01;

  private PluginManager pluginMgr;
  private PlatformUtil platInfo = PlatformUtil.getInstance();
  private List repoList = Collections.EMPTY_LIST;
  int paramNodeCacheSize = DEFAULT_MAX_PER_AU_CACHE_SIZE;
  boolean paramIsGlobalNodeCache = DEFAULT_GLOBAL_CACHE_ENABLED;
  int paramGlobalNodeCacheSize = DEFAULT_MAX_GLOBAL_CACHE_SIZE;
  int paramSuspectVersionsCacheSize = DEFAULT_MAX_SUSPECT_VERSIONS_CACHE_SIZE;
  UniqueRefLruCache globalNodeCache =
      new UniqueRefLruCache(DEFAULT_MAX_GLOBAL_CACHE_SIZE);
  UniqueRefLruCache suspectVersionsCache =
    new UniqueRefLruCache(DEFAULT_MAX_SUSPECT_VERSIONS_CACHE_SIZE);
  Map localRepos = new HashMap();
  private static int maxUnusedDirSearch = DEFAULT_MAX_UNUSED_DIR_SEARCH;
  private static boolean isStatefulUnusedDirSearch =
    DEFAULT_IS_STATEFUL_UNUSED_DIR_SEARCH;
  private static boolean enableLongComponents = DEFAULT_ENABLE_LONG_COMPONENTS;
  private static boolean enableLongComponentsCompatibility =
    DEFAULT_ENABLE_LONG_COMPONENTS_COMPATIBILITY;
  private static int maxComponentLength = DEFAULT_MAX_COMPONENT_LENGTH;
  private static CheckUnnormalizedMode checkUnnormalized =
    DEFAULT_CHECK_UNNORMALIZED;
  private String paramMoveDeletedAusTo = DEFAULT_MOVE_DELETED_AUS_TO;
  private long paramDeleteThreadInterval = DEFAULT_DELETEAUS_INTERVAL;
  private int paramDeleteAusThreads = DEFAULT_DELETEAUS_THREADS;
  private AuEventHandler auEventHandler;


  PlatformUtil.DF paramDFWarn =
    PlatformUtil.DF.makeThreshold(DEFAULT_DISK_WARN_FRRE_MB,
				  DEFAULT_DISK_WARN_FRRE_PERCENT);
  PlatformUtil.DF paramDFFull =
    PlatformUtil.DF.makeThreshold(DEFAULT_DISK_FULL_FRRE_MB,
				  DEFAULT_DISK_FULL_FRRE_PERCENT);

  private float sizeCalcMaxLoad = DEFAULT_SIZE_CALC_MAX_LOAD;

  public void startService() {
    super.startService();
    pluginMgr = getDaemon().getPluginManager();
    localRepos = new HashMap();

    // register our AU event handler
    auEventHandler = new AuEventHandler.Base() {
	@Override public void auDeleted(AuEvent event, ArchivalUnit au) {
	  moveAuDir(event, au);
	}};
    pluginMgr.registerAuEventHandler(auEventHandler);

    resetConfig();        // ensure migration-related params processed
  }

  public void stopService() {
    stopDeleteAusThreads();
    stopSizeCalcThread();
    if (auEventHandler != null && pluginMgr != null) {
      pluginMgr.unregisterAuEventHandler(auEventHandler);
      auEventHandler = null;
    }
    super.stopService();
  }

  /** Optionally rename the repository dir(s) belonging to a deleted AU to
   * a "deleted" directory, and remove the auid -> dir mapping(s) */

  // Testcase for this is TestPluginManager.testRenameDeletedAuDir().
  // This is not bulletproof, but making it so would be more work than is
  // warranted for a repository implementation that's on its way out.  The
  // intended use is for autest, which normally won't delete AUs with
  // active polls or crawls.
  private void moveAuDir(AuEvent event, ArchivalUnit au) {
    switch (event.getType()) {
      // Do this only for Delete, not Deactivate or RestartDelete
    case Delete:
      moveDeletedAu(au);
      break;
    }
  }

  //////////////////////////////////////////////////////////////////////
  // Deletion of AU dirs that have been renamed into the "deleted AUs"
  // dir (see PARAM_MOVE_DELETED_AUS_TO).  This is normally driven by
  // migration: as each AU finishes migrating to V2 its dir is renamed
  // into that dir, and must then be removed to reclaim space. This is
  // also used in Content Testing, in conjunction with AUTest.
  //
  // A small pool of DeleteAusThreads each loop: claim the next AU dir
  // to delete - the one in the "deleted AUs" dir on the disk with the
  // least free space - and remove it by running 'rm -rf'.  Each thread
  // chooses its dir at the moment it becomes free, so the choice always
  // reflects current disk usage.  A handful of concurrent 'rm -rf'
  // processes was empirically found to keep up with the migrator;
  // deleting one dir at a time did not.
  //////////////////////////////////////////////////////////////////////

  /** A "deleted AUs" dir, paired with the space info of the disk it
   * lives on. */
  static class FileDF {
    final File file;
    final PlatformUtil.DF df;

    FileDF(File file, PlatformUtil.DF df) {
      this.file = file;
      this.df = df;
    }

    File getFile() {
      return file;
    }

    PlatformUtil.DF getDF() {
      return df;
    }

    public String toString() {
      return "[FileDF: " + file + ", avail=" +
        (df == null ? "unknown" : Long.toString(df.getAvail())) + "]";
    }
  }

  /** Comparator that sorts by increasing free space.  Dirs whose free
   * space couldn't be determined sort last. */
  static final Comparator<FileDF> ascendingSpaceComparator =
    new Comparator<FileDF>()  {
      public int compare(FileDF f1, FileDF f2) {
        return Long.compare(avail(f1), avail(f2));
      }
      long avail(FileDF fdf) {
        return fdf.df == null ? Long.MAX_VALUE : fdf.df.getAvail();
      }
    };

  /** Dirs claimed by a delete thread, so shouldn't be claimed again. */
  Set<File> activeDeletes = Collections.synchronizedSet(new HashSet<File>());

  /** Dirs a delete thread failed to delete (unwritable dir, etc.).
   * Skipped until the next periodic rescan, else the threads would
   * claim the same undeletable dir over and over. */
  Set<File> failedDeletes = Collections.synchronizedSet(new HashSet<File>());

  /** Monitor on which idle delete threads park. */
  private final Object deleteLock = new Object();

  /** Bumped whenever there might be new work.  Delete threads compare
   * the value they saw before looking for work with the current value,
   * so a dir that arrives while a thread is between looking and waiting
   * can't be missed.  Guarded by deleteLock. */
  private long deleteGeneration = 0;

  /** When failedDeletes was last cleared.  Guarded by this. */
  private long lastFailedRetry = 0;

  /** Return space info for the disk holding the dir, or null if it
   * can't be determined.  Overridable for testing. */
  PlatformUtil.DF getDF(File dir) {
    try {
      return platInfo.getDF(dir.toString());
    } catch (RuntimeException e) {
      log.warning("Couldn't determine free space on " + dir, e);
      return null;
    }
  }

  /** Return a FileDF for each existing "deleted AUs" dir in the
   * repository.
   * @throws IllegalStateException if {@value #PARAM_MOVE_DELETED_AUS_TO}
   * isn't set.
   */
  List<FileDF> getDiskSpaceList() {
    String delRoot = paramMoveDeletedAusTo;
    if (StringUtil.isNullString(delRoot)) {
      throw new IllegalStateException(PARAM_MOVE_DELETED_AUS_TO +" is not set.");
    }
    List<FileDF> fdfs = new ArrayList<FileDF>();
    for (String repoName : getRepositoryList()) {
      String repoRoot = LockssRepositoryImpl.getLocalRepositoryPath(repoName);
      if (repoRoot == null) {
        continue;
      }
      File deletedAusDir = new File(repoRoot, delRoot);
      if (deletedAusDir.isDirectory()) {
        fdfs.add(new FileDF(deletedAusDir, getDF(deletedAusDir)));
      }
    }
    return fdfs;
  }

  /** Return the next dir to delete: an entry in the "deleted AUs" dir on
   * the disk with the least free space, which no thread has already
   * claimed, or null if there's nothing to delete. */
  File findDirOnMostFullDisk() {
    return findDirOnMostFullDisk(getDiskSpaceList());
  }

  /** Return the next dir to delete: an entry in the "deleted AUs" dir on
   * the disk with the least free space, which no thread has already
   * claimed, or null if there's nothing to delete.
   * @param fdfs DF info for each disk
   */
  File findDirOnMostFullDisk(List<FileDF> fdfs) {
    Collections.sort(fdfs, ascendingSpaceComparator);
    for (FileDF fdf : fdfs) {
      File res = findUnclaimedDir(fdf.file);
      if (res != null) {
        return res;
      }
    }
    return null;
  }

  /** Return the first entry in dir that isn't already claimed, else
   * null.  Uses a DirectoryStream so that a dir holding many thousands
   * of AUs needn't be read in its entirety. */
  private File findUnclaimedDir(File dir) {
    try (DirectoryStream<Path> stream =
         Files.newDirectoryStream(dir.toPath())) {
      for (Path path : stream) {
        File file = path.toFile();
        if (!activeDeletes.contains(file) && !failedDeletes.contains(file)) {
          return file;
        }
      }
    } catch (IOException | RuntimeException e) {
      log.warning("Couldn't list deleted AUs dir: " + dir, e);
    }
    return null;
  }

  /** Choose the next dir to delete and claim it.  Synchronized so that
   * the choice and the claim are atomic: two threads becoming free at
   * the same moment can't pick the same dir.
   * @return the claimed dir, or null if there's nothing to delete
   */
  synchronized File claimNextDir() {
    // Periodically give previously failed dirs another chance
    if (TimeBase.msSince(lastFailedRetry) >= paramDeleteThreadInterval) {
      if (!failedDeletes.isEmpty()) {
        log.debug2("Rescan: retrying " + failedDeletes.size() +
                   " previously failed dir(s)");
        failedDeletes.clear();
      }
      lastFailedRetry = TimeBase.nowMs();
    }
    List<FileDF> fdfs = getDiskSpaceList();
    File next = findDirOnMostFullDisk(fdfs);
    if (next == null) {
      return null;
    }
    if (!isDeletableDir(next, fdfs)) {
      // Shouldn't happen; cheap insurance in front of 'rm -rf'
      log.error("Not an entry in a deleted AUs dir, refusing to delete: " + next);
      failedDeletes.add(next);
      return null;
    }
    activeDeletes.add(next);
    return next;
  }

  /** Return true iff dir is an immediate child of one of the "deleted
   * AUs" dirs. */
  private boolean isDeletableDir(File dir, List<FileDF> fdfs) {
    File parent = dir.getParentFile();
    if (parent == null) {
      return false;
    }
    for (FileDF fdf : fdfs) {
      if (parent.equals(fdf.file)) {
        return true;
      }
    }
    return false;
  }

  /** Delete a dir tree claimed by {@link #claimNextDir()}, and release
   * the claim.
   * @return true iff the dir was deleted
   */
  boolean deleteClaimedDir(File dir) {
    long start = TimeBase.nowMs();
    boolean ok = false;
    try {
      log.debug2("Deleting dir tree: " + dir);
      ok = FileUtil.fastDelTree(dir);
      if (!ok) {
        log.warning("Failed to delete dir tree: " + dir);
      }
    } catch (IOException e) {
      log.warning("Error deleting dir tree, falling back to Java delete: " +
                  dir, e);
      try {
        // delTree() requires a directory; a "deleted AUs" dir could in
        // principle hold a stray plain file, which 'rm -rf' would have
        // removed
        ok = dir.isDirectory() ? FileUtil.delTree(dir)
          : FileUtil.safeDeleteFile(dir);
      } catch (RuntimeException re) {
        log.error("Fallback delete failed: " + dir, re);
      }
      if (!ok) {
        log.error("Failed to delete dir tree: " + dir);
      }
    } catch (RuntimeException e) {
      log.error("Error deleting dir tree: " + dir, e);
    } finally {
      if (!ok) {
        // Don't retry until the next rescan
        failedDeletes.add(dir);
      }
      activeDeletes.remove(dir);
      if (log.isDebug2()) {
        log.debug2("Delete " + (ok ? "finished" : "failed") + " (" +
                   StringUtil.timeIntervalToString(TimeBase.nowMs() - start) +
                   "): " + dir);
      }
      try {
        deleteFinished(dir, ok);
      } catch (RuntimeException e) {
        log.error("deleteFinished() threw", e);
      }
    }
    return ok;
  }

  /** Called when a delete thread has finished with a dir, successfully
   * or not.  Overridable for testing. */
  protected void deleteFinished(File dir, boolean success) {
  }

  /** Tell idle delete threads that there may be work to do. */
  void pokeDeleteAusThreads() {
    synchronized (deleteLock) {
      deleteGeneration++;
      deleteLock.notifyAll();
    }
  }

  /** Return the current work generation. */
  private long getDeleteGeneration() {
    synchronized (deleteLock) {
      return deleteGeneration;
    }
  }

  /** Wait until poked or the rescan interval elapses.  Returns
   * immediately if a poke has happened since the caller read gen, so
   * that a dir arriving between looking for work and waiting for it
   * can't be missed.
   * @param gen generation read before the caller looked for work
   */
  private void awaitDeleteWork(long gen) throws InterruptedException {
    synchronized (deleteLock) {
      if (deleteGeneration == gen) {
        deleteLock.wait(paramDeleteThreadInterval);
      }
    }
  }

  /** Repeatedly claims the next AU dir to delete and deletes it. */
  private class DeleteAusThread extends LockssThread {
    private volatile boolean keepGoing = true;

    DeleteAusThread(int ix) {
      super("DeleteAus-" + ix);
    }

    /** Ask the thread to exit.  A thread in the middle of a delete
     * finishes it first: interrupting wouldn't stop the 'rm' process it
     * is waiting for, it would just orphan it. */
    void stopDeleteThread() {
      keepGoing = false;
    }

    @Override
    protected void lockssRun() {
      setPriority(PRIORITY_PARAM_DELETEAUS_THREAD,
                  PRIORITY_DEFAULT_DELETEAUS_THREAD);
      nowRunning();
      while (keepGoing) {
        try {
          // Read the generation before looking, so that work arriving
          // while we look isn't missed when we wait below
          long gen = getDeleteGeneration();
          File dir = claimNextDir();
          if (dir != null) {
            deleteClaimedDir(dir);
          } else {
            awaitDeleteWork(gen);
          }
        } catch (InterruptedException e) {
          // just wakeup and check for exit
        } catch (RuntimeException e) {
          // E.g., PARAM_MOVE_DELETED_AUS_TO unset out from under us.
          // Don't let the thread die; wait and retry.
          log.error("Delete AUs thread error, continuing", e);
          try {
            awaitDeleteWork(getDeleteGeneration());
          } catch (InterruptedException ie) {
            // just wakeup and check for exit
          }
        }
      }
      log.debug2(getName() + " exiting");
    }
  }

  /**
   * Moves a deleted AU to the deleted AUs directory for actual removal by
   * an external process.
   *
   * @return A {@link File} that the AU was moved to, or {@code null} if
   */
  void moveDeletedAu(ArchivalUnit au) {
    String auid = au.getAuId();

    if (!StringUtil.isNullString(paramMoveDeletedAusTo)) {
      // Normally will be only one dir for AU, no harm in checking all
      // the repos
      for (String repoName : getRepositoryList()) {
        String repoRoot =
            LockssRepositoryImpl.getLocalRepositoryPath(repoName);
        // Ensure deleted dir exists
        File deletedDir = new File(repoRoot, paramMoveDeletedAusTo);
        if (!deletedDir.exists()) {
          log.debug("Creating deleted AU dir: " + deletedDir);
          if (!deletedDir.mkdirs()) {
            log.error("Couldn't create deleted AU dir: " + deletedDir);
            continue;
          }
        }
        LockssRepositoryImpl.LocalRepository localRepo =
            LockssRepositoryImpl.getLocalRepository(repoRoot);
        synchronized (localRepo) {
          String auDirPath =
              LockssRepositoryImpl.getAuDir(auid, repoRoot, false);
          if (auDirPath != null) {
            log.debug2("del repo for: " + au.getName() + ": " + auDirPath);
            File auDir = new File(auDirPath);
            String auDirName = auDir.getName();

            // Uniqueify the dirname under the "deleted" dir
            File moveToDir = new File(deletedDir, auDirName);
            int suffix = 1;
            while (moveToDir.exists()) {
              moveToDir = new File(deletedDir, auDirName + "." + suffix++);
            }
            try {
              log.debug2("move(" + auDir.toPath() + ", "
                  + moveToDir.toPath() + ")");

              if (pluginMgr.getAuFromId(auid) != null) {
                log.debug("AU recreated, not deleting repo dir: " +
                    au.getName());
                continue;
              }
              Files.move(auDir.toPath(), moveToDir.toPath(),
                  StandardCopyOption.ATOMIC_MOVE);
              // Remove entry from map
              LockssRepositoryImpl.removeAuDirEntry(pluginMgr, auid, repoRoot);

              // Update timestamp so an external janitor script knows when
              // to delete the dir.  Best effort only: a Deleter may
              // already have removed the dir.
              try {
                FileUtils.touch(moveToDir);
              } catch (IOException e) {
                log.debug2("Couldn't touch moved AU dir: " + moveToDir, e);
              }
              // Tell the delete threads there's something new to do
              pokeDeleteAusThreads();
            } catch (IOException e) {
              log.error("failed to move deleted AU dir: " + auDir +
                  " to deleted dir: " + moveToDir, e);
            }
          }
        }
      }
    }
  }

  private String localRepoSpec(String path) {
    return StringPool.REPO_SPEC.intern("local:" + path);
  }

  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {
    //  Build list of repositories from list of disk (fs) paths).  Needs to
    //  be generalized if ever another repository implementation.
    if (changedKeys.contains(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST)) {
      List lst = new ArrayList();
      String dspace =
	config.get(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, "");
      List<String> paths = StringUtil.breakAt(dspace, ';');
      if (paths != null) {
	for (String path : paths) {
	  lst.add(localRepoSpec(path));
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
    if (changedKeys.contains(PARAM_MAX_SUSPECT_VERSIONS_CACHE_SIZE)) {
      paramSuspectVersionsCacheSize =
	config.getInt(PARAM_MAX_SUSPECT_VERSIONS_CACHE_SIZE,
		      DEFAULT_MAX_SUSPECT_VERSIONS_CACHE_SIZE);
      suspectVersionsCache.setMaxSize(paramSuspectVersionsCacheSize);
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
    if (changedKeys.contains(DISK_PREFIX)) {
      int minMB = config.getInt(PARAM_DISK_WARN_FRRE_MB,
				DEFAULT_DISK_WARN_FRRE_MB);
      double minPer = config.getPercentage(PARAM_DISK_WARN_FRRE_PERCENT,
					   DEFAULT_DISK_WARN_FRRE_PERCENT);
      paramDFWarn = PlatformUtil.DF.makeThreshold(minMB, minPer);
      minMB = config.getInt(PARAM_DISK_FULL_FRRE_MB,
				DEFAULT_DISK_FULL_FRRE_MB);
      minPer = config.getPercentage(PARAM_DISK_FULL_FRRE_PERCENT,
					   DEFAULT_DISK_FULL_FRRE_PERCENT);
      paramDFFull = PlatformUtil.DF.makeThreshold(minMB, minPer);
    }
    if (changedKeys.contains(PARAM_SIZE_CALC_MAX_LOAD)) {
      sizeCalcMaxLoad = config.getPercentage(PARAM_SIZE_CALC_MAX_LOAD,
					     DEFAULT_SIZE_CALC_MAX_LOAD);
    }
    if (changedKeys.contains(PREFIX)) {
      maxUnusedDirSearch = config.getInt(PARAM_MAX_UNUSED_DIR_SEARCH,
					 DEFAULT_MAX_UNUSED_DIR_SEARCH);
      isStatefulUnusedDirSearch =
	config.getBoolean(PARAM_IS_STATEFUL_UNUSED_DIR_SEARCH,
			  DEFAULT_IS_STATEFUL_UNUSED_DIR_SEARCH);
      enableLongComponents = config.getBoolean(PARAM_ENABLE_LONG_COMPONENTS,
					       DEFAULT_ENABLE_LONG_COMPONENTS);
      enableLongComponentsCompatibility =
	config.getBoolean(PARAM_ENABLE_LONG_COMPONENTS_COMPATIBILITY,
			  DEFAULT_ENABLE_LONG_COMPONENTS_COMPATIBILITY);
      maxComponentLength = config.getInt(PARAM_MAX_COMPONENT_LENGTH,
					 DEFAULT_MAX_COMPONENT_LENGTH);
      checkUnnormalized =
	(CheckUnnormalizedMode)
	config.getEnum(CheckUnnormalizedMode.class,
		       PARAM_CHECK_UNNORMALIZED, DEFAULT_CHECK_UNNORMALIZED);

      paramMoveDeletedAusTo = config.get(PARAM_MOVE_DELETED_AUS_TO,
                                         DEFAULT_MOVE_DELETED_AUS_TO);

      paramDeleteThreadInterval = config.getTimeInterval(
          PARAM_DELETEAUS_INTERVAL,
          DEFAULT_DELETEAUS_INTERVAL);

      paramDeleteAusThreads = config.getInt(PARAM_DELETEAUS_THREADS,
                                            DEFAULT_DELETEAUS_THREADS);

      if (changedKeys.contains(PARAM_DELETEAUS_INTERVAL)) {
        // Wake the delete threads so they pick up the new interval
        pokeDeleteAusThreads();
      }
      if (changedKeys.contains(PARAM_DELETEAUS_THREADS)
          && isDeleteAusThreadsRunning()) {
        // Restart the pool with the new thread count
        startOrKickDeleteAusThreads();
      }

      if (isInited()) {
        if (changedKeys.contains(MigrationManager.PARAM_IS_IN_MIGRATION_MODE) ||
            changedKeys.contains(MigrateContent.PARAM_DELETE_AFTER_MIGRATION) ||
            changedKeys.contains(PARAM_MOVE_DELETED_AUS_TO)) {

          boolean deleteAusAfterMigration =
            config.getBoolean(MigrateContent.PARAM_DELETE_AFTER_MIGRATION,
                              MigrateContent.DEFAULT_DELETE_AFTER_MIGRATION);

          if (getDaemon().getMigrationManager().isRealMigrationMode()
              && deleteAusAfterMigration
              && !StringUtil.isNullString(paramMoveDeletedAusTo)) {
            startOrKickDeleteAusThreads();
          } else {
            stopDeleteAusThreads();
          }
        }
      }
    }
  }

  public static boolean isEnableLongComponents() {
    return enableLongComponents;
  }

  public static boolean isEnableLongComponentsCompatibility() {
    return enableLongComponentsCompatibility;
  }

  public static int getMaxComponentLength() {
    return maxComponentLength;
  }

  public static CheckUnnormalizedMode getCheckUnnormalizedMode() {
    return checkUnnormalized;
  }

  /** Return list of known repository names.  Needs a registration
   * mechanism if ever another repository implementation. */
  public List<String> getRepositoryList() {
    return repoList;
  }

  public PlatformUtil.DF getRepositoryDF(String repoName) {
    String path = LockssRepositoryImpl.getLocalRepositoryPath(repoName);
    log.debug("path: " + path);
      return platInfo.getDF(path);
  }

  public LinkedMap<String,PlatformUtil.DF> getRepositoryMap() {
    LinkedMap<String,PlatformUtil.DF> repoMap = new LinkedMap<>();
    for (String repo : getRepositoryList()) {
      repoMap.put(repo, getRepositoryDF(repo));
    }
    return repoMap;
  }

  public int getNumAuDirs(String repoSpec) {
     LockssRepositoryImpl.LocalRepository lrepo =
       LockssRepositoryImpl.getLocalRepository(LockssRepositoryImpl.getLocalRepositoryPath(repoSpec));
     return lrepo.getNumAuDirs();
  }

  public void updateAuIdMaps() {
    for (String repo : getRepositoryList()) {
      LockssRepositoryImpl.updateAuIdMap(repo);
    }
  }

  public void updateAuIdMap(String repoSpec) {
    LockssRepositoryImpl.updateAuIdMap(LockssRepositoryImpl.getLocalRepositoryPath(repoSpec));
  }

  public void rescanAuIdMap(String repoSpec) {
    LockssRepositoryImpl.rescanAuIdMap(LockssRepositoryImpl.getLocalRepositoryPath(repoSpec));
  }

  public String findLeastFullRepository() {
    return findLeastFullRepository(getRepositoryMap());
  }

  public String findLeastFullRepository(Map<String,PlatformUtil.DF> repoMap) {
    String mostFree = null;
    for (String repo : repoMap.keySet()) {
      PlatformUtil.DF df = repoMap.get(repo);
      if (df != null) {
	if (mostFree == null ||
	    (repoMap.get(mostFree)).getAvail() < df.getAvail()) {
	  mostFree = repo;
	}
      }
    }
    return mostFree;
  }

  public PlatformUtil.DF getDiskWarnThreshold() {
    return paramDFWarn;
  }

  public PlatformUtil.DF getDiskFullThreshold() {
    return paramDFFull;
  }

  public static int getMaxUnusedDirSearch() {
    return maxUnusedDirSearch;
  }

  public static boolean isStatefulUnusedDirSearch() {
    return isStatefulUnusedDirSearch;
  }

  LockssRepositoryImpl.LocalRepository getLocalRepository(String repoPath) {
    return LockssRepositoryImpl.getLocalRepository(repoPath);
  }

  public List<String> findExistingRepositoriesFor(String auid) {
    List<String> res = null;
    for (String repoName : getRepositoryList()) {
      String path = LockssRepositoryImpl.getLocalRepositoryPath(repoName);
      if (LockssRepositoryImpl.doesAuDirExist(auid, path)) {
	if (res == null) {
	  res = new ArrayList<>();
	}
	res.add(repoName);
      }
    }
    return res == null ? Collections.emptyList() : res;
  }

  public List<String> findExistingAuBaseDirsFor(String auid) {
    List<String> res = new ArrayList<>();
    for (String repoName : findExistingRepositoriesFor(auid)) {
      res.add(StringUtil.replaceFirst(repoName, "local:", ""));
    }
    return res;
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

  public UniqueRefLruCache getSuspectVersionsCache() {
    return suspectVersionsCache;
  }

  // Background thread to (re)calculate AU size and disk usage.

  private Set sizeCalcQueue = new HashSet();
  private BinarySemaphore sizeCalcSem = new BinarySemaphore();
  private SizeCalcThread sizeCalcThread;
  /** Guards deleteAusThreads.  A separate lock so that starting or
   * stopping the threads doesn't hold the monitor that claimNextDir()
   * needs. */
  private final Object deleteThreadsLock = new Object();
  private List<DeleteAusThread> deleteAusThreads =
    new ArrayList<DeleteAusThread>();

  /** engqueue a size calculation for the AU */
  public void queueSizeCalc(ArchivalUnit au) {
    queueSizeCalc(AuUtil.getAuRepoNode(au));
  }

  /** engqueue a size calculation for the node */
  public void queueSizeCalc(RepositoryNode node) {
    synchronized (sizeCalcQueue) {
      if (sizeCalcQueue.add(node)) {
	log.debug2("Queue size calc: " + node);
	startOrKickSizeCalcThread();
      }
    }
  }

  public int sizeCalcQueueLen() {
    synchronized (sizeCalcQueue) {
      return sizeCalcQueue.size();
    }
  }

  void startOrKickSizeCalcThread() {
    if (sizeCalcThread == null) {
      log.debug2("Starting thread");
      sizeCalcThread = new SizeCalcThread();
      sizeCalcThread.start();
      sizeCalcThread.waitRunning();
    }
    sizeCalcSem.give();
  }

  void stopSizeCalcThread() {
    if (sizeCalcThread != null) {
      log.debug2("Stopping thread");
      sizeCalcThread.stopSizeCalc();
      sizeCalcThread = null;
    }
  }

  /** Return the number of delete threads currently running */
  int getDeleteAusThreadCount() {
    synchronized (deleteThreadsLock) {
      return deleteAusThreads.size();
    }
  }

  boolean isDeleteAusThreadsRunning() {
    synchronized (deleteThreadsLock) {
      return !deleteAusThreads.isEmpty();
    }
  }

  /** Start the delete threads if they aren't running, restart them if
   * the configured thread count has changed, and wake any that are
   * idle. */
  void startOrKickDeleteAusThreads() {
    synchronized (deleteThreadsLock) {
      int nThreads = Math.max(1, paramDeleteAusThreads);
      if (!deleteAusThreads.isEmpty() && deleteAusThreads.size() != nThreads) {
        log.debug("Delete AUs thread count changed to " + nThreads);
        stopDeleteAusThreads();
      }
      if (deleteAusThreads.isEmpty()) {
        // nThreads changed or never started any threads
        log.debug2("Starting " + nThreads + " delete AUs thread(s)");
        synchronized (this) {
          lastFailedRetry = TimeBase.nowMs();
        }
        for (int ix = 0; ix < nThreads; ix++) {
          DeleteAusThread th = new DeleteAusThread(ix);
          deleteAusThreads.add(th);
          th.start();
          th.waitRunning();
        }
      }
    }
    pokeDeleteAusThreads();
  }

  /** Ask the delete threads to exit.  Threads in the middle of a delete
   * finish it first, so this returns before they have all exited. */
  void stopDeleteAusThreads() {
    synchronized (deleteThreadsLock) {
      if (!deleteAusThreads.isEmpty()) {
        log.debug2("Stopping delete AUs threads");
        for (DeleteAusThread th : deleteAusThreads) {
          th.stopDeleteThread();
        }
        deleteAusThreads.clear();
      }
    }
    failedDeletes.clear();
    // Wake the idle ones so they notice
    pokeDeleteAusThreads();
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
