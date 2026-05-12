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

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.postgresql.ds.PGSimpleDataSource;
import org.apache.commons.httpclient.methods.multipart.*;
import org.apache.commons.httpclient.params.*;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.LockssThread;
import org.lockss.db.DbManager;
import org.lockss.metadata.MetadataManager;
import org.lockss.remote.*;
import org.lockss.servlet.MigrateContent;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

import static org.lockss.laaws.MigrationConstants.*;

public class DBMover extends Worker {
  private static final long DBSIZE_CHECK_INTERVAL = 2*Constants.SECOND;
  private static final int IOBUF_SIZE = 256 * 1024;
  // Number of stderr/stdout lines retained per process for error reporting.
  // Older lines are silently discarded once the buffer is full.
  private static final int ERR_LINE_BUF_SIZE = 100;
  private final Logger log = Logger.getLogger(DBMover.class);

  // v1 connection parameters
  String v1user = DbManager.DEFAULT_DATASOURCE_USER;
  String v1password = DbManager.DEFAULT_DATASOURCE_PASSWORD;
  String v1host = DbManager.DEFAULT_DATASOURCE_SERVERNAME;
  String v1port = "5432";
  String v1dbname = v1user;

  // v2 connection parameters
  String v2host;
  String v2user;
  String v2pass;

  // v2 database connection parameters
  String v2dbuser;
  String v2dbpassword;
  String v2dbhost;
  String v2dbport;
  String v2dbname;

  long srcSize;
  long dstSize;

  LockssDaemon daemon;
  DbManager dbManager;
  MigrationManager migrationMgr;
  RemoteApi rapi;
  DbSizeUpdater sizeUpdater;

  public DBMover(V2AuMover auMover, MigrationTask task) {
    super(auMover, task);
    daemon = LockssDaemon.getLockssDaemon();
    dbManager = daemon.getDbManager();
    migrationMgr = auMover.getMigrationMgr();
    rapi = daemon.getRemoteApi();
  }

  public void run() throws MigrationTaskFailedException {
    try {
      if (dbHasMoved()) {
        log.info("Db has already been moved.");
        return;
      }
      disableMdIndexing();
      initParams();
      if (dbManager.isTypeDerby()) {
        log.info("Migrating Derby DB Content");
        copyDerbyDb();
      }
      else if (dbManager.isTypePostgresql()) {
        log.info("Migrating Postgresql Content..");
        srcSize = getDatabaseSize(ensureIPv4Host(v1host),v1port,v1user,v1password,v1dbname);
        dstSize = getDatabaseSize(v2dbhost, v2dbport, v2dbuser, v2dbpassword,v2dbname);
        log.info("v1 db size = " + srcSize + ", v2 db size = " + dstSize);
        auMover.dbBytesCopied = 0;
        auMover.dbBytesTotal = srcSize;
        copyPostgresDb();
      }
      else {
        throw new MigrationTaskFailedException("Unable to move database of unsupported type");
      }
    } catch (MigrationTaskFailedException e) {
      logError("Database copy failed: " + e.getMessage(), null);
      throw e;
    } catch (Exception e) {
      String msg = "Database copy failed: " + e.getMessage();
      logError(msg, e);
      throw new MigrationTaskFailedException(msg);
    } finally {
      enableMdIndexing();
    }
  }

  /** Disable MD indexing while copy is in progress */
  private void disableMdIndexing() {
    daemon.getMetadataManager().setIndexingEnabled(false);
  }

  /** Reenable MD indexing if so configured */
  private void enableMdIndexing() {
    Configuration config = ConfigManager.getCurrentConfig();
    if (config.getBoolean(MetadataManager.PARAM_INDEXING_ENABLED,
                          MetadataManager.DEFAULT_INDEXING_ENABLED)) {
      daemon.getMetadataManager().setIndexingEnabled(true);
    }
  }

  private void logError(String msg, Exception e) {
    log.error(msg, e);
    auMover.logReportAndError(msg);
    auMover.addError(msg);
    auMover.setFailed(true);
  }

