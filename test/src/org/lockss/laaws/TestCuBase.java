/*
 * 2026, Board of Trustees of Leland Stanford Jr. University,
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.lockss.laaws;

import java.io.*;
import java.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.repository.*;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * Tests that the CU versions the migrator builds from damaged V1
 * repository nodes (lockss-engineering-tasks#751) are repaired before
 * they're mapped to V2 names.
 */
public class TestCuBase extends LockssTestCase {
  static final String URL = "http://www.example.com/testDir/leaf1";

  MockLockssDaemon theDaemon;
  MyMockArchivalUnit mau;
  LockssRepository repo;
  V2AuMover mover;

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    mau = new MyMockArchivalUnit();
    MockPlugin plugin = new MockPlugin();
    plugin.initPlugin(theDaemon);
    mau.setPlugin(plugin);
    repo = theDaemon.getLockssRepository(mau);
    theDaemon.getNodeManager(mau).startService();
    mover = new V2AuMover();
  }

  public void tearDown() throws Exception {
    repo.stopService();
    super.tearDown();
  }

  RepositoryNode createVersions(int n) throws Exception {
    RepositoryNode node =
      TestRepositoryNodeImpl.createLeaf(repo, URL, "content 1", null);
    for (int ix = 2; ix <= n; ix++) {
      TestRepositoryNodeImpl.createContentVersion(node, "content " + ix, null);
    }
    return node;
  }

  CuBase buildCompatMap() {
    CachedUrl cu = mau.makeCachedUrl(URL);
    MigrationTask task = MigrationTask.copyCuVersions(mover, mau, cu)
      .setAuStatus(new AuStatus(mover, mau));
    CuBase cb = new CuBase(mover, task);
    cb.buildCompatMap(cu);
    return cb;
  }

  List<Integer> mappedVersions(CuBase cb) {
    List<Integer> res = new ArrayList<Integer>();
    for (CachedUrl cu : cb.mappedCus.get(URL)) {
      res.add(cu.getVersion());
      AuUtil.safeRelease(cu);
    }
    return res;
  }

  // Leaf URLs under prefix, listed as CachedUrlSet iteration lists them
  List<String> nodeUrls(String prefix) {
    CachedUrlSet cus =
      mau.makeCachedUrlSet(new RangeCachedUrlSetSpec(prefix));
    List<String> res = new ArrayList<String>();
    for (Iterator<CachedUrlSetNode> it = cus.contentHashIterator();
         it.hasNext(); ) {
      CachedUrlSetNode cusn = it.next();
      if (cusn.isLeaf()) {
        res.add(cusn.getUrl());
      }
    }
    return res;
  }

  List<String> errors(CuBase cb) {
    return cb.task.getAuStatus().getErrors();
  }

  public void testHealthyNode() throws Exception {
    createVersions(3);
    CuBase cb = buildCompatMap();
    assertEquals(ListUtil.list(3, 2, 1), mappedVersions(cb));
    assertEmpty(errors(cb));
  }

  // Without the repair the versions were [1, 2, 1] (version 0 skipped),
  // so the copy of the second version 1 failed with a 409 from V2
  public void testDuplicateVersionNumberRepaired() throws Exception {
    RepositoryNode node = createVersions(3);
    TestRepositoryNodeImpl.makeDuplicateVersionState(TestRepositoryNodeImpl.getContentDir(node));
    CuBase cb = buildCompatMap();
    assertEquals(ListUtil.list(3, 2, 1), mappedVersions(cb));
    assertEmpty(errors(cb));
  }

  public void testUnusableCurrentPropsRepaired() throws Exception {
    RepositoryNode node = createVersions(3);
    File contentDir = TestRepositoryNodeImpl.getContentDir(node);
    TestRepositoryNodeImpl.zeroFile(new File(contentDir, "current.props"));
    CuBase cb = buildCompatMap();
    assertEquals(ListUtil.list(2, 1), mappedVersions(cb));
    assertEmpty(errors(cb));
  }

  // A version whose props were lost isn't copied, and is reported as a
  // migration error (as any version whose props can't be read is)
  public void testVersionWithZeroedPropsNotCopied() throws Exception {
    RepositoryNode node = createVersions(3);
    File contentDir = TestRepositoryNodeImpl.getContentDir(node);
    TestRepositoryNodeImpl.zeroFile(new File(contentDir, "1.props"));
    CuBase cb = buildCompatMap();
    assertEquals(ListUtil.list(3, 2), mappedVersions(cb));
    assertEquals(1, errors(cb).size());
    assertMatchesRE("Couldn't read props for .*v=1", errors(cb).get(0));
    assertTrue(new File(contentDir, "1.ERROR").exists());

    // Not listed, so not reported, the next time
    cb = buildCompatMap();
    assertEquals(ListUtil.list(3, 2), mappedVersions(cb));
    assertEmpty(errors(cb));
  }

  // A node deactivated by a failed read of its missing 'current' is
  // reactivated and repaired, so the migrator finds and copies it
  public void testDeactivatedLostCurrentRepaired() throws Exception {
    RepositoryNodeImpl node = (RepositoryNodeImpl)createVersions(3);
    TestRepositoryNodeImpl.makeLostCurrentState(
      TestRepositoryNodeImpl.getContentDir(node));
    TestRepositoryNodeImpl.deactivateWithoutCurrent(node);
    TestRepositoryNodeImpl.clearNodeCache(repo);
    // The migrator iterates the AU's CachedUrlSet, whose node listing
    // leaves out inactive leaves
    assertEquals(ListUtil.list(URL), nodeUrls("http://www.example.com/testDir"));
    CuBase cb = buildCompatMap();
    assertEquals(ListUtil.list(2, 1), mappedVersions(cb));
    assertEmpty(errors(cb));
  }

  static class MyMockArchivalUnit extends MockArchivalUnit {
    public CachedUrlSet makeCachedUrlSet(CachedUrlSetSpec cuss) {
      return new BaseCachedUrlSet(this, cuss);
    }

    public CachedUrl makeCachedUrl(String url) {
      return new BaseCachedUrl(this, url);
    }
  }
}
