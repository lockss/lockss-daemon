package org.lockss.laaws;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.db.DbManager;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

public class DBMover extends Worker {
  public static final String DEFAULT_DB_USER = "LOCKSS";
  public static final String DEFAULT_HOST = "localhost";
  public static final String DEFAULT_V1_PASSWORD = "goodPassword";
  public static final String DEFAULT_v2_PORT = "24602";
  private static final Logger log = Logger.getLogger(DBMover.class);
  private static final String DB_USER_KEY = "user";
  private static final String DB_PASSWORD_KEY = "password";
  private static final String DB_SERVER_KEY = "serverName";
  private static final String DB_PORT_KEY = "portNumber";
  // v1 connection parameters
  String v1user = DEFAULT_DB_USER;
  String v1password = DEFAULT_V1_PASSWORD;
  String v1host = DEFAULT_HOST;
  String v1port = "5432";

  // v2 connection parameters
  String v2user=DEFAULT_DB_USER;
  String v2password;
  String v2host = DEFAULT_HOST;
  String v2port = DEFAULT_v2_PORT;

  String dbName="lockss";
  String srcSize;
  String dstSize;

  DbManager dbManager;

  public DBMover(V2AuMover auMover, MigrationTask task) {
    super(auMover, task);
    dbManager = LockssDaemon.getLockssDaemon().getDbManager();
  }

  public void run() {
    String err;
    try {
      if( dbManager.isTypeDerby()) {
        log.info("Migrating Derby DB Content");
        copyDerbyDb();
      }
      else if(dbManager.isTypePostgresql()) {
        log.info("Migrating Postgresql Content");
        if (initParams()) {
          srcSize = getDatabaseSize(v1host,v1port,v1user,v1password,dbName);
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
  }

  private void copyDerbyDb() {

  }

  boolean initParams() {
    Configuration config = ConfigManager.getCurrentConfig();
    Configuration v1config = config.getConfigTree(DbManager.DATASOURCE_ROOT);
    v1user = v1config.get(DB_USER_KEY, DbManager.DEFAULT_DATASOURCE_USER);
    v1password = v1config.get(DB_PASSWORD_KEY, DbManager.DEFAULT_DATASOURCE_PASSWORD);
    v1host = v1config.get(DB_SERVER_KEY, DbManager.DEFAULT_DATASOURCE_SERVERNAME);
    v1port = v1config.get(DB_PORT_KEY, DbManager.DEFAULT_DATASOURCE_PORTNUMBER_MYSQL);

    Configuration v2config = config.getConfigTree("v2."+DbManager.DATASOURCE_ROOT);
    v2user = v2config.get(DB_USER_KEY,DEFAULT_DB_USER);
    v2password = v2config.get(DB_PASSWORD_KEY);
    v2host = v2config.get(DB_SERVER_KEY);
    v2port = v2config.get(DB_PORT_KEY, DEFAULT_v2_PORT);

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

  private String getDatabaseSize(String host, String port, String user, String password, String dbName) {
    Connection connection = null;
    Statement stmt = null;
    ResultSet rs = null;
    String url = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
    String curSize = null;

    try {
      connection = DriverManager.getConnection(url, user, password);
      // Create Statement
      stmt = connection.createStatement();

      // Execute Query to get Database Size
      rs = stmt.executeQuery("SELECT pg_size_pretty(pg_database_size('"+ dbName +"'))");

      // If result exists, print the Database Size
      if (rs.next()) {
        curSize = rs.getString(1);
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

  private void copyPostgresDb() {
    String err;
    StringBuilder sbcmd = new StringBuilder();
    sbcmd.append("pg_dumpall -c --if-exists ");
    sbcmd.append("--dbname=postgresql://");
    sbcmd.append(v1user).append(":").append(v1password).append("@");
    sbcmd.append(v1host).append(":").append(v1port).append("/");
    sbcmd.append(dbName);
    sbcmd.append(" | ");
    sbcmd.append("psql -q --dbname=postgresql://");
    sbcmd.append(v2user).append(":").append(v2password).append("@");
    sbcmd.append(v2host).append(":").append(v2port).append("/");
    sbcmd.append(dbName);
    try {
      ProcessBuilder pb = new ProcessBuilder();
      pb.command("/bin/sh", "-c", sbcmd.toString());
 //     pb.inheritIO();
      pb.redirectErrorStream(true);
      Process proc = pb.start();
      log.debug("Running command: "+sbcmd.toString());
      int exitCode = proc.waitFor();
      log.debug("External process exited with code" + exitCode);
      BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
      String line;
      while ((line = br.readLine()) != null) {
        log.debug(line);
      }
      if(exitCode != 0) {
        err = "Call to move database failed with exitCode:" + exitCode;
        log.error(err);
        auMover.addError(err);
      }
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

}

