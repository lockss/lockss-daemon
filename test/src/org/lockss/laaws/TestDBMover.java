/*

Copyright (c) 2021-2025 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.db.DbManager;
import org.lockss.metadata.MetadataManager;
import org.lockss.remote.RemoteApi;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import org.lockss.util.TimeBase;

import static org.mockito.Mockito.*;

/**
 * Tests for DBMover, including migration of a database with multiple tables.
 *
 * Testable seams:
 * <ul>
 *   <li>{@code ensureIPv4Host()} — static utility, no deps</li>
 *   <li>{@code initParams()} — config validation, package-private</li>
 *   <li>early-exit when DB already moved — tested via {@code run()}</li>
 *   <li>{@code copyPostgresDb()} — package-private; tested with a subclass
 *       that replaces {@code pg_dump}/{@code psql} with shell-script stubs so
 *       the actual pipe/thread mechanism runs against controlled SQL data</li>
 *   <li>{@code openV2Connection()} — package-private; overridden in tests to
 *       return a mock {@link java.sql.Connection}, avoiding real PG connections</li>
 *   <li>{@code sleepUntilDeadline()} — package-private; overridden in tests to
 *       check the deadline without sleeping, so timeout tests run instantly</li>
 * </ul>
 */
public class TestDBMover extends LockssTestCase {

  private MockLockssDaemon mockDaemon;
  private V2AuMover        mockAuMover;
  private MigrationManager mockMigrationMgr;
  private DbManager        mockDbManager;
  private MetadataManager  mockMetadataMgr;
  private RemoteApi        mockRemoteApi;
  private MigrationTask    task;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    mockDaemon      = getMockLockssDaemon();
    mockAuMover     = Mockito.mock(V2AuMover.class);
    mockMigrationMgr = Mockito.mock(MigrationManager.class);
    mockDbManager   = Mockito.mock(DbManager.class);
    mockMetadataMgr = Mockito.mock(MetadataManager.class);
    mockRemoteApi   = Mockito.mock(RemoteApi.class);

    Mockito.when(mockAuMover.getMigrationMgr()).thenReturn(mockMigrationMgr);
    mockDaemon.setDbManager(mockDbManager);
    mockDaemon.setMetadataManager(mockMetadataMgr);
    mockDaemon.setRemoteApi(mockRemoteApi);

