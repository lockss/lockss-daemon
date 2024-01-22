package org.lockss.laaws;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.httpclient.methods.multipart.*;
import org.apache.commons.httpclient.params.*;

import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.LockssRunnable;
import org.lockss.db.DbManager;
import org.lockss.db.DbVersion21To22Migrator;
import org.lockss.remote.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

public class DBMover extends Worker {
  public static final String DEFAULT_DB_USER = "LOCKSS";
  public static final String DEFAULT_HOST = "localhost";
  public static final String DEFAULT_V1_PASSWORD = "goodPassword";
  public static final String DEFAULT_v2_PORT = "24602";
  private final Logger log = Logger.getLogger(DBMover.class);
  private static final String DB_USER_KEY = "user";
  private static final String DB_PASSWORD_KEY = "password";
  private static final String DB_SERVER_KEY = "serverName";
  private static final String DB_PORT_KEY = "portNumber";
  private static final int V2_DEFAULT_CFGSVC_UI_PORT = 24621;

  // v1 connection parameters
  String v1user = DEFAULT_DB_USER;
  String v1password = DEFAULT_V1_PASSWORD;
  String v1host = DEFAULT_HOST;
  String v1port = "5432";
  String v1dbname = v1user;

  // v2 connection parameters
  String v2user=DEFAULT_DB_USER;
  String v2password;
  String v2host = DEFAULT_HOST;
  String v2port = DEFAULT_v2_PORT;
  String v2dbname;

  long srcSize;
  long dstSize;

  LockssDaemon daemon;
  DbManager dbManager;
  RemoteApi rapi;
  DbSizeUpdater sizeUpdater;

  public DBMover(V2AuMover auMover, MigrationTask task) {
    super(auMover, task);
    daemon = LockssDaemon.getLockssDaemon();
    dbManager = daemon.getDbManager();
    rapi = daemon.getRemoteApi();
  }

  public void run() {
    String err;
    try {
      if (dbHasMoved()) {
        log.info("Db has already been moved.");
        return;
      }
      if( dbManager.isTypeDerby()) {
        log.info("Migrating Derby DB Content");
        copyDerbyDb();
      }
      else if(dbManager.isTypePostgresql()) {
        log.info("Migrating Postgresql Content..");
        if (initParams()) {
          srcSize = getDatabaseSize(v1host,v1port,v1user,v1password,v1dbname);
          dstSize = getDatabaseSize(v2host,v2port,v2user,v2password,v2dbname);
          log.info("v1 db size = " + srcSize + ", v2 db size = " + dstSize);
          auMover.dbBytesCopied = 0;
          auMover.dbBytesTotal = srcSize;
          sizeUpdater= new DbSizeUpdater(v2host,v2port,v2user,v2password,v2dbname);
          copyPostgresDb();
        }
      }
      else {
        err = "Unable to move database of unsupported type";
        log.error(err);
        auMover.addError(err);
      }

    }catch(Exception ex) {
      log.error("DbMover failed: " + ex.getMessage());
      auMover.addError(ex.getMessage());
    }
    finally {
      if(sizeUpdater != null) {
        sizeUpdater.stopScheduler();
      }
    }
  }

