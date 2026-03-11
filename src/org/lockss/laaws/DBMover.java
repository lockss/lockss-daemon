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
    String err;
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
      else if(dbManager.isTypePostgresql()) {
        log.info("Migrating Postgresql Content..");
        srcSize = getDatabaseSize(v1host,v1port,v1user,v1password,v1dbname);
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
    if (StringUtil.isNullString(v2dbhost) ||
        v2dbuser == null || v2dbpassword == null) {
      String msg;
      if (StringUtil.isNullString(v2dbhost)) {
        msg = "Database copy failed: destination hostname was not supplied.";
      } else {
        msg = "Database copy failed: Missing database user name or password.";
      }
      throw new MigrationTaskFailedException(msg);
    }
  }

  private boolean dbHasMoved() {
    //if v1 datasource = v2 datasource then db has moved.
    Configuration config = ConfigManager.getCurrentConfig();
    return config.getBoolean(MigrationManager.PARAM_IS_DB_MOVED,
        MigrationManager.DEFAULT_IS_DB_MOVED);
  }

  private void copyDerbyDb() throws IOException {
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
                               migrationMgr.getConnectionPool());
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
        log.error("Subscriptions import to V2 failed: " + statusCode);
        Properties respProps = new Properties();
        conn.storeResponseHeaderInto(respProps, "");
        log.error("Response headers: " + respProps);
        log.error("Response: " +
          StringUtil.fromInputStream(conn.getResponseInputStream()));
      }
    } finally {
      FileUtil.delTree(bakDir);
    }
  }

  // NOTE: This command must result in pg_dump connecting to the DB
  // (esp. the V1 DB) using an IPv4 address, as the DB user:pass may
  // not be set up for IPv6
  static String createPgConnectionString(String user, String password, String host, String port, String dbname) {
    return "postgresql://"
      + user + ":"
      + password + "@"
      + ensureIPv4Host(host) + ":"
      + port + "/"
      + dbname;
  }

  static String ensureIPv4Host(String host) {
    if ("localhost".equalsIgnoreCase(host)) {
      return "127.0.0.1";
    }
    return host;
  }

  private void copyPostgresDb() throws MigrationTaskFailedException {
    String v1ConnectionString = createPgConnectionString(v1user, v1password, v1host, v1port, v1dbname);
    String v2ConnectionString = createPgConnectionString(v2dbuser, v2dbpassword, v2dbhost, v2dbport, v2dbname);

    // test at end causes pipe to return nonzero if either component
    // returns nonzero
    String copyCommand = "pg_dump -a --disable-triggers -S " + v2dbuser + " " + v1ConnectionString + " | psql -b " + v2ConnectionString + "; test ${PIPESTATUS[0]} -eq 0 -a ${PIPESTATUS[1]} -eq 0";
    log.debug("Running copy command: "+copyCommand);
    startUpdater();
    long startTime = TimeBase.nowMs();
    try {
      ProcessBuilder pb = new ProcessBuilder();
      pb.command("/bin/sh", "-c", copyCommand);
      pb.redirectErrorStream(true);
      Process proc = pb.start();
      BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
      List<String> outLines = new ArrayList<>();
      String line;
      while ((line = br.readLine()) != null) {
        outLines.add(line);
        log.debug(line);
      }
      int exitCode = proc.waitFor();
      log.debug("External process exited with code " + exitCode);
      if(exitCode != 0) {
        String err = "PostgreSQL copy script failed with exitCode " + exitCode;
        if (!outLines.isEmpty()) {
          err +=
//           "\n" + StringUtil.separatedString(outLines, "\n");
            "\n" + outLines.get(outLines.size() -1);
        }
        throw new MigrationTaskFailedException(err);
      }
      String msg = "PostgreSQL DB copy completed in " +
        StringUtil.timeIntervalToString(TimeBase.msSince(startTime));
      auMover.addFinishedOther(msg);
      auMover.logReport(msg);
      migrationMgr.setIsDbMoved(true);
    } catch (IOException ioe) {
      throw new MigrationTaskFailedException("Request to move database failed",
                                             ioe);
    } catch (InterruptedException e) {
      throw new MigrationTaskFailedException("Request to move database was interrupted",
                                             e);
    }
    finally {
      stopUpdater();
    }
  }

  private long getDatabaseSize(String host, String port,
                               String user, String password, String dbName)
      throws MigrationTaskFailedException {
    Connection connection = null;
    Statement stmt = null;
    ResultSet rs = null;
    String url = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
    long curSize = 0L;

    try {
      connection = DriverManager.getConnection(url, user, password);
      // Create Statement
      stmt = connection.createStatement();

      // Execute Query to get Database Size
      rs = stmt.executeQuery("SELECT pg_database_size('"+ dbName +"')");

      // If result exists, print the Database Size
      if (rs.next()) {
        curSize = rs.getLong(1);
      }
    } catch (SQLException ex) {
      String msg = "DBMover Connection to PostgreSQL failed or error executing query to get size";
      // redundant log
      throw new MigrationTaskFailedException(msg);
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (stmt != null) {
          stmt.close();
        }
        if (connection != null) {
          connection.close();
        }
      } catch (SQLException ex) {
        log.error("Exception cleaning up after getDatabaseSize(), ignored", ex);
      }
    }
    return curSize;
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
    private  boolean goOn = true;

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