    task = MigrationTask.migrateDb(mockAuMover);
  }

  // -------------------------------------------------------------------------
  // ensureIPv4Host
  // -------------------------------------------------------------------------

  public void testEnsureIPv4Host_localhost() {
    assertEquals("127.0.0.1", DBMover.ensureIPv4Host("localhost"));
  }

  public void testEnsureIPv4Host_localhostUpperCase() {
    assertEquals("127.0.0.1", DBMover.ensureIPv4Host("LOCALHOST"));
  }

  public void testEnsureIPv4Host_localhostMixedCase() {
    assertEquals("127.0.0.1", DBMover.ensureIPv4Host("LocalHost"));
  }

  public void testEnsureIPv4Host_numericAddress() {
    assertEquals("10.0.0.1", DBMover.ensureIPv4Host("10.0.0.1"));
  }

  public void testEnsureIPv4Host_namedHost() {
    assertEquals("db.example.com", DBMover.ensureIPv4Host("db.example.com"));
  }

  public void testEnsureIPv4Host_ipv6Loopback() {
    // IPv6 addresses are not translated — caller must ensure IPv4 is configured
    assertEquals("::1", DBMover.ensureIPv4Host("::1"));
  }

  public void testEnsureIPv4Host_emptyString() {
    assertEquals("", DBMover.ensureIPv4Host(""));
  }

  public void testEnsureIPv4Host_null() {
    // "localhost".equalsIgnoreCase(null) returns false, so null passes through
    assertNull(DBMover.ensureIPv4Host(null));
  }

  // -------------------------------------------------------------------------
  // dbHasMoved — tested through run() since the method is private
  // -------------------------------------------------------------------------

  public void testRun_skipsWhenDbAlreadyMoved() throws Exception {
    ConfigurationUtil.addFromArgs(MigrationManager.PARAM_IS_DB_MOVED, "true");
    DBMover mover = new DBMover(mockAuMover, task);

    mover.run();

    // If the early-exit fires, nothing DB-related should be called.
    verify(mockDbManager, never()).isTypeDerby();
    verify(mockDbManager, never()).isTypePostgresql();
  }

  // -------------------------------------------------------------------------
  // initParams — package-private, callable directly
  // -------------------------------------------------------------------------

  /** Key prefix for V2 datasource params as read by initParams(). */
  private String v2Key(String paramSuffix) {
    // config.getConfigTree("v2") strips the "v2." prefix, so the full key
    // in the global config is "v2." + the param.
    return "v2." + paramSuffix;
  }

  private void setValidV2DbConfig() {
    ConfigurationUtil.addFromArgs(
        v2Key(DbManager.PARAM_DATASOURCE_SERVERNAME), "v2host.example.com",
        v2Key(DbManager.PARAM_DATASOURCE_USER),       "v2user",
        v2Key(DbManager.PARAM_DATASOURCE_PASSWORD),   "v2pass",
        v2Key(DbManager.PARAM_DATASOURCE_PORTNUMBER), "5432");
    ConfigurationUtil.addFromArgs(
        v2Key(DbManager.PARAM_DATASOURCE_DATABASENAME), "lockssdb");
  }

  public void testInitParams_missingV2DbHost() throws Exception {
    ConfigurationUtil.addFromArgs(
        v2Key(DbManager.PARAM_DATASOURCE_USER),       "v2user",
        v2Key(DbManager.PARAM_DATASOURCE_PASSWORD),   "v2pass",
        v2Key(DbManager.PARAM_DATASOURCE_PORTNUMBER), "5432"
    );
    DBMover mover = new DBMover(mockAuMover, task);
    try {
      mover.initParams();
      fail("Expected MigrationTaskFailedException for missing hostname");
    } catch (MigrationTaskFailedException e) {
      assertTrue("Error should mention hostname: " + e.getMessage(),
                 e.getMessage().contains("hostname"));
    }
  }

  public void testInitParams_missingCredentials() throws Exception {
    ConfigurationUtil.addFromArgs(
        v2Key(DbManager.PARAM_DATASOURCE_SERVERNAME), "v2host.example.com",
        v2Key(DbManager.PARAM_DATASOURCE_PORTNUMBER), "5432"
        // no user or password
    );
    DBMover mover = new DBMover(mockAuMover, task);
    try {
      mover.initParams();
      fail("Expected MigrationTaskFailedException for missing credentials");
    } catch (MigrationTaskFailedException e) {
      assertTrue("Error should mention user name or password: " + e.getMessage(),
                 e.getMessage().contains("user name or password"));
    }
  }

  public void testInitParams_missingV2DbUserOnly() throws Exception {
    ConfigurationUtil.addFromArgs(
        v2Key(DbManager.PARAM_DATASOURCE_SERVERNAME),  "v2host.example.com",
        v2Key(DbManager.PARAM_DATASOURCE_PASSWORD),    "v2pass",
        v2Key(DbManager.PARAM_DATASOURCE_PORTNUMBER),  "5432",
        v2Key(DbManager.PARAM_DATASOURCE_DATABASENAME), "lockssdb"
    );
    DBMover mover = new DBMover(mockAuMover, task);
    try {
      mover.initParams();
      fail("Expected MigrationTaskFailedException for missing user");
    } catch (MigrationTaskFailedException e) {
      assertTrue("Error should mention user name or password: " + e.getMessage(),
                 e.getMessage().contains("user name or password"));
    }
  }

  public void testInitParams_missingV2DbPasswordOnly() throws Exception {
    ConfigurationUtil.addFromArgs(
        v2Key(DbManager.PARAM_DATASOURCE_SERVERNAME),  "v2host.example.com",
        v2Key(DbManager.PARAM_DATASOURCE_USER),        "v2user",
        v2Key(DbManager.PARAM_DATASOURCE_PORTNUMBER),  "5432",
        v2Key(DbManager.PARAM_DATASOURCE_DATABASENAME), "lockssdb"
    );
    DBMover mover = new DBMover(mockAuMover, task);
    try {
      mover.initParams();
      fail("Expected MigrationTaskFailedException for missing password");
    } catch (MigrationTaskFailedException e) {
      assertTrue("Error should mention user name or password: " + e.getMessage(),
                 e.getMessage().contains("user name or password"));
    }
  }

  public void testInitParams_missingPort() throws Exception {
    ConfigurationUtil.addFromArgs(
        v2Key(DbManager.PARAM_DATASOURCE_SERVERNAME), "v2host.example.com",
        v2Key(DbManager.PARAM_DATASOURCE_USER),       "v2user",
        v2Key(DbManager.PARAM_DATASOURCE_PASSWORD),   "v2pass"
        // no port
    );
    DBMover mover = new DBMover(mockAuMover, task);
    try {
      mover.initParams();
      fail("Expected MigrationTaskFailedException for missing port");
    } catch (MigrationTaskFailedException e) {
      assertTrue("Error should mention port: " + e.getMessage(),
                 e.getMessage().contains("port"));
    }
  }

  public void testInitParams_invalidPort() throws Exception {
    ConfigurationUtil.addFromArgs(
        v2Key(DbManager.PARAM_DATASOURCE_SERVERNAME), "v2host.example.com",
        v2Key(DbManager.PARAM_DATASOURCE_USER),       "v2user",
        v2Key(DbManager.PARAM_DATASOURCE_PASSWORD),   "v2pass",
        v2Key(DbManager.PARAM_DATASOURCE_PORTNUMBER), "not-a-number"
    );
    DBMover mover = new DBMover(mockAuMover, task);
    try {
      mover.initParams();
      fail("Expected MigrationTaskFailedException for non-integer port");
    } catch (MigrationTaskFailedException e) {
      assertTrue("Error should mention invalid integer: " + e.getMessage(),
                 e.getMessage().contains("not a valid integer"));
    }
  }

  public void testInitParams_missingV2DbName() throws Exception {
    ConfigurationUtil.addFromArgs(
        v2Key(DbManager.PARAM_DATASOURCE_SERVERNAME), "v2host.example.com",
        v2Key(DbManager.PARAM_DATASOURCE_USER),       "v2user",
        v2Key(DbManager.PARAM_DATASOURCE_PASSWORD),   "v2pass",
        v2Key(DbManager.PARAM_DATASOURCE_PORTNUMBER), "5432"
        // no database name
    );
    DBMover mover = new DBMover(mockAuMover, task);
    try {
      mover.initParams();
      fail("Expected MigrationTaskFailedException for missing database name");
    } catch (MigrationTaskFailedException e) {
      assertTrue("Error should mention database name: " + e.getMessage(),
                 e.getMessage().contains("database name"));
    }
  }

  public void testInitParams_setsV2DbFieldsOnSuccess() throws Exception {
    setValidV2DbConfig();
    DBMover mover = new DBMover(mockAuMover, task);
    mover.initParams();
    assertEquals("v2host.example.com", mover.v2dbhost);
    assertEquals("v2user",             mover.v2dbuser);
    assertEquals("v2pass",             mover.v2dbpassword);
    assertEquals("5432",               mover.v2dbport);
    assertEquals("lockssdb",           mover.v2dbname);
  }

  // -------------------------------------------------------------------------
  // Multi-table PostgreSQL DB migration
  // -------------------------------------------------------------------------

  /**
   * SQL that simulates a pg_dump of a multi-table LOCKSS metadata database.
   * Includes four distinct tables: publication, subscription,
   * counter_book_type_aggregates, and au_md.
   */
  static final String MULTI_TABLE_SQL =
      "-- pg_dump data-only output\n"
      + "INSERT INTO publication (id, name, pissn) VALUES (1, 'Journal A', '0001-0001');\n"
      + "INSERT INTO publication (id, name, pissn) VALUES (2, 'Conference B', '0002-0002');\n"
      + "INSERT INTO subscription (id, publication_seq, publisher_subscription)"
      + " VALUES (1, 1, TRUE);\n"
      + "INSERT INTO subscription (id, publication_seq, publisher_subscription)"
      + " VALUES (2, 2, FALSE);\n"
      + "INSERT INTO counter_book_type_aggregates"
      + " (publisher_name, is_publisher_involved, publication_year)"
      + " VALUES ('Pub A', TRUE, 2020);\n"
      + "INSERT INTO counter_book_type_aggregates"
      + " (publisher_name, is_publisher_involved, publication_year)"
      + " VALUES ('Pub B', FALSE, 2021);\n"
      + "INSERT INTO au_md (md_extraction_time, au_key)"
      + " VALUES (1617000000, 'auid1');\n"
      + "INSERT INTO au_md (md_extraction_time, au_key)"
      + " VALUES (1617003600, 'auid2');\n";

  /**
   * A testable subclass of DBMover that:
   * <ul>
   *   <li>replaces pg_dump with a shell {@code printf} that emits
   *       configurable SQL (simulating a multi-table dump)</li>
   *   <li>replaces psql with a shell {@code cat} that writes stdin to a
   *       temp file (so tests can verify the data that would have been
   *       imported)</li>
   *   <li>overrides {@code getDatabaseSize} to avoid real PG connections</li>
   *   <li>makes the size-updater thread a no-op</li>
   * </ul>
   */
  class TestableDBMover extends DBMover {
    private final String dumpSql;
    private File capturedRestoreInput;

    TestableDBMover(V2AuMover auMover, MigrationTask task, String dumpSql) {
      super(auMover, task);
      this.dumpSql = dumpSql;
    }

    @Override
    List<String> buildDumpCommand() {
      // Use printf so the SQL is not subject to shell word-splitting or
      // backslash interpretation that 'echo' would apply.
      return Arrays.asList("sh", "-c",
          "printf '%s' " + shellQuote(dumpSql));
    }

    @Override
    List<String> buildRestoreCommand() {
      try {
        capturedRestoreInput = File.createTempFile("dbmover-test", ".sql");
        capturedRestoreInput.deleteOnExit();
      } catch (IOException e) {
        throw new RuntimeException("Could not create temp file for psql capture", e);
      }
      return Arrays.asList("sh", "-c",
          "cat > " + capturedRestoreInput.getAbsolutePath());
    }

    @Override
    void startUpdater() { /* no-op: avoid background PG connection in tests */ }

    @Override
    void stopUpdater()  { /* no-op */ }

    File getCapturedRestoreInput() {
      return capturedRestoreInput;
    }

    private String shellQuote(String s) {
      return "'" + s.replace("'", "'\\''") + "'";
    }
  }

  /** Sets all v1/v2 fields directly on a mover, bypassing initParams(). */
  private void setMoverFields(DBMover mover) {
    Mockito.when(mockAuMover.getDbCopyTimeout()).thenReturn(60_000L);
    mover.v1host     = "127.0.0.1";
    mover.v1port     = "5432";
    mover.v1user     = "lockss";
    mover.v1password = "testpass";
    mover.v1dbname   = "lockss";

    mover.v2dbhost     = "v2host.example.com";
    mover.v2dbport     = "5432";
    mover.v2dbuser     = "v2user";
    mover.v2dbpassword = "v2pass";
    mover.v2dbname     = "lockssdb";

    mover.srcSize = 1024L * 1024L;
    mover.dstSize = 0L;
  }

  /**
   * Happy path: the piped dump output for a multi-table database is received
   * in full by the simulated restore process, and the migration is marked done.
   */
  public void testCopyPostgresDb_multipleTablesSuccess() throws Exception {
    TestableDBMover mover =
        new TestableDBMover(mockAuMover, task, MULTI_TABLE_SQL);
    setMoverFields(mover);

    mover.copyPostgresDb();

    verify(mockMigrationMgr).setIsDbMoved(true);
    verify(mockAuMover).addFinishedOther(anyString());
    verify(mockAuMover).logReport(anyString());

    File captured = mover.getCapturedRestoreInput();
    assertNotNull("Restore process should have written to a file", captured);
    String received = new String(Files.readAllBytes(captured.toPath()));

    assertTrue("Received SQL must contain publication table inserts",
               received.contains("publication"));
    assertTrue("Received SQL must contain subscription table inserts",
               received.contains("subscription"));
    assertTrue("Received SQL must contain counter aggregate table inserts",
               received.contains("counter_book_type_aggregates"));
    assertTrue("Received SQL must contain au_md table inserts",
               received.contains("au_md"));

    // Spot-check specific rows from each table
    assertTrue(received.contains("Journal A"));
    assertTrue(received.contains("Conference B"));
    assertTrue(received.contains("auid1"));
    assertTrue(received.contains("auid2"));
    assertTrue(received.contains("Pub A"));
    assertTrue(received.contains("Pub B"));
  }

  /**
   * If pg_dump exits with a non-zero code the migration must fail and
   * setIsDbMoved must never be called.
   */
  public void testCopyPostgresDb_dumpFailure() throws Exception {
    TestableDBMover mover = new TestableDBMover(mockAuMover, task, "") {
      @Override
      List<String> buildDumpCommand() {
        return Arrays.asList("sh", "-c", "exit 1");
      }
      @Override
      List<String> buildRestoreCommand() {
        return Arrays.asList("sh", "-c", "cat > /dev/null");
      }
    };
    setMoverFields(mover);

    try {
      mover.copyPostgresDb();
      fail("Expected MigrationTaskFailedException when pg_dump fails");
    } catch (MigrationTaskFailedException e) {
      assertTrue("Exception should report pg_dump exit code: " + e.getMessage(),
                 e.getMessage().contains("pg_dump exit=1"));
    }

    verify(mockMigrationMgr, never()).setIsDbMoved(true);
  }

  /**
   * If psql exits with a non-zero code the migration must fail.
   */
  public void testCopyPostgresDb_restoreFailure() throws Exception {
    TestableDBMover mover =
        new TestableDBMover(mockAuMover, task, "INSERT INTO t1 VALUES (1);") {
      @Override
      List<String> buildRestoreCommand() {
        return Arrays.asList("sh", "-c", "cat > /dev/null; exit 1");
      }
    };
    setMoverFields(mover);

    try {
      mover.copyPostgresDb();
      fail("Expected MigrationTaskFailedException when psql fails");
    } catch (MigrationTaskFailedException e) {
      assertTrue("Exception should report psql exit code: " + e.getMessage(),
                 e.getMessage().contains("psql exit=1"));
    }

    verify(mockMigrationMgr, never()).setIsDbMoved(true);
  }

  /**
   * Verifies that the pg_dump command uses the correct flags including
   * {@code --exclude-table=version} and {@code --disable-triggers}.
   */
  public void testBuildDumpCommand_includesRequiredFlags() throws Exception {
    setValidV2DbConfig();
    DBMover mover = new DBMover(mockAuMover, task);
    mover.initParams();
    mover.v1host = "127.0.0.1"; mover.v1port = "5432";
    mover.v1user = "lockss"; mover.v1dbname = "lockss";

    List<String> cmd = mover.buildDumpCommand();

    assertTrue("Dump command must include pg_dump", cmd.contains("pg_dump"));
    assertTrue("Dump command must include -a (data only)",
               cmd.contains("-a"));
    assertTrue("Dump command must exclude version table",
               cmd.contains("--exclude-table=version"));
    assertTrue("Dump command must disable triggers",
               cmd.contains("--disable-triggers"));
  }

  /**
   * Verifies that the restore command targets the correct v2 host and database.
   */
  public void testBuildRestoreCommand_targetsV2Destination() throws Exception {
    setValidV2DbConfig();
    DBMover mover = new DBMover(mockAuMover, task);
    mover.initParams();

    List<String> cmd = mover.buildRestoreCommand();

    assertTrue("Restore command must include psql", cmd.contains("psql"));
    assertTrue("Restore command must target v2 host",
               cmd.contains("v2host.example.com"));
    assertTrue("Restore command must target v2 port",
               cmd.contains("5432"));
    assertTrue("Restore command must target v2 database",
               cmd.contains("lockssdb"));
  }

  public void testBuildDumpCommand_localhostConvertedToIPv4() throws Exception {
    setValidV2DbConfig();
    DBMover mover = new DBMover(mockAuMover, task);
    mover.initParams();
    mover.v1host = "localhost";

    List<String> cmd = mover.buildDumpCommand();

    int hIdx = cmd.indexOf("-h");
    assertTrue("Dump command must include -h flag", hIdx >= 0);
    assertEquals("v1 localhost must become 127.0.0.1 in dump command",
                 "127.0.0.1", cmd.get(hIdx + 1));
  }

  public void testBuildRestoreCommand_localhostConvertedToIPv4() throws Exception {
    ConfigurationUtil.addFromArgs(
        v2Key(DbManager.PARAM_DATASOURCE_SERVERNAME),  "localhost",
        v2Key(DbManager.PARAM_DATASOURCE_USER),        "v2user",
        v2Key(DbManager.PARAM_DATASOURCE_PASSWORD),    "v2pass",
      v2Key(DbManager.PARAM_DATASOURCE_PORTNUMBER), "5432");
    ConfigurationUtil.addFromArgs(
      v2Key(DbManager.PARAM_DATASOURCE_DATABASENAME), "lockssdb");
    DBMover mover = new DBMover(mockAuMover, task);
    mover.initParams();

    List<String> cmd = mover.buildRestoreCommand();

    int hIdx = cmd.indexOf("-h");
    assertTrue("Restore command must include -h flag", hIdx >= 0);
    assertEquals("v2 localhost must become 127.0.0.1 in restore command",
                 "127.0.0.1", cmd.get(hIdx + 1));
  }

  // -------------------------------------------------------------------------
  // Additional copyPostgresDb scenarios
  // -------------------------------------------------------------------------

  /**
   * pg_dump produces no output; psql receives empty input and exits 0.
   * The migration should still succeed and be marked complete.
   */
  public void testCopyPostgresDb_emptyDump() throws Exception {
    TestableDBMover mover = new TestableDBMover(mockAuMover, task, "") {
      @Override
      List<String> buildDumpCommand() {
        return Arrays.asList("sh", "-c", "true");
      }
    };
    setMoverFields(mover);

    mover.copyPostgresDb();

    verify(mockMigrationMgr).setIsDbMoved(true);
    String received = new String(Files.readAllBytes(
        mover.getCapturedRestoreInput().toPath()));
    assertEquals("Empty dump should produce empty restore input", "", received);
  }

  /**
   * Both pg_dump and psql exit non-zero; the error message must include both
   * exit codes.
   */
  public void testCopyPostgresDb_bothFail() throws Exception {
    TestableDBMover mover = new TestableDBMover(mockAuMover, task, "") {
      @Override
      List<String> buildDumpCommand() {
        return Arrays.asList("sh", "-c", "exit 2");
      }
      @Override
      List<String> buildRestoreCommand() {
        return Arrays.asList("sh", "-c", "cat > /dev/null; exit 3");
      }
    };
    setMoverFields(mover);

    try {
      mover.copyPostgresDb();
      fail("Expected MigrationTaskFailedException when both processes fail");
    } catch (MigrationTaskFailedException e) {
      assertTrue("Exception must report pg_dump exit code: " + e.getMessage(),
                 e.getMessage().contains("pg_dump exit=2"));
      assertTrue("Exception must report psql exit code: " + e.getMessage(),
                 e.getMessage().contains("psql exit=3"));
    }

    verify(mockMigrationMgr, never()).setIsDbMoved(true);
  }

  /**
   * pg_dump exceeds the timeout; the migration must fail with a clear message
   * and processes must be cleaned up.
   */
  public void testCopyPostgresDb_dumpTimeout() throws Exception {
    TestableDBMover mover = new TestableDBMover(mockAuMover, task, "") {
      @Override
      List<String> buildDumpCommand() {
        return Arrays.asList("sh", "-c", "sleep 60");
      }
      @Override
      List<String> buildRestoreCommand() {
        return Arrays.asList("sh", "-c", "cat > /dev/null");
      }
    };
    setMoverFields(mover);
    Mockito.when(mockAuMover.getDbCopyTimeout()).thenReturn(1_000L);

    try {
      mover.copyPostgresDb();
      fail("Expected MigrationTaskFailedException on pg_dump timeout");
    } catch (MigrationTaskFailedException e) {
      assertTrue("Exception must mention pg_dump timed out: " + e.getMessage(),
                 e.getMessage().contains("pg_dump timed out"));
    }

    verify(mockMigrationMgr, never()).setIsDbMoved(true);
  }

  /**
   * psql exceeds the timeout after pg_dump completes successfully.
   */
  public void testCopyPostgresDb_restoreTimeout() throws Exception {
    TestableDBMover mover = new TestableDBMover(mockAuMover, task, "") {
      @Override
      List<String> buildDumpCommand() {
        return Arrays.asList("sh", "-c", "true");
      }
      @Override
      List<String> buildRestoreCommand() {
        return Arrays.asList("sh", "-c", "sleep 60");
      }
    };
    setMoverFields(mover);
    Mockito.when(mockAuMover.getDbCopyTimeout()).thenReturn(1_000L);

    try {
      mover.copyPostgresDb();
      fail("Expected MigrationTaskFailedException on psql timeout");
    } catch (MigrationTaskFailedException e) {
      assertTrue("Exception must mention psql timed out: " + e.getMessage(),
                 e.getMessage().contains("psql timed out"));
    }

    verify(mockMigrationMgr, never()).setIsDbMoved(true);
  }

  // -------------------------------------------------------------------------
  // MD indexing enable/disable via run()
  // -------------------------------------------------------------------------

  /**
   * disableMdIndexing() must be called before the copy and enableMdIndexing()
   * must be called afterwards (even when the copy fails), and must re-enable
   * indexing when the config says it should be on.
   */
  public void testRun_disablesAndReenablesMdIndexing() throws Exception {
    ConfigurationUtil.addFromArgs(MetadataManager.PARAM_INDEXING_ENABLED, "true");
    DBMover mover = new DBMover(mockAuMover, task);
    try {
      mover.run(); // fails in initParams() — missing v2 config
    } catch (MigrationTaskFailedException expected) { }

    InOrder order = inOrder(mockMetadataMgr);
    order.verify(mockMetadataMgr).setIndexingEnabled(false);
    order.verify(mockMetadataMgr).setIndexingEnabled(true);
  }

  /**
   * enableMdIndexing() must not re-enable indexing when the config says it
   * should remain off.
   */
  public void testRun_doesNotReenableMdIndexingWhenConfigSaysOff() throws Exception {
    ConfigurationUtil.addFromArgs(MetadataManager.PARAM_INDEXING_ENABLED, "false");
    DBMover mover = new DBMover(mockAuMover, task);
    try {
      mover.run();
    } catch (MigrationTaskFailedException expected) { }

    verify(mockMetadataMgr).setIndexingEnabled(false);
    verify(mockMetadataMgr, never()).setIndexingEnabled(true);
  }

  // -------------------------------------------------------------------------
  // waitForV2DbReady
  // -------------------------------------------------------------------------

  /**
   * Returns a DBMover subclass that uses the supplied mock Connection instead
   * of a real PG connection, and overrides sleepUntilDeadline() so tests run
   * without actual Thread.sleep delays while still enforcing the deadline.
   */
  private DBMover newWaitMover(Connection conn) {
    DBMover mover = new DBMover(mockAuMover, task) {
      @Override
      Connection openV2Connection() throws SQLException {
        return conn;
      }
      @Override
      void sleepUntilDeadline(long deadlineMs, String msg)
          throws MigrationTaskFailedException {
        if (TimeBase.nowMs() >= deadlineMs) {
          throw new MigrationTaskFailedException(msg);
        }
      }
    };
    mover.v2dbhost     = "v2host.example.com";
    mover.v2dbport     = "5432";
    mover.v2dbuser     = "v2user";
    mover.v2dbpassword = "v2pass";
    mover.v2dbname     = "lockssdb";
    return mover;
  }

  /**
   * Returns a mock Connection where version table, subsystem column, and DB
   * version are all already present/correct on the first check.
   */
  private Connection readyV2Connection(int version) throws SQLException {
    Connection conn     = mock(Connection.class);
    DatabaseMetaData meta = mock(DatabaseMetaData.class);
    when(conn.getMetaData()).thenReturn(meta);

    ResultSet tableRs = mock(ResultSet.class);
    when(meta.getTables(any(), any(), any(), any())).thenReturn(tableRs);
    when(tableRs.next()).thenReturn(true);

    ResultSet colRs = mock(ResultSet.class);
    when(meta.getColumns(any(), any(), any(), any())).thenReturn(colRs);
    when(colRs.next()).thenReturn(true);

    PreparedStatement stmt = mock(PreparedStatement.class);
    ResultSet versionRs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(stmt);
    when(stmt.executeQuery()).thenReturn(versionRs);
    when(versionRs.next()).thenReturn(true, false);
    when(versionRs.getInt("version")).thenReturn(version);

    return conn;
  }

  /** All three conditions met on the first check — must return without throwing. */
  public void testWaitForV2DbReady_alreadyReady() throws Exception {
    Connection conn = readyV2Connection(DbManager.DEFAULT_TARGET_DB_VERSION);
    newWaitMover(conn).waitForV2DbReady(TimeBase.nowMs() + 60_000L);
  }

  /** Version table absent on first poll, present on second — must succeed and poll twice. */
  public void testWaitForV2DbReady_pollsUntilTableAppears() throws Exception {
    Connection conn = mock(Connection.class);
    DatabaseMetaData meta = mock(DatabaseMetaData.class);
    when(conn.getMetaData()).thenReturn(meta);

    ResultSet tableRs1 = mock(ResultSet.class);
    ResultSet tableRs2 = mock(ResultSet.class);
    when(meta.getTables(any(), any(), any(), any()))
        .thenReturn(tableRs1).thenReturn(tableRs2);
    when(tableRs1.next()).thenReturn(false);
    when(tableRs2.next()).thenReturn(true);

    ResultSet colRs = mock(ResultSet.class);
    when(meta.getColumns(any(), any(), any(), any())).thenReturn(colRs);
    when(colRs.next()).thenReturn(true);

    PreparedStatement stmt = mock(PreparedStatement.class);
    ResultSet versionRs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(stmt);
    when(stmt.executeQuery()).thenReturn(versionRs);
    when(versionRs.next()).thenReturn(true, false);
    when(versionRs.getInt("version")).thenReturn(DbManager.DEFAULT_TARGET_DB_VERSION);

    newWaitMover(conn).waitForV2DbReady(TimeBase.nowMs() + 60_000L);

    verify(meta, times(2)).getTables(any(), any(), any(), any());
  }

  /** Subsystem column absent on first poll, present on second — must succeed and poll twice. */
  public void testWaitForV2DbReady_pollsUntilColumnAppears() throws Exception {
    Connection conn = mock(Connection.class);
    DatabaseMetaData meta = mock(DatabaseMetaData.class);
    when(conn.getMetaData()).thenReturn(meta);

    ResultSet tableRs = mock(ResultSet.class);
    when(meta.getTables(any(), any(), any(), any())).thenReturn(tableRs);
    when(tableRs.next()).thenReturn(true);

    ResultSet colRs1 = mock(ResultSet.class);
    ResultSet colRs2 = mock(ResultSet.class);
    when(meta.getColumns(any(), any(), any(), any()))
        .thenReturn(colRs1).thenReturn(colRs2);
    when(colRs1.next()).thenReturn(false);
    when(colRs2.next()).thenReturn(true);

    PreparedStatement stmt = mock(PreparedStatement.class);
    ResultSet versionRs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(stmt);
    when(stmt.executeQuery()).thenReturn(versionRs);
    when(versionRs.next()).thenReturn(true, false);
    when(versionRs.getInt("version")).thenReturn(DbManager.DEFAULT_TARGET_DB_VERSION);

    newWaitMover(conn).waitForV2DbReady(TimeBase.nowMs() + 60_000L);

    verify(meta, times(2)).getColumns(any(), any(), any(), any());
  }

  /** DB version below target on first poll, at target on second — must succeed and query twice. */
  public void testWaitForV2DbReady_pollsUntilVersionReady() throws Exception {
    Connection conn = mock(Connection.class);
    DatabaseMetaData meta = mock(DatabaseMetaData.class);
    when(conn.getMetaData()).thenReturn(meta);

    ResultSet tableRs = mock(ResultSet.class);
    when(meta.getTables(any(), any(), any(), any())).thenReturn(tableRs);
    when(tableRs.next()).thenReturn(true);

    ResultSet colRs = mock(ResultSet.class);
    when(meta.getColumns(any(), any(), any(), any())).thenReturn(colRs);
    when(colRs.next()).thenReturn(true);

    // First version query returns 0 rows (version 0); second returns target version.
    PreparedStatement stmt = mock(PreparedStatement.class);
    ResultSet versionRs1 = mock(ResultSet.class);
    ResultSet versionRs2 = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(stmt);
    when(stmt.executeQuery()).thenReturn(versionRs1).thenReturn(versionRs2);
    when(versionRs1.next()).thenReturn(false);
    when(versionRs2.next()).thenReturn(true, false);
    when(versionRs2.getInt("version")).thenReturn(DbManager.DEFAULT_TARGET_DB_VERSION);

    newWaitMover(conn).waitForV2DbReady(TimeBase.nowMs() + 60_000L);

    verify(stmt, times(2)).executeQuery();
  }

  /** Version table never appears before the deadline — must throw with an informative message. */
  public void testWaitForV2DbReady_timeoutWaitingForVersionTable() throws Exception {
    Connection conn = mock(Connection.class);
    DatabaseMetaData meta = mock(DatabaseMetaData.class);
    ResultSet tableRs = mock(ResultSet.class);
    when(conn.getMetaData()).thenReturn(meta);
    when(meta.getTables(any(), any(), any(), any())).thenReturn(tableRs);
    when(tableRs.next()).thenReturn(false);

    try {
      newWaitMover(conn).waitForV2DbReady(0L);
      fail("Expected MigrationTaskFailedException on timeout");
    } catch (MigrationTaskFailedException e) {
      assertTrue("Error should mention version table: " + e.getMessage(),
                 e.getMessage().contains("version table"));
    }
  }

  /** Subsystem column never appears before the deadline — must throw with an informative message. */
  public void testWaitForV2DbReady_timeoutWaitingForSubsystemColumn() throws Exception {
    Connection conn = mock(Connection.class);
    DatabaseMetaData meta = mock(DatabaseMetaData.class);
    when(conn.getMetaData()).thenReturn(meta);

    ResultSet tableRs = mock(ResultSet.class);
    when(meta.getTables(any(), any(), any(), any())).thenReturn(tableRs);
    when(tableRs.next()).thenReturn(true);

    ResultSet colRs = mock(ResultSet.class);
    when(meta.getColumns(any(), any(), any(), any())).thenReturn(colRs);
    when(colRs.next()).thenReturn(false);

    try {
      newWaitMover(conn).waitForV2DbReady(0L);
      fail("Expected MigrationTaskFailedException on timeout");
    } catch (MigrationTaskFailedException e) {
      assertTrue("Error should mention subsystem column: " + e.getMessage(),
                 e.getMessage().contains("subsystem column"));
    }
  }

  /** DB version never reaches target before the deadline — must throw with an informative message. */
  public void testWaitForV2DbReady_timeoutWaitingForVersionUpdate() throws Exception {
    Connection conn = mock(Connection.class);
    DatabaseMetaData meta = mock(DatabaseMetaData.class);
    when(conn.getMetaData()).thenReturn(meta);

    ResultSet tableRs = mock(ResultSet.class);
    when(meta.getTables(any(), any(), any(), any())).thenReturn(tableRs);
    when(tableRs.next()).thenReturn(true);

    ResultSet colRs = mock(ResultSet.class);
    when(meta.getColumns(any(), any(), any(), any())).thenReturn(colRs);
    when(colRs.next()).thenReturn(true);

    PreparedStatement stmt = mock(PreparedStatement.class);
    ResultSet versionRs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(stmt);
    when(stmt.executeQuery()).thenReturn(versionRs);
    when(versionRs.next()).thenReturn(false);  // always version 0

    try {
      newWaitMover(conn).waitForV2DbReady(0L);
      fail("Expected MigrationTaskFailedException on timeout");
    } catch (MigrationTaskFailedException e) {
      assertTrue("Error should mention 'reach version': " + e.getMessage(),
                 e.getMessage().contains("reach version"));
    }
  }

  /** openV2Connection() failure is wrapped in a MigrationTaskFailedException. */
  public void testWaitForV2DbReady_sqlExceptionOnConnect() throws Exception {
    DBMover mover = new DBMover(mockAuMover, task) {
      @Override
      Connection openV2Connection() throws SQLException {
        throw new SQLException("Connection refused");
      }
    };
    mover.v2dbhost     = "v2host.example.com";
    mover.v2dbport     = "5432";
    mover.v2dbuser     = "v2user";
    mover.v2dbpassword = "v2pass";
    mover.v2dbname     = "lockssdb";

    try {
      mover.waitForV2DbReady(TimeBase.nowMs() + 60_000L);
      fail("Expected MigrationTaskFailedException on SQL error");
    } catch (MigrationTaskFailedException e) {
      assertTrue("Error should mention V2 database: " + e.getMessage(),
                 e.getMessage().contains("V2 database"));
    }
  }

  /** PARAM_TARGET_DB_VERSION config overrides the default target version. */
  public void testWaitForV2DbReady_respectsConfiguredTargetVersion() throws Exception {
    ConfigurationUtil.addFromArgs(DbManager.PARAM_TARGET_DB_VERSION, "5");
    // Mock returns version 5, which satisfies the configured target of 5
    // but would loop forever against the default target of 28.
    Connection conn = readyV2Connection(5);
    newWaitMover(conn).waitForV2DbReady(TimeBase.nowMs() + 60_000L);
  }
}
