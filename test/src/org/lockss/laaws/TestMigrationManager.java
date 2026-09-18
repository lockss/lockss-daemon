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

package org.lockss.laaws;

import org.lockss.servlet.MigrateSettings;
import org.lockss.test.LockssTestCase;
import org.lockss.util.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class TestMigrationManager extends LockssTestCase {
  private static Logger log = Logger.getLogger("TestMigrationManager");

  private MigrationManager migrationMgr;

  public void setUp() throws Exception {
    super.setUp();
    migrationMgr = getMockLockssDaemon().getMigrationManager();
  }

  // Directly poke MigrationManager's private mover/runner fields so
  // we can exercise getStatus()/getErrorsPage() as they behave while
  // a migration is "running", without actually running one (which
  // would require live V2 services).  MigrationManager has no public
  // setters for these -- they're normally set by startRunner() -- and
  // PrivilegedAccessor (test/src/org/lockss/test/PrivilegedAccessor)
  // only exposes getValue()/invokeMethod(), not a field setter, so
  // this uses java.lang.reflect directly.
  private void setPrivateField(Object obj, String fieldName, Object value)
      throws Exception {
    Field f = obj.getClass().getDeclaredField(fieldName);
    f.setAccessible(true);
    f.set(obj, value);
  }

  private V2AuMover makeRunningMover() throws Exception {
    V2AuMover mover = new V2AuMover();
    MigrationManager.Runner runner =
      migrationMgr.new Runner(Collections.<V2AuMover.Args>emptyList());
    setPrivateField(migrationMgr, "mover", mover);
    setPrivateField(migrationMgr, "runner", runner);
    return mover;
  }

  public void testMapFromCsvStream() throws Exception {
    final String CSV = "Name,Value\n" +
        "org.lockss.contentui.port,24680\n" +
        "org.lockss.contentui.start,true\n" +
        "org.lockss.proxy.port,24670\n" +
        "org.lockss.proxy.start,true\n" +
        "org.lockss.localV3Identity,TCP:[127.0.0.1]:9729\n" +
        "org.lockss.metadataDbManager.datasource.className,org.apache.derby.jdbc.ClientDataSource\n" +
        "org.lockss.metadataDbManager.datasource.dbcp.enabled,true\n" +
        "org.lockss.metadataDbManager.datasource.portNumber,1527\n";

    InputStream csvStream = new ByteArrayInputStream(CSV.getBytes(StandardCharsets.UTF_8));

    Properties csvMap = migrationMgr.propsFromCsv(csvStream);

    assertEquals("24680", csvMap.get("org.lockss.contentui.port"));
    assertEquals("true", csvMap.get("org.lockss.contentui.start"));
    assertEquals("24670", csvMap.get("org.lockss.proxy.port"));
    assertEquals("true", csvMap.get("org.lockss.proxy.start"));
    assertEquals("TCP:[127.0.0.1]:9729", csvMap.get("org.lockss.localV3Identity"));
    assertEquals("org.apache.derby.jdbc.ClientDataSource",
        csvMap.get("org.lockss.metadataDbManager.datasource.className"));
    assertEquals("true", csvMap.get("org.lockss.metadataDbManager.datasource.dbcp.enabled"));
    assertEquals("1527", csvMap.get("org.lockss.metadataDbManager.datasource.portNumber"));
  }

  // -----------------------------------------------------------------
  // Tests for GitHub issue #743 ("Make the migration errors list
  // incremental"): getStatus() must no longer carry the (potentially
  // very long) error/warning list on every poll while a migration is
  // running -- only a count -- and the messages themselves must be
  // fetchable a page at a time via getErrorsPage(), the same idiom
  // already used for the finished-AU list (getFinishedPage()).
  // -----------------------------------------------------------------

  public void testStatusOmitsFullErrorListWhileRunning() throws Exception {
    V2AuMover mover = makeRunningMover();
    for (int i = 0; i < 10; i++) {
      mover.addError("err " + i);
    }

    Map stat = migrationMgr.getStatus();
    assertFalse("getStatus() must not include the 'errors' key while "
                + "running -- the full list must not be sent on every poll",
                stat.containsKey("errors"));
    assertTrue(stat.containsKey("errors_count"));
    assertEquals(10, stat.get("errors_count"));
  }

  public void testErrorsPageIncrementalWhileRunning() throws Exception {
    V2AuMover mover = makeRunningMover();
    for (int i = 0; i < 5; i++) {
      mover.addError("err " + i);
    }

    // First poll (client has seen nothing yet).
    Map page1 = migrationMgr.getErrorsPage(0, 3);
    assertEquals(ListUtil.list("err 0", "err 1", "err 2"), page1.get("errors_page"));
    assertEquals(0, page1.get("errors_index"));

    // More errors arrive between polls.
    mover.addError("err 5");

    // Second poll starts where the client left off (3 seen so far).
    Map page2 = migrationMgr.getErrorsPage(3, 10);
    assertEquals(ListUtil.list("err 3", "err 4", "err 5"), page2.get("errors_page"));
    assertEquals(3, page2.get("errors_index"));
  }

  public void testStatusIdleWithNoError() throws Exception {
    Map stat = migrationMgr.getStatus();
    assertFalse(stat.containsKey("errors"));
    assertEquals(0, stat.get("errors_count"));
    Map page = migrationMgr.getErrorsPage(0, 10);
    assertEmpty((List) page.get("errors_page"));
  }

  public void testStatusIdleWithIdleError() throws Exception {
    setPrivateField(migrationMgr, "idleError", "V2AuMover failed to start: boom");

    Map stat = migrationMgr.getStatus();
    assertFalse(stat.containsKey("errors"));
    assertEquals(1, stat.get("errors_count"));

    Map page = migrationMgr.getErrorsPage(0, 10);
    assertEquals(ListUtil.list("V2AuMover failed to start: boom"),
                page.get("errors_page"));

    // Once the client has already fetched the single idle error,
    // further polls at index 1 return nothing new.
    Map page2 = migrationMgr.getErrorsPage(1, 10);
    assertEmpty((List) page2.get("errors_page"));
  }
}