  private void copyDerbyDb() throws IOException {
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

      LockssUrlConnection conn =
        UrlUtil.openConnection(LockssUrlConnection.METHOD_POST, restoreUrl, null);
      conn.setCredentials(v2user, v2password);
      conn.setUserAgent("lockss");
      Part[] parts = {
        new StringPart("lockssAction", "SelectRestoreTitles"),
        new StringPart("Verb", "5"),
        new FilePart("AuConfigBackupContents", bakFile)
      };
      conn.setRequestEntity(new MultipartRequestEntity(parts, new HttpMethodParams()));

      conn.execute();
      int statusCode = conn.getResponseCode();
      if (statusCode == 200) {
        log.info("Success!");
      } else {
        log.error("Restore failed : " + statusCode);
        log.error("Response: " +
          StringUtil.fromInputStream(conn.getResponseInputStream()));
      }
    } finally {
      FileUtil.delTree(bakDir);
    }
  }

  boolean initParams() {
    Configuration config = ConfigManager.getCurrentConfig();

    v1user = config.get(DbManager.PARAM_DATASOURCE_USER, DbManager.DEFAULT_DATASOURCE_USER);
    v1password = config.get(DbManager.PARAM_DATASOURCE_PASSWORD, DbManager.DEFAULT_DATASOURCE_PASSWORD);
    v1host = config.get(DbManager.PARAM_DATASOURCE_SERVERNAME, DbManager.DEFAULT_DATASOURCE_SERVERNAME);
    v1port = config.get(DbManager.PARAM_DATASOURCE_PORTNUMBER, DbManager.DEFAULT_DATASOURCE_PORTNUMBER_PG);
    v1dbname = config.get(DbManager.PARAM_DATASOURCE_DATABASENAME, v1user);

    Configuration v2config = config.getConfigTree("v2");
    v2user = v2config.get(DbManager.PARAM_DATASOURCE_USER);
    v2password = v2config.get(DbManager.PARAM_DATASOURCE_PASSWORD);
    v2host = v2config.get(DbManager.PARAM_DATASOURCE_SERVERNAME);
    v2port = v2config.get(DbManager.PARAM_DATASOURCE_PORTNUMBER);
    v2dbname = v2config.get(DbManager.PARAM_DATASOURCE_DATABASENAME);;
    if (StringUtil.isNullString(v2host)) {
      String msg = "DbMover failed: destination hostname was not supplied.";
      auMover.addError(msg);
      return false;
    }
    if (v2user == null || v2password == null) {
      String msg = "DbMover failed: Missing database user name or password.";
      log.error(msg);
      auMover.addError(msg);
      return false;
    }
    return true;
  }
  private boolean dbHasMoved() {
    //if v1 datasource = v2 datasource then db has moved.
    Configuration config = ConfigManager.getCurrentConfig();
    Configuration v1 = config.getConfigTree(DbManager.DATASOURCE_ROOT);
    Configuration v2 = config.getConfigTree("v2."+DbManager.DATASOURCE_ROOT);
    return v1.equals(v2);
  }


  private void copyPostgresDb() {
    String err;
    StringBuilder sbcmd = new StringBuilder();
    sbcmd.append("pg_dump -C  ");
    sbcmd.append("--dbname=postgresql://");
    sbcmd.append(v1user).append(":").append(v1password).append("@");
    sbcmd.append(v1host).append(":").append(v1port).append("/");
    sbcmd.append(v1dbname);
    sbcmd.append(" | ");
    sbcmd.append("psql -q --dbname=postgresql://");
    sbcmd.append(v2user).append(":").append(v2password).append("@");
    sbcmd.append(v2host).append(":").append(v2port).append("/");
    sbcmd.append(v2dbname);
    sbcmd.append(" && echo $?");
    try {
      ProcessBuilder pb = new ProcessBuilder();
      pb.command("/bin/sh", "-c", sbcmd.toString());
      pb.redirectErrorStream(true);
      Process proc = pb.start();
      BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
      String line;

      while ((line = br.readLine()) != null) {
        log.debug(line);
      }
      log.debug("Running command: "+sbcmd.toString());
      sizeUpdater.startScheduler();
      int exitCode = proc.waitFor();
      log.debug("External process exited with code" + exitCode);
      if(exitCode != 0) {
        err = "Call to move database failed with exitCode:" + exitCode;
        log.error(err);
        auMover.addError(err);
      }
      sizeUpdater.stopScheduler();
    } catch (IOException ioe) {
      err = "Request to move database failed: " + ioe.getMessage();
      log.error(err, ioe);
      auMover.addError(err);
    } catch (InterruptedException e) {
      err = "Request to Move Database was interuppted, " + e.getMessage();
      log.error(err);
      auMover.addError(err);
    }
  }

  private long getDatabaseSize(String host, String port, String user, String password, String dbName) {
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
      String err = "DBMover Connection to PostgreSQL failed or error executing query to get size.";
      log.error(err,ex);
      auMover.addError(err+": " +ex.getMessage());
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
        final String msg = "Exception thrown while getting size";
        log.error(msg, ex);
        auMover.addError(msg +": " +ex.getMessage());
      }
    }
    return curSize;
  }

  class DbSizeUpdater extends LockssRunnable {

    private final String host;
    private final String port;
    private final String user;
    private final String password;
    private final String dbName;
    private ScheduledExecutorService scheduler;

    DbSizeUpdater(String host, String port, String user, String password, String dbName) {
      super("DbMover:DBSizeUpdater");
      this.host = host;
      this.port = port;
      this.user = user;
      this.password = password;
      this.dbName = dbName;
    }
    public void startScheduler() {
      final Runnable task = this::lockssRun;
      scheduler = Executors.newScheduledThreadPool(1);
      scheduler.scheduleAtFixedRate(task, 0, 1, TimeUnit.MINUTES);
    }

    @Override
    protected void lockssRun() {
      long curSize = getDatabaseSize(host, port, user, password, dbName);
      log.info("Destination database size = " + curSize);
      auMover.dbBytesCopied = curSize - dstSize;
    }

    // You can stop the scheduler with this method if needed
    public void stopScheduler() {
      if (scheduler != null) {
        scheduler.shutdown();
        scheduler = null;
      }
    }

  }

}

