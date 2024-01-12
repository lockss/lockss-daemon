package org.lockss.laaws;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.db.DbManager;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

public class DBMover extends Worker {
  private static final Logger log = Logger.getLogger(DBMover.class);
  public static final String DEFAULT_USER = "LOCKSS";
  public static final String DEFAULT_HOST = "localhost";
  public static final String DEFAULT_V1_PASSWORD = "goodPassword";
  public static final String DEFAULT_v2_PORT = "24602";
  // v1 connection parameters
  String v1user = "LOCKSS";
  String v1password = "goodPassword";
  String v1host = "localhost";
  String v1port = "5432";

  // v2 connection parameters
  String v2user="LOCKSS";
  String v2password;
  String v2host = "localhost";
  String v2port = "24602";

  String dbName="lockss";

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

  private void copyPostgresDb() {
    String err;
    StringBuilder sbcmd = new StringBuilder();
    sbcmd.append("pg_dumpall -c --if-exists ");
    sbcmd.append("--dbname=postgresql://");
    sbcmd.append(v1user).append(":").append(v1password).append("@");
    sbcmd.append(v1host).append(":").append(v1port).append("/");
    sbcmd.append(dbName);
    sbcmd.append("|");
    sbcmd.append("psql -q --dbname=postgresql://");
    sbcmd.append(v2user).append(":").append(v2password).append("@");
    sbcmd.append(v2host).append(":").append(v2port).append("/");
    sbcmd.append(dbName);
    try {
      ProcessBuilder pb = new ProcessBuilder();
      pb.command("/bin/sh", "-c", sbcmd.toString());
      pb.inheritIO();
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

  boolean initParams() {
    Configuration config = ConfigManager.getCurrentConfig();
    Configuration v1config = config.getConfigTree(DbManager.DATASOURCE_ROOT);
    v1user = v1config.get("user", DbManager.DEFAULT_DATASOURCE_USER);
    v1password = v1config.get("password", DbManager.DEFAULT_DATASOURCE_PASSWORD);
    v1host = v1config.get("serverName", DbManager.DEFAULT_DATASOURCE_SERVERNAME);
    v1port = v1config.get("portNumber", DbManager.DEFAULT_DATASOURCE_PORTNUMBER_MYSQL);

    Configuration v2config = config.getConfigTree("v2."+DbManager.DATASOURCE_ROOT);
    v2user = v2config.get("user",DEFAULT_USER);
    v2password = v2config.get("password");
    v2host = v2config.get("serverName");
    v2port = v2config.get("portNumber", DEFAULT_v2_PORT);

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

}

