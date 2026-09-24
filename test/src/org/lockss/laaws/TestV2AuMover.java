package org.lockss.laaws;

import org.lockss.repository.RepositoryManager;
import org.lockss.servlet.MigrateSettings;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.util.CompoundLinearSlope;
import org.lockss.util.PlatformUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class TestV2AuMover extends LockssTestCase {
//   private static Logger log = Logger.getLogger("TestV2AuMover");

  public void testIsEqualUpToFinalSlash() {
    assertFalse(V2AuMover.isEqualUpToFinalSlash(null, null));
    assertFalse(V2AuMover.isEqualUpToFinalSlash("foo", null));
    assertFalse(V2AuMover.isEqualUpToFinalSlash(null, "foo"));
    assertFalse(V2AuMover.isEqualUpToFinalSlash(null, "foo/"));
    assertFalse(V2AuMover.isEqualUpToFinalSlash("foo", "foo"));
    assertTrue(V2AuMover.isEqualUpToFinalSlash("foo", "foo/"));
    assertFalse(V2AuMover.isEqualUpToFinalSlash("foo", "foo//"));
    assertFalse(V2AuMover.isEqualUpToFinalSlash("foo/", "foo"));
    assertTrue(V2AuMover.isEqualUpToFinalSlash("foo", "foo/"));
  }

  public void testDefaultCurves() {
    new CompoundLinearSlope(V2AuMover.DEFAULT_DISK_SPACE_BYTES_CURVE);
    new CompoundLinearSlope(V2AuMover.DEFAULT_DISK_SPACE_ARTIFACTS_CURVE);
    new CompoundLinearSlope(V2AuMover.DEFAULT_DB_SIZE_CHECK_CURVE);
  }

  // #721 prototype: checkDiskSpaceOrAbort()

  private V2AuMover makeMoverWithFakeRepoMgr(final PlatformUtil.DF reading,
                                             final PlatformUtil.DF threshold,
                                             final AtomicInteger dfCalls,
                                             final AtomicInteger pokeCalls) {
    V2AuMover mover = new V2AuMover();
    mover.repoMgr = new RepositoryManager() {
      public PlatformUtil.DF getRepositoryDF(String repoName) {
        dfCalls.incrementAndGet();
        return reading;
      }
      public PlatformUtil.DF getAuMoverPauseThreshold() {
        return threshold;
      }
      public void pokeDeleteAusThreads() {
        pokeCalls.incrementAndGet();
      }
    };
    return mover;
  }

  public void testCheckDiskSpaceOrAbortProceedsWhenSpaceOk() throws Exception {
    AtomicInteger dfCalls = new AtomicInteger();
    AtomicInteger pokeCalls = new AtomicInteger();
    // Plenty of room: reading's avail is far above the threshold's.
    PlatformUtil.DF reading = PlatformUtil.DF.makeThreshold(100000, 0.0);
    PlatformUtil.DF threshold = PlatformUtil.DF.makeThreshold(20000, 0.0);
    V2AuMover mover = makeMoverWithFakeRepoMgr(reading, threshold,
                                               dfCalls, pokeCalls);
    mover.diskPauseMaxAttempts = 3;
    mover.diskPauseRetryInterval = 1;

    mover.checkDiskSpaceOrAbort(new MockArchivalUnit());

    assertEquals("Should return after a single check when space is OK",
                1, dfCalls.get());
    assertEquals("Should not need to nudge deletion when space is OK",
                0, pokeCalls.get());
  }

  public void testCheckDiskSpaceOrAbortThrowsAfterMaxAttempts() throws Exception {
    AtomicInteger dfCalls = new AtomicInteger();
    AtomicInteger pokeCalls = new AtomicInteger();
    // Never enough room: reading's avail stays below the threshold's.
    PlatformUtil.DF reading = PlatformUtil.DF.makeThreshold(1000, 0.0);
    PlatformUtil.DF threshold = PlatformUtil.DF.makeThreshold(20000, 0.0);
    V2AuMover mover = makeMoverWithFakeRepoMgr(reading, threshold,
                                               dfCalls, pokeCalls);
    mover.diskPauseMaxAttempts = 3;
    mover.diskPauseRetryInterval = 1;

    assertEquals(0, mover.totalAusWithErrors);

    try {
      mover.checkDiskSpaceOrAbort(new MockArchivalUnit());
      fail("Should have thrown InsufficientDiskSpaceException");
    } catch (V2AuMover.InsufficientDiskSpaceException e) {
      // expected
    }

    assertEquals("Should check disk space once per attempt",
                3, dfCalls.get());
    assertEquals("Should nudge deletion once per attempt",
                3, pokeCalls.get());
    assertEquals("Should record the AU in the migration error log so it "
                + "can be retried later",
                1, mover.totalAusWithErrors);
  }

}
