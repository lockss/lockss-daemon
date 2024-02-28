package org.lockss.laaws;

import java.io.*;
import java.net.*;
import java.sql.*;
import org.apache.commons.httpclient.methods.multipart.*;
import org.apache.commons.httpclient.params.*;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.LockssThread;
import org.lockss.db.DbManager;
import org.lockss.remote.*;
import org.lockss.servlet.MigrateContent;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

import static org.lockss.laaws.MigrationConstants.*;

public class DBMover extends Worker {
  public static final String DEFAULT_DB_USER = "LOCKSS";
  public static final String DEFAULT_HOST = "localhost";
  public static final String DEFAULT_V1_PASSWORD = "insecure";
  private static final long DBSIZE_CHECK_INTERVAL = 2*Constants.SECOND;
  private final Logger log = Logger.getLogger(DBMover.class);

  // v1 connection parameters
  String v1user = DEFAULT_DB_USER;
  String v1password = DEFAULT_V1_PASSWORD;
  String v1host = DEFAULT_HOST;
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
  RemoteApi rapi;
  DbSizeUpdater sizeUpdater;

  public DBMover(V2AuMover auMover, MigrationTask task) {
    super(auMover, task);
    daemon = LockssDaemon.getLockssDaemon();
    dbManager = daemon.getDbManager();
    rapi = daemon.getRemoteApi();
  }

  public void run() throws MigrationTaskFailedException {
    String err;
    try {
      if (dbHasMoved()) {
        log.info("Db has already been moved.");
        return;
      }
      if (initParams()) {
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
          err = "Unable to move database of unsupported type";
          log.error(err);
          auMover.addError(err);
        }
      }
    } catch(Exception ex) {
      String msg = "DbMover failed: " + ex.toString();
      log.error(msg, ex);
      auMover.addError(msg);
      throw new MigrationTaskFailedException(msg);
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
      conn.setCredentials(v2user, v2pass);
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
        auMover.getMigrationMgr().setIsDbMoved(true);
      } else {
        log.error("Restore failed : " + statusCode);
        log.error("Response: " +
          StringUtil.fromInputStream(conn.getResponseInputStream()));
        terminated = true;
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
    if (StringUtil.isNullString(v2dbhost)) {
      String msg = "DbMover failed: destination hostname was not supplied.";
      log.error(msg);
      auMover.addError(msg);
      return false;
    }
    if (v2dbuser == null || v2dbpassword == null) {
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
    return config.getBoolean(MigrationManager.PARAM_IS_DB_MOVED,
        MigrationManager.DEFAULT_IS_DB_MOVED);
  }

  private String createPgConnectionString(String user, String password, String host, String port, String dbname) {
    return "postgresql://"
      + user + ":"
      + password + "@"
      + host + ":"
      + port + "/"
      + dbname;
  }


  private void copyPostgresDb() throws IOException, InterruptedException {
    String err =null;
    String v1ConnectionString = createPgConnectionString(v1user, v1password, v1host, v1port, v1dbname);
    String v2ConnectionString = createPgConnectionString(v2dbuser, v2dbpassword, v2dbhost, v2dbport, v2dbname);

    String copyCommand = "pg_dump -a " + v1ConnectionString + " | psql -q " + v2ConnectionString + " && echo $?";
    log.debug("Running copy command: "+copyCommand);
    startUpdater();
    try {
      ProcessBuilder pb = new ProcessBuilder();
      pb.command("/bin/sh", "-c", copyCommand);
      pb.redirectErrorStream(true);
      Process proc = pb.start();
      BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
      String line;
      while ((line = br.readLine()) != null) {
        log.debug(line);
      }
      int exitCode = proc.waitFor();
      log.debug("External process exited with code: " + exitCode);
      if(exitCode != 0) {
        err = "Call to move database failed with exitCode:" + exitCode;
        throw new IOException(err);
      }

    } catch (IOException ioe) {
      err = "Request to move database failed: " + ioe.getMessage();
      throw ioe;
    } catch (InterruptedException e) {
      err = "Request to move database was interrupted, " + e.getMessage();
      throw e;
    }
    finally {
      stopUpdater();
      if(err != null) {
        log.error(err);
        auMover.addError(err);
        terminated = true;
      }
      else {
        auMover.getMigrationMgr().setIsDbMoved(true);
      }
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

  void startUpdater() {
    if (sizeUpdater != null) {
      log.warning("Updater already running; stopping old one first");
      stopUpdater();
    } else {
      log.info("Starting handler");
    }
    sizeUpdater= new DbSizeUpdater(v2dbhost, v2dbport, v2dbuser, v2dbpassword,v2dbname);
    sizeUpdater.start();
  }

  void stopUpdater() {
    if (sizeUpdater != null) {
      log.info("Stopping handler");
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
      long curSize = getDatabaseSize(host, port, user, password, dbName);
      log.info(String.format("Destination database size = %d", curSize));
      auMover.dbBytesCopied = curSize - dstSize;
    }

    // You can stop the scheduler with this method if needed
    private void stopUpdater() {
      goOn = false;
      this.interrupt();
    }
  }

}