  void initParams() throws MigrationTaskFailedException {
    Configuration config = ConfigManager.getCurrentConfig();

    v1user = config.get(DbManager.PARAM_DATASOURCE_USER, DbManager.DEFAULT_DATASOURCE_USER);
    v1password = config.get(DbManager.PARAM_DATASOURCE_PASSWORD, DbManager.DEFAULT_DATASOURCE_PASSWORD);
    v1host = config.get(DbManager.PARAM_DATASOURCE_SERVERNAME, DbManager.DEFAULT_DATASOURCE_SERVERNAME);
    v1port = config.get(DbManager.PARAM_DATASOURCE_PORTNUMBER, DbManager.DEFAULT_DATASOURCE_PORTNUMBER_PG);
    v1dbname = config.get(DbManager.PARAM_DATASOURCE_DATABASENAME, v1user);

    // Migration target (e.g., for services REST APIs)
    v2host = config.get(MigrateContent.PARAM_HOSTNAME);
    v2user = config.get(MigrateContent.PARAM_USERNAME);
    v2pass = config.get(MigrateContent.PARAM_PASSWORD);

    // Database connection parameters
    Configuration v2config = config.getConfigTree("v2");
    v2dbuser = v2config.get(DbManager.PARAM_DATASOURCE_USER);
    v2dbpassword = v2config.get(DbManager.PARAM_DATASOURCE_PASSWORD);
    v2dbhost = v2config.get(DbManager.PARAM_DATASOURCE_SERVERNAME);
    v2dbport = v2config.get(DbManager.PARAM_DATASOURCE_PORTNUMBER);
    v2dbname = v2config.get(DbManager.PARAM_DATASOURCE_DATABASENAME);
    // validate each parameter
    if (StringUtil.isNullString(v2dbhost)) {
      throw new MigrationTaskFailedException(
          "Database copy failed: destination hostname was not supplied.");
    }
    if (StringUtil.isNullString(v2dbuser) || StringUtil.isNullString(v2dbpassword)) {
      throw new MigrationTaskFailedException(
          "Database copy failed: Missing database user name or password.");
    }
    if (StringUtil.isNullString(v2dbport)) {
      throw new MigrationTaskFailedException(
          "Database copy failed: destination port was not supplied.");
    }
    try {
      Integer.parseInt(v2dbport);
    } catch (NumberFormatException e) {
      throw new MigrationTaskFailedException(
          "Database copy failed: destination port is not a valid integer: " + v2dbport);
    }
    if (StringUtil.isNullString(v2dbname)) {
      throw new MigrationTaskFailedException(
          "Database copy failed: destination database name was not supplied.");
    }
  }

  private boolean dbHasMoved() {
    //if v1 datasource = v2 datasource then db has moved.
    Configuration config = ConfigManager.getCurrentConfig();
    return config.getBoolean(MigrationManager.PARAM_IS_DB_MOVED,
        MigrationManager.DEFAULT_IS_DB_MOVED);
  }

  private void copyDerbyDb() throws IOException, MigrationTaskFailedException {
    LockssUrlConnectionPool connectionPool =
      auMover.getMigrationMgr().getConnectionPool();
    long startTime = TimeBase.nowMs();
    // Dump V1 subscriptions & COUNTER data
    File bakDir = FileUtil.createTempDir("dbtemp", "");
    File bakFile = new File(bakDir, "subscriptions.zip");
    try {
      rapi.createSubscriptionsAndCounterBackupFile(bakFile);

      // Restore into V2
      String restoreUrl = new URL("http", v2host, V2_DEFAULT_CFGSVC_UI_PORT,
        "/BatchAuConfig")
        .toString();
      log.info("V2 restore url = " + restoreUrl);

      // UrlUtil.handleLoginForm() doesn't handle POST, so this relies
      // on a previous GET having used handleLoginForm() to provide
      // credentials and establish a session.  That happens in
      // MigrationManager.getConfigFromMigrationTarget().  The cookie
      // is stored in the connection pool, so this *must* used the
      // same pool
      LockssUrlConnection conn =
        UrlUtil.openConnection(LockssUrlConnection.METHOD_POST, restoreUrl,
                               connectionPool);
      conn.setCredentials(v2user, v2pass);
      conn.setUserAgent("lockss");
      Part[] parts = {
        new StringPart("lockssAction", "SelectRestoreTitles"),
        new StringPart("Verb", "5"),
        new FilePart("AuConfigBackupContents", bakFile)
      };
      conn.setRequestEntity(new MultipartRequestEntity(parts, new HttpMethodParams()));

      conn.execute();
      conn = UrlUtil.handleLoginForm(conn, v2user, v2pass, connectionPool);
      int statusCode = conn.getResponseCode();
      if (statusCode == 200) {
        log.info("Success!");
        String msg = "Subscriptions copy completed in " +
          StringUtil.timeIntervalToString(TimeBase.msSince(startTime));
        auMover.addFinishedOther(msg);
        auMover.logReport(msg);
        migrationMgr.setIsDbMoved(true);
      } else {
        String msg = "Subscriptions import to V2 failed with status " + statusCode;
        Properties respProps = new Properties();
        conn.storeResponseHeaderInto(respProps, "");
        log.error(msg);
        log.error("Response headers: " + respProps);
        log.error("Response: " +
          StringUtil.fromInputStream(conn.getResponseInputStream()));
        throw new MigrationTaskFailedException(msg);
      }
    } finally {
      FileUtil.delTree(bakDir);
    }
  }

