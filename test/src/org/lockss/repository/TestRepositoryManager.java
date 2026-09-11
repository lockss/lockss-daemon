/*
 * $Id$
 */

/*
 Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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
import java.nio.file.attribute.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.lockss.app.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

public class TestRepositoryManager extends LockssTestCase {
  private MockArchivalUnit mau;
  private MyRepositoryManager mgr;

  private MockLockssDaemon theDaemon;

  public void setUp() throws Exception {
    super.setUp();
    theDaemon = getMockLockssDaemon();
    mgr = new MyRepositoryManager();
    theDaemon.setRepositoryManager(mgr);
    mgr.initService(theDaemon);
  }

  public void tearDown() throws Exception {
    if (mgr != null) {
      mgr.stopDeleteAusThreads();
    }
    super.tearDown();
  }

  MyMockLockssRepositoryImpl makeRepo(String root) {
    MockArchivalUnit mau = new MockArchivalUnit();
    MyMockLockssRepositoryImpl repo = new MyMockLockssRepositoryImpl(root);
    theDaemon.setLockssRepository(repo, mau);
    repo.initService(theDaemon);
    repo.startService();
    return repo;
  }

  public void testConfig() throws Exception {
    MyMockLockssRepositoryImpl repo1 = makeRepo("foo");
    assertEquals(RepositoryManager.DEFAULT_MAX_PER_AU_CACHE_SIZE,
		 repo1.nodeCacheSize);

    ConfigurationUtil.setFromArgs(RepositoryManager.PARAM_MAX_PER_AU_CACHE_SIZE,
				  "4");
    MyMockLockssRepositoryImpl repo2 = makeRepo("bar");
    assertEquals(4, repo1.nodeCacheSize);
    assertEquals(4, repo2.nodeCacheSize);

    repo1.cnt = 0;
    ConfigurationUtil.setFromArgs(RepositoryManager.PARAM_MAX_PER_AU_CACHE_SIZE,
				  "37");
    assertEquals(37, repo1.nodeCacheSize);
    assertEquals(37, repo2.nodeCacheSize);
    assertEquals(1, repo1.cnt);
    // ensure setNodeCacheSize doesn't get called if param doesn't change
    ConfigurationUtil.setFromArgs(RepositoryManager.PARAM_MAX_PER_AU_CACHE_SIZE,
				  "37",
				  "org.lockss.somethingElse", "bar");
    assertEquals(1, repo1.cnt);

    PlatformUtil.DF warn = mgr.getDiskWarnThreshold();
    PlatformUtil.DF full = mgr.getDiskFullThreshold();
    assertEquals(5000 * 1024, warn.getAvail());
    assertEquals(0.98, warn.getPercent(), .00001);
    assertEquals(100 * 1024, full.getAvail());
    assertEquals(0.99, full.getPercent(), .00001);

    Properties p = new Properties();
    p.put(RepositoryManager.PARAM_DISK_WARN_FRRE_MB, "17");
    p.put(RepositoryManager.PARAM_DISK_WARN_FRRE_PERCENT, "20");
    p.put(RepositoryManager.PARAM_DISK_FULL_FRRE_MB, "7");
    p.put(RepositoryManager.PARAM_DISK_FULL_FRRE_PERCENT, "10");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    warn = mgr.getDiskWarnThreshold();
    full = mgr.getDiskFullThreshold();
    assertEquals(17 * 1024, warn.getAvail());
    assertEquals(0.80, warn.getPercent(), .00001);
    assertEquals(7 * 1024, full.getAvail());
    assertEquals(0.90, full.getPercent(), .00001);
  }

  public void testGetRepositoryList() throws Exception {
    assertEmpty(mgr.getRepositoryList());
    String tempDirPath = setUpDiskSpace();
    assertEquals(ListUtil.list("local:" + tempDirPath),
		 mgr.getRepositoryList());
    String tempdir2 = getTempDir().getAbsolutePath() + File.separator;
    ConfigurationUtil.setFromArgs("org.lockss.platform.diskSpacePaths",
				  tempdir2 + ";" + tempDirPath);
    assertEquals(ListUtil.list("local:" + tempdir2, "local:" + tempDirPath),
		 mgr.getRepositoryList());
  }

  public void testGetRepositoryDF () throws Exception {
    PlatformUtil.DF df = mgr.getRepositoryDF("local:.");
    assertNotNull(df);
  }

  public void testFindLeastFullRepository () throws Exception {
    Map repoMap = MapUtil.map("local:one", new MyDF("/one", 1000),
			      "local:two",  new MyDF("/two", 3000),
			      "local:three",  new MyDF("/three", 2000));
    mgr.setRepoMap(repoMap);

    assertEquals("local:two", mgr.findLeastFullRepository());
  }

  public void testSizeCalc () throws Exception {
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    mgr.setSem(sem);
    RepositoryNode node1 = new RepositoryNodeImpl("url1", "testDir", null);
    RepositoryNode node2 = new RepositoryNodeImpl("url2", "testDir", null);
    RepositoryNode node3 = new RepositoryNodeImpl("url3", "testDir", null);
    mgr.queueSizeCalc(node1);
    assertTrue(sem.take(TIMEOUT_SHOULDNT));
    assertEquals(ListUtil.list(node1), mgr.getNodes());
    mgr.queueSizeCalc(node2);
    mgr.queueSizeCalc(node3);
    assertTrue(sem.take(TIMEOUT_SHOULDNT));
    if (mgr.getNodes().size() < 3) {
      assertTrue(sem.take(TIMEOUT_SHOULDNT));
    }
    assertSameElements(ListUtil.list(node1, node2, node3), mgr.getNodes());
  }

  public void testSleepCalc () throws Exception {
    assertEquals(90, mgr.sleepTimeToAchieveLoad(10L, .1F));
    assertEquals(40, mgr.sleepTimeToAchieveLoad(10L, .2F));
    assertEquals(10, mgr.sleepTimeToAchieveLoad(10L, .5F));
    assertEquals(50, mgr.sleepTimeToAchieveLoad(150L, .75F));
  }

  //////////////////////////////////////////////////////////////////////
  // Deletion of AU dirs that have been moved to the "deleted AUs" dir
  //////////////////////////////////////////////////////////////////////

  /** Create root/<subdir>/... containing a nested file, return the dir
   * that should be deleted (root/<subdir>) */
  File makeAuDir(File deletedAusDir, String name) throws IOException {
    File auDir = new File(deletedAusDir, name);
    File sub = new File(auDir, "a/b/c");
    assertTrue(sub.mkdirs());
    assertTrue(new File(sub, "content").createNewFile());
    assertTrue(new File(auDir, "au_state.xml").createNewFile());
    return auDir;
  }

  /** Set up a repo root with a "MIGRATED" dir, tell the manager about
   * it, return the MIGRATED dir */
  File setUpDeletedAusDir(String rootName) throws IOException {
    File root = getTempDir(rootName);
    File migrated = new File(root, DELETED_DIR);
    assertTrue(migrated.mkdirs());
    mgr.setRepos(ListUtil.list("local:" + root.getAbsolutePath()));
    return migrated;
  }

  static final String DELETED_DIR = "MIGRATED";

  public void testGetDiskSpaceListNotConfigured() throws Exception {
    try {
      mgr.getDiskSpaceList();
      fail("getDiskSpaceList() should throw when "
	   + RepositoryManager.PARAM_MOVE_DELETED_AUS_TO + " isn't set");
    } catch (IllegalStateException e) {
    }
  }

  public void testGetDiskSpaceList() throws Exception {
    File migrated = setUpDeletedAusDir("dfList");
    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_MOVE_DELETED_AUS_TO,
				  DELETED_DIR);
    List<RepositoryManager.FileDF> fdfs = mgr.getDiskSpaceList();
    assertEquals(1, fdfs.size());
    assertEquals(migrated, fdfs.get(0).getFile());
    assertNotNull(fdfs.get(0).getDF());

    // A repo with no "deleted AUs" dir shouldn't appear
    File other = getTempDir("dfList2");
    mgr.setRepos(ListUtil.list("local:" + other.getAbsolutePath()));
    assertEmpty(mgr.getDiskSpaceList());
  }

  public void testFindDirOnMostFullDisk() throws Exception {
    File root = getTempDir("findDir");
    File dirA = new File(root, "a");
    File dirB = new File(root, "b");
    File dirEmpty = new File(root, "empty");
    assertTrue(dirA.mkdirs());
    assertTrue(dirB.mkdirs());
    assertTrue(dirEmpty.mkdirs());
    File auA = makeAuDir(dirA, "auA");
    File auB = makeAuDir(dirB, "auB");
    // Nonexistent dir, and one whose free space is unknown; neither may
    // cause trouble
    File noSuchDir = new File(root, "nosuch");

    List<RepositoryManager.FileDF> fdfs =
      (List<RepositoryManager.FileDF>)ListUtil.list
      (new RepositoryManager.FileDF(dirA, new MyDF(dirA.toString(), 2000)),
       new RepositoryManager.FileDF(dirEmpty, new MyDF(dirEmpty.toString(), 1)),
       new RepositoryManager.FileDF(noSuchDir, new MyDF(noSuchDir.toString(), 2)),
       new RepositoryManager.FileDF(dirB, new MyDF(dirB.toString(), 1000)));

    // dirEmpty and noSuchDir have the least free space but nothing to
    // delete, so the fullest disk that does is dirB
    assertEquals(auB, mgr.findDirOnMostFullDisk(fdfs));
    // Dirs already claimed by a Deleter are skipped
    mgr.activeDeletes.add(auB);
    assertEquals(auA, mgr.findDirOnMostFullDisk(fdfs));
    mgr.activeDeletes.add(auA);
    assertNull(mgr.findDirOnMostFullDisk(fdfs));
    mgr.activeDeletes.clear();
    assertEquals(auB, mgr.findDirOnMostFullDisk(fdfs));
  }

  /** DF info that couldn't be determined (null) must sort last, not NPE */
  public void testFindDirOnMostFullDiskUnknownDF() throws Exception {
    File root = getTempDir("findDir");
    File dirA = new File(root, "a");
    File dirB = new File(root, "b");
    assertTrue(dirA.mkdirs());
    assertTrue(dirB.mkdirs());
    File auA = makeAuDir(dirA, "auA");
    File auB = makeAuDir(dirB, "auB");

    List<RepositoryManager.FileDF> fdfs =
      (List<RepositoryManager.FileDF>)ListUtil.list
      (new RepositoryManager.FileDF(dirA, null),
       new RepositoryManager.FileDF(dirB, new MyDF(dirB.toString(), 1000)));
    assertEquals(auB, mgr.findDirOnMostFullDisk(fdfs));
  }

  public void testClaimNextDir() throws Exception {
    File migrated = setUpDeletedAusDir("claim");
    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_MOVE_DELETED_AUS_TO,
				  DELETED_DIR);
    // Nothing to delete
    assertNull(mgr.claimNextDir());
    File au1 = makeAuDir(migrated, "au1");
    File au2 = makeAuDir(migrated, "au2");
    // Each claim returns a different dir, and marks it claimed
    File c1 = mgr.claimNextDir();
    assertNotNull(c1);
    assertTrue(mgr.activeDeletes.contains(c1));
    File c2 = mgr.claimNextDir();
    assertNotNull(c2);
    assertNotEquals(c1, c2);
    assertSameElements(ListUtil.list(au1, au2), ListUtil.list(c1, c2));
    // Both claimed, nothing left
    assertNull(mgr.claimNextDir());
    // Deleting one releases the claim
    assertTrue(mgr.deleteClaimedDir(c1));
    assertFalse(c1.exists());
    assertFalse(mgr.activeDeletes.contains(c1));
    assertNull(mgr.claimNextDir());
  }

  public void testDeleteAusThread() throws Exception {
    File migrated = setUpDeletedAusDir("deleteAus");
    List<File> auDirs = new ArrayList<File>();
    for (int ix = 0; ix < 8; ix++) {
      auDirs.add(makeAuDir(migrated, "au" + ix));
    }
    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_MOVE_DELETED_AUS_TO,
				  DELETED_DIR,
				  RepositoryManager.PARAM_DELETEAUS_INTERVAL,
				  "100");
    mgr.startOrKickDeleteAusThreads();
    assertTrue("Timed out waiting for deletes",
	       mgr.waitDeletes(auDirs.size(), TIMEOUT_SHOULDNT * 3));
    for (File auDir : auDirs) {
      assertFalse("Should have been deleted: " + auDir, auDir.exists());
    }
    assertSameElements(auDirs, mgr.getDeleted());
    // The "deleted AUs" dir itself must survive
    assertTrue(migrated.exists());
    assertEmpty(mgr.activeDeletes);
  }

  /** A dir that shows up after the thread has gone idle must be picked
   * up when the thread is poked, without waiting for the rescan
   * interval. */
  public void testDeleteAusThreadPoke() throws Exception {
    File migrated = setUpDeletedAusDir("deleteAusPoke");
    File au1 = makeAuDir(migrated, "au1");
    // Long rescan interval, so only the poke can wake the thread
    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_MOVE_DELETED_AUS_TO,
				  DELETED_DIR,
				  RepositoryManager.PARAM_DELETEAUS_INTERVAL,
				  "1d");
    mgr.startOrKickDeleteAusThreads();
    assertTrue("Timed out waiting for first delete",
	       mgr.waitDeletes(1, TIMEOUT_SHOULDNT * 3));
    assertFalse(au1.exists());

    File au2 = makeAuDir(migrated, "au2");
    mgr.pokeDeleteAusThreads();
    assertTrue("Timed out waiting for second delete",
	       mgr.waitDeletes(2, TIMEOUT_SHOULDNT * 3));
    assertFalse(au2.exists());
  }

  /** Each thread chooses its dir at the moment it becomes free, using
   * current free space info.  With one thread, completion order is
   * selection order, so a mid-run change in which disk is fullest must
   * show up in the very next dir chosen. */
  public void testSelectionFollowsChangingDiskSpace() throws Exception {
    File rootA = getTempDir("flipA");
    File rootB = getTempDir("flipB");
    final File migA = new File(rootA, DELETED_DIR);
    final File migB = new File(rootB, DELETED_DIR);
    assertTrue(migA.mkdirs());
    assertTrue(migB.mkdirs());
    for (int ix = 0; ix < 5; ix++) {
      makeAuDir(migA, "a" + ix);
    }
    for (int ix = 0; ix < 3; ix++) {
      makeAuDir(migB, "b" + ix);
    }
    mgr.setRepos(ListUtil.list("local:" + rootA.getAbsolutePath(),
			       "local:" + rootB.getAbsolutePath()));
    // A is initially the fullest disk; flipped once two dirs are gone
    mgr.setDFMap(migA, 1000, migB, 2000);
    mgr.flipAfter(2, migA, 2000, migB, 1000);

    // One thread, so completion order is selection order
    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_MOVE_DELETED_AUS_TO,
				  DELETED_DIR,
				  RepositoryManager.PARAM_DELETEAUS_INTERVAL,
				  "100",
				  RepositoryManager.PARAM_DELETEAUS_THREADS,
				  "1");
    mgr.startOrKickDeleteAusThreads();
    assertTrue("Timed out waiting for deletes",
	       mgr.waitDeletes(8, TIMEOUT_SHOULDNT * 3));
    List<File> order = mgr.getDeleted();
    assertEquals(8, order.size());
    // Before the flip, dirs come from the fullest disk, A
    assertEquals(migA, order.get(0).getParentFile());
    assertEquals(migA, order.get(1).getParentFile());
    // Nothing is chosen ahead of time, so the flip takes effect at once
    for (int ix = 2; ix <= 4; ix++) {
      assertEquals("Dir " + ix + " (" + order.get(ix) + ") should be on the " +
		   "disk that became fullest", migB, order.get(ix).getParentFile());
    }
    // Then the rest of A
    for (int ix = 5; ix <= 7; ix++) {
      assertEquals(migA, order.get(ix).getParentFile());
    }
  }

  /** The threads must actually work in parallel.  Each delete thread
   * rendezvouses at a CyclicBarrier in deleteFinished(), and this
   * thread is a party to it too, so it trips only when all nThreads
   * delete threads are inside deleteFinished() simultaneously.  Being a
   * party is what makes this reliable: waiting on the delete count
   * instead would race with the threads still arriving. */
  public void testDeletesRunInParallel() throws Exception {
    final int nThreads = 3;
    File migrated = setUpDeletedAusDir("parallel");
    for (int ix = 0; ix < nThreads; ix++) {
      makeAuDir(migrated, "au" + ix);
    }
    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_MOVE_DELETED_AUS_TO,
				  DELETED_DIR,
				  RepositoryManager.PARAM_DELETEAUS_INTERVAL,
				  "100",
				  RepositoryManager.PARAM_DELETEAUS_THREADS,
				  Integer.toString(nThreads));
    // nThreads delete threads plus this one
    CyclicBarrier barrier = new CyclicBarrier(nThreads + 1);
    mgr.setParallelBarrier(barrier);
    mgr.startOrKickDeleteAusThreads();
    try {
      barrier.await(TIMEOUT_SHOULDNT * 3, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      fail("Deletes did not overlap: fewer than " + nThreads +
	   " threads were deleting at the same time");
    } catch (BrokenBarrierException e) {
      fail("Deletes did not overlap: a delete thread gave up waiting for " +
	   "the others");
    }
    assertTrue("Timed out waiting for deletes",
	       mgr.waitDeletes(nThreads, TIMEOUT_SHOULDNT * 3));
    for (int ix = 0; ix < nThreads; ix++) {
      assertFalse(new File(migrated, "au" + ix).exists());
    }
  }

  public void testDeleteAusThreadCountReconfig() throws Exception {
    File migrated = setUpDeletedAusDir("nthreads");
    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_MOVE_DELETED_AUS_TO,
				  DELETED_DIR,
				  RepositoryManager.PARAM_DELETEAUS_INTERVAL,
				  "100",
				  RepositoryManager.PARAM_DELETEAUS_THREADS,
				  "2");
    mgr.startOrKickDeleteAusThreads();
    assertEquals(2, mgr.getDeleteAusThreadCount());
    // Changing the count restarts the pool
    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_DELETEAUS_THREADS,
				  "3");
    assertEquals(3, mgr.getDeleteAusThreadCount());
    // and it still works
    File au1 = makeAuDir(migrated, "au1");
    mgr.pokeDeleteAusThreads();
    assertTrue("Timed out waiting for delete after reconfig",
	       mgr.waitDeletes(1, TIMEOUT_SHOULDNT * 3));
    assertFalse(au1.exists());
    mgr.stopDeleteAusThreads();
    assertEquals(0, mgr.getDeleteAusThreadCount());
  }

  /** A dir that can't be deleted must not be retried in a tight loop */
  public void testDeleteAusThreadFailure() throws Exception {
    if ("root".equals(System.getProperty("user.name"))) {
      log.info("Skipping testDeleteAusThreadFailure, running as root");
      return;
    }
    File migrated = setUpDeletedAusDir("deleteFail");
    File auDir = makeAuDir(migrated, "au1");
    // Make auDir/a/b unwritable so auDir/a/b/c can't be removed
    File locked = new File(auDir, "a/b");
    assertTrue(locked.isDirectory());
    // Long rescan interval so a retry could only come from the failure path
    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_MOVE_DELETED_AUS_TO,
				  DELETED_DIR,
				  RepositoryManager.PARAM_DELETEAUS_INTERVAL,
				  "1d");
    Files.setPosixFilePermissions(locked.toPath(),
				  PosixFilePermissions.fromString("r-xr-xr-x"));
    try {
      mgr.startOrKickDeleteAusThreads();
      assertTrue("Timed out waiting for failed delete",
		 mgr.waitFailures(1, TIMEOUT_SHOULDNT * 3));
      // Give the threads ample opportunity to retry
      TimerUtil.guaranteedSleep(500);
      assertEquals("Undeletable dir was retried", 1, mgr.getFailed().size());
      assertTrue(auDir.exists());
      assertEmpty(mgr.activeDeletes);
      assertTrue(mgr.failedDeletes.contains(auDir));
      // A dir that appears later is still deleted
      File au2 = makeAuDir(migrated, "au2");
      mgr.pokeDeleteAusThreads();
      assertTrue("Timed out waiting for delete of second dir",
		      mgr.waitDeletes(1, TIMEOUT_SHOULDNT * 3));
      assertFalse(au2.exists());
    } finally {
      Files.setPosixFilePermissions(locked.toPath(),
				    PosixFilePermissions.fromString("rwxr-xr-x"));
    }
  }

  /** A dir that failed to delete must be retried at the next rescan,
   * and must succeed once whatever blocked it is gone. */
  public void testFailedDeleteRetriedAndSucceeds() throws Exception {
    if ("root".equals(System.getProperty("user.name"))) {
      log.info("Skipping testFailedDeleteRetriedAndSucceeds, running as root");
      return;
    }
    File migrated = setUpDeletedAusDir("retryOk");
    File auDir = makeAuDir(migrated, "au1");
    // Make auDir/a/b unwritable so auDir/a/b/c can't be removed
    File locked = new File(auDir, "a/b");
    assertTrue(locked.isDirectory());
    // Short rescan interval so the retry comes quickly.  One thread, so
    // the retry is unambiguous.
    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_MOVE_DELETED_AUS_TO,
        DELETED_DIR,
        RepositoryManager.PARAM_DELETEAUS_INTERVAL,
        "200",
        RepositoryManager.PARAM_DELETEAUS_THREADS,
        "1");
    Files.setPosixFilePermissions(locked.toPath(),
        PosixFilePermissions.fromString("r-xr-xr-x"));
    try {
      mgr.startOrKickDeleteAusThreads();
      assertTrue("Timed out waiting for the delete to fail",
          mgr.waitFailures(1, TIMEOUT_SHOULDNT * 3));
      assertTrue(auDir.exists());
      assertTrue(mgr.failedDeletes.contains(auDir));
      assertEmpty(mgr.getDeleted());

      // Unblock it.  The next rescan must retry it, and succeed.
      Files.setPosixFilePermissions(locked.toPath(),
          PosixFilePermissions.fromString("rwxr-xr-x"));
      assertTrue("Failed dir was never retried",
          mgr.waitDeletes(1, TIMEOUT_SHOULDNT * 3));
      assertEquals(ListUtil.list(auDir), mgr.getDeleted());
      assertFalse(auDir.exists());
      assertEmpty(mgr.failedDeletes);
    } finally {
      if (locked.isDirectory()) {
        Files.setPosixFilePermissions(locked.toPath(),
            PosixFilePermissions.fromString("rwxr-xr-x"));
      }
    }
  }

  /** stopDeleteAusThread() must leave the manager able to start again */
  public void testStopAndRestartDeleteAusThread() throws Exception {
    File migrated = setUpDeletedAusDir("restart");
    File au1 = makeAuDir(migrated, "au1");
    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_MOVE_DELETED_AUS_TO,
				  DELETED_DIR,
				  RepositoryManager.PARAM_DELETEAUS_INTERVAL,
				  "100");
    mgr.startOrKickDeleteAusThreads();
    assertTrue(mgr.waitDeletes(1, TIMEOUT_SHOULDNT * 3));
    assertFalse(au1.exists());
    mgr.stopDeleteAusThreads();
    // Idempotent
    mgr.stopDeleteAusThreads();

    File au2 = makeAuDir(migrated, "au2");
    mgr.startOrKickDeleteAusThreads();
    assertTrue("Timed out waiting for delete after restart",
	       mgr.waitDeletes(2, TIMEOUT_SHOULDNT * 3));
    assertFalse(au2.exists());
  }

  class MyRepositoryManager extends RepositoryManager {
    List nodes = new ArrayList();
    SimpleBinarySemaphore sem;
    List repos;
    Map repoMap;
    Map<File,Integer> dfMap;
    volatile CyclicBarrier parallelBarrier;
    int flipAfterCnt = -1;
    Map<File,Integer> flipMap;
    List<File> deleted = Collections.synchronizedList(new ArrayList<File>());
    List<File> failed = Collections.synchronizedList(new ArrayList<File>());
    BinarySemaphore deleteDoneSem = new BinarySemaphore();

    @Override
    protected void deleteFinished(File dir, boolean success) {
      if (success) {
        deleted.add(dir);
      } else {
        failed.add(dir);
      }
      if (flipMap != null && deleted.size() >= flipAfterCnt) {
        dfMap = flipMap;
        flipMap = null;
      }
      CyclicBarrier barrier = parallelBarrier;
      if (barrier != null) {
        // Trips only if every delete thread is here at the same time.
        // Breaking it on timeout is what the waiting test thread sees.
        try {
          barrier.await(TIMEOUT_SHOULDNT, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
          // The others never arrived, so the deletes were serialized
        } catch (BrokenBarrierException e) {
          // Another party already gave up
        } catch (InterruptedException e) {
        }
      }
      deleteDoneSem.give();
    }

    void setParallelBarrier(CyclicBarrier barrier) {
      parallelBarrier = barrier;
    }

    /** Report canned free space for the given dirs */
    void setDFMap(File d1, int avail1, File d2, int avail2) {
      Map<File,Integer> map = new HashMap<File,Integer>();
      map.put(d1, avail1);
      map.put(d2, avail2);
      dfMap = map;
    }

    /** Switch to different canned free space once n dirs are deleted */
    void flipAfter(int n, File d1, int avail1, File d2, int avail2) {
      Map<File,Integer> map = new HashMap<File,Integer>();
      map.put(d1, avail1);
      map.put(d2, avail2);
      flipAfterCnt = n;
      flipMap = map;
    }

    @Override
    PlatformUtil.DF getDF(File dir) {
      Map<File,Integer> map = dfMap;
      if (map != null) {
	Integer avail = map.get(dir);
	if (avail != null) {
	  return new MyDF(dir.toString(), avail.intValue());
	}
      }
      return super.getDF(dir);
    }

    List<File> getDeleted() {
      return new ArrayList<File>(deleted);
    }

    List<File> getFailed() {
      return new ArrayList<File>(failed);
    }

    /** Wait until at least n dirs have been deleted, or the timeout
     * expires. */
    boolean waitDeletes(int n, long timeout) throws InterruptedException {
      return waitList(deleted, n, timeout);
    }

    /** Wait until at least n dirs have failed to delete, or the timeout
     * expires. */
    boolean waitFailures(int n, long timeout) throws InterruptedException {
      return waitList(failed, n, timeout);
    }

    private boolean waitList(List<File> lst, int n, long timeout)
        throws InterruptedException {
      Deadline deadline = Deadline.in(timeout);
      while (lst.size() < n) {
        if (!deleteDoneSem.take(deadline)) {
          return lst.size() >= n;
        }
      }
      return true;
    }

    void setSem(SimpleBinarySemaphore sem) {
      this.sem = sem;
    }
    List getNodes() {
      return nodes;
    }
    void doSizeCalc(RepositoryNode node) {
      TimerUtil.guaranteedSleep(10);
      nodes.add(node);
      sem.give();
    }    
    public List<String> getRepositoryList() {
      if (repos != null) return repos;
      return super.getRepositoryList();
    }
    public void setRepos(List repos) {
      this.repos = repos;
    }
    public PlatformUtil.DF getRepositoryDF(String repoName) {
      if (repoMap != null) return (PlatformUtil.DF)repoMap.get(repoName);
      return super.getRepositoryDF(repoName);
    }
    public void setRepoMap(Map<String,PlatformUtil.DF> repoMap) {
      List repos = new ArrayList();
      this.repoMap = repoMap;
      for (String repo : repoMap.keySet()) {
	repos.add(repo);
      }
      setRepos(repos);
    }
  }

  class MyMockLockssRepositoryImpl extends LockssRepositoryImpl {
    int nodeCacheSize = 0;
    int cnt = 0;

    public MyMockLockssRepositoryImpl(String root) {
      super(root);
    }

    public void setNodeCacheSize(int size) {
      nodeCacheSize = size;
      cnt++;
    }
  }
  class MyDF extends PlatformUtil.DF {
    MyDF(String path, int avail) {
      super();
      this.path = path;
      this.avail = avail;
    }
  }
}
