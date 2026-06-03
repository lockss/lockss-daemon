package org.lockss.laaws;

import org.lockss.servlet.MigrateSettings;
import org.lockss.test.LockssTestCase;
import org.lockss.util.CompoundLinearSlope;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

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



}