  // NOTE: pg_dump must connect to the DB (esp. the V1 DB) using an IPv4
  // address, as the DB user may not be set up for IPv6.
  static String ensureIPv4Host(String host) {
    if ("localhost".equalsIgnoreCase(host)) {
      return "127.0.0.1";
    }
    return host;
  }

  /** Returns the pg_dump command to use for copying the source database. */
  List<String> buildDumpCommand() {
    return Arrays.asList(
        "pg_dump", "-a", "--disable-triggers", "--exclude-table=version",
        "-S", v2dbuser,
        "-h", ensureIPv4Host(v1host), "-p", v1port,
        "-U", v1user, "-d", v1dbname);
  }

  /** Returns the psql command to use for restoring to the destination database. */
  List<String> buildRestoreCommand() {
    return Arrays.asList(
        "psql", "-b",
        "-h", ensureIPv4Host(v2dbhost), "-p", v2dbport,
        "-U", v2dbuser, "-d", v2dbname);
  }

  void copyPostgresDb() throws MigrationTaskFailedException {
    // Cache frequently retrieved values to avoid multiple method calls
    long dbCopyTimeout = auMover.getDbCopyTimeout();
    long startTime = TimeBase.nowMs();
    long deadlineMs = startTime + dbCopyTimeout;

    // Credentials are passed via PGPASSWORD env vars on each ProcessBuilder
    // rather than embedded in the command arguments, preventing both credential
    // exposure in logs and shell injection through parameter values.
    List<String> dumpCmd = buildDumpCommand();
    List<String> restoreCmd = buildRestoreCommand();

    log.debug("Running: " + String.join(" ", dumpCmd) + " | " + String.join(" ", restoreCmd));
    startUpdater();
    try {
      ProcessBuilder pbDump = new ProcessBuilder(dumpCmd);
      pbDump.environment().put("PGPASSWORD", v1password);

      ProcessBuilder pbRestore = new ProcessBuilder(restoreCmd);
      pbRestore.environment().put("PGPASSWORD", v2dbpassword);
      pbRestore.redirectErrorStream(true);

      Process procDump = pbDump.start();
      Process procRestore = pbRestore.start();

      // Pipe pg_dump stdout to psql stdin in a dedicated thread.
      // On pipe failure, drain pg_dump's remaining output so it can exit.
      Thread pipeThread = new LockssThread("DBMover:pipe") {
        @Override
        protected void lockssRun() {
          try (OutputStream restoreIn = procRestore.getOutputStream()) {
            byte[] buf = new byte[IOBUF_SIZE];
            int n;
            while ((n = procDump.getInputStream().read(buf)) != -1) {
              restoreIn.write(buf, 0, n);
            }
          } catch (IOException e) {
            log.warning("Error piping pg_dump output to psql", e);
            try {
              byte[] buf = new byte[IOBUF_SIZE];
              while (procDump.getInputStream().read(buf) != -1) { }
            } catch (IOException ignored) { }
          }
        }
      };
      pipeThread.start();

      // Capture pg_dump stderr separately. A fixed-size ring buffer is used
      // so that a verbose dump of a large database cannot exhaust heap.
      CircularFifoQueue<String> dumpErrLines = new CircularFifoQueue<>(ERR_LINE_BUF_SIZE);
      Thread dumpErrReader = new LockssThread("DBMover:dump-err") {
        @Override
        protected void lockssRun() {
          try (BufferedReader br = new BufferedReader(
                   new InputStreamReader(procDump.getErrorStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
              dumpErrLines.add(line);
              log.debug("pg_dump: " + line);
            }
          } catch (IOException ignored) { }
        }
      };
      dumpErrReader.start();

      // Capture psql output (stdout+stderr merged via redirectErrorStream)
      CircularFifoQueue<String> restoreOutLines = new CircularFifoQueue<>(ERR_LINE_BUF_SIZE);
      Thread restoreOutReader = new LockssThread("DBMover:restore-out") {
        @Override
        protected void lockssRun() {
          try (BufferedReader br = new BufferedReader(
                   new InputStreamReader(procRestore.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
              restoreOutLines.add(line);
              log.debug("psql: " + line);
            }
          } catch (IOException ignored) { }
        }
      };
      restoreOutReader.start();

      // Use a single absolute deadline so both processes share one timeout
      // budget rather than each getting a fresh DB_COPY_TIMEOUT_MS window.

      long dumpWaitMs = deadlineMs - TimeBase.nowMs();
      if (dumpWaitMs <= 0 || !procDump.waitFor(dumpWaitMs, TimeUnit.MILLISECONDS)) {
        procDump.destroyForcibly();
        procRestore.destroyForcibly();
        interruptAndJoin(pipeThread, dumpErrReader, restoreOutReader);
        throw new MigrationTaskFailedException("pg_dump timed out after " +
            StringUtil.timeIntervalToString(dbCopyTimeout));
      }
      int dumpExit = procDump.exitValue();

      long restoreWaitMs = deadlineMs - TimeBase.nowMs();
      if (restoreWaitMs <= 0 || !procRestore.waitFor(restoreWaitMs, TimeUnit.MILLISECONDS)) {
        procRestore.destroyForcibly();
        interruptAndJoin(pipeThread, dumpErrReader, restoreOutReader);
        throw new MigrationTaskFailedException("psql timed out after " +
            StringUtil.timeIntervalToString(dbCopyTimeout));
      }
      int restoreExit = procRestore.exitValue();
      pipeThread.join();
      dumpErrReader.join();
      restoreOutReader.join();

      if (dumpExit != 0 || restoreExit != 0) {
        List<String> lastLines = new ArrayList<>();
        if (!dumpErrLines.isEmpty()) {
          lastLines.add("pg_dump: " + lastOf(dumpErrLines));
        }
        if (!restoreOutLines.isEmpty()) {
          lastLines.add("psql: " + lastOf(restoreOutLines));
        }
        String err = String.format("PostgreSQL copy failed (pg_dump exit=%d, psql exit=%d)",
            dumpExit, restoreExit);
        if (!lastLines.isEmpty()) {
          err += "\n" + String.join("\n", lastLines);
        }
        throw new MigrationTaskFailedException(err);
      }
      String msg = "PostgreSQL DB copy completed in " +
        StringUtil.timeIntervalToString(TimeBase.msSince(startTime));
      auMover.addFinishedOther(msg);
      auMover.logReport(msg);
      migrationMgr.setIsDbMoved(true);
    } catch (IOException ioe) {
      throw new MigrationTaskFailedException("Request to move database failed", ioe);
    } catch (InterruptedException e) {
      throw new MigrationTaskFailedException("Request to move database was interrupted", e);
    } finally {
      stopUpdater();
    }
  }

  /** Returns the most recently added element of a {@link CircularFifoQueue}. */
  private static String lastOf(CircularFifoQueue<String> q) {
    return q.get(q.size() - 1);
  }

  /**
   * Interrupts each thread and waits for it to finish.  Called when the
   * copy has been aborted (timeout or forced kill) so that the I/O threads
   * do not linger after the method returns.
   */
  private static void interruptAndJoin(Thread... threads) {
    for (Thread t : threads) {
      t.interrupt();
    }
    for (Thread t : threads) {
      try {
        t.join();
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private long getDatabaseSize(String host, String port,
                               String user, String password, String dbName)
      throws MigrationTaskFailedException {
    PGSimpleDataSource ds = new PGSimpleDataSource();
    ds.setServerName(host);
    ds.setPortNumber(Integer.parseInt(port));
    ds.setDatabaseName(dbName);
    ds.setUser(user);
    ds.setPassword(password);
    try (Connection connection = ds.getConnection();
         Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(
             "SELECT pg_database_size(current_database())")) {
      return rs.next() ? rs.getLong(1) : 0L;
    } catch (SQLException ex) {
      throw new MigrationTaskFailedException(
          "PostgreSQL connection failed or error querying database size", ex);
    }
  }

  void startUpdater() {
    if (sizeUpdater != null) {
      log.warning("Progress updater already running; stopping old one first");
      stopUpdater();
    }
    log.debug("Starting progress updater");
    sizeUpdater= new DbSizeUpdater(v2dbhost, v2dbport, v2dbuser, v2dbpassword,v2dbname);
    sizeUpdater.start();
  }

  void stopUpdater() {
    if (sizeUpdater != null) {
      log.debug("Stopping progress updater");
      sizeUpdater.stopUpdater();
      sizeUpdater = null;
    }
  }

  class DbSizeUpdater extends LockssThread {

    private final String host;
    private final String port;
    private final String user;
    private final String password;
    private final String dbName;
    private volatile boolean goOn = true;

    DbSizeUpdater(String host, String port, String user, String password, String dbName) {
      super("DbMover:DBSizeUpdater");
      this.host = host;
      this.port = port;
      this.user = user;
      this.password = password;
      this.dbName = dbName;
    }

    @Override
    protected void lockssRun() {
      while (goOn) {
        loadCurrentDatabaseSize();
        try {
          sleep(DBSIZE_CHECK_INTERVAL);
        } catch (InterruptedException e) {
          // just wakeup and check for exit
        }
      }
    }

    private void loadCurrentDatabaseSize() {
      try {
        long curSize = getDatabaseSize(host, port, user, password, dbName);
        log.info(String.format("Destination database size = %d", curSize));
        auMover.dbBytesCopied = curSize - dstSize;
      } catch (MigrationTaskFailedException e) {
        log.warning("Couldn't get current DB size for progress display", e);
      }
    }

    // You can stop the scheduler with this method if needed
    private void stopUpdater() {
      goOn = false;
      this.interrupt();
    }
  }

}

