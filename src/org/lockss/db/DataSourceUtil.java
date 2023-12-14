package org.lockss.db;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.derby.drda.NetworkServerControl;
import org.apache.derby.jdbc.ClientDataSource;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.util.Deadline;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

import javax.sql.DataSource;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.*;

import static org.lockss.db.DbManagerSql.FIND_DATABASE_QUERY_MYSQL;
import static org.lockss.db.DbManagerSql.FIND_DATABASE_QUERY_PG;

public class DataSourceUtil {
  private static final Logger log = Logger.getLogger(DataSourceUtil.class);

  private static final int maxRetryCount = DbManager.DEFAULT_MAX_RETRY_COUNT;
  private static final long retryDelay = DbManager.DEFAULT_RETRY_DELAY;
  private static final int fetchSize = DbManager.DEFAULT_FETCH_SIZE;

  private static DataSource createDataSource(String dsClassName) throws DbException {
    // Locate the datasource class.
    Class<?> dataSourceClass;

    try {
      dataSourceClass = Class.forName(dsClassName);
    } catch (Throwable t) {
      throw new DbException("Cannot locate datasource class '" + dsClassName
          + "'", t);
    }

    // Create the datasource.
    try {
      return ((DataSource) dataSourceClass.newInstance());
    } catch (ClassCastException cce) {
      throw new DbException("Class '" + dsClassName + "' is not a DataSource.",
          cce);
    } catch (Throwable t) {
      throw new DbException("Cannot create instance of datasource class '"
          + dsClassName + "'", t);
    }
  }

  /**
   * Transforms datasource configuration loaded from a configuration file or
   * other source into runtime-specific datasource configuration.
   */
  public static Configuration getRuntimeDataSourceConfig(Configuration mCfg) throws DbException {
    // Create the new datasource configuration.
    Configuration dsCfg = ConfigManager.newConfiguration();

    // Populate it from the current configuration datasource tree.
    dsCfg.copyFrom(mCfg.getConfigTree(DbManager.DATASOURCE_ROOT));
    String dsClassName = dsCfg.get("className");

    if (DbManagerSql.isTypeDerby(dsClassName)) {
      // Suppress Derby from creating the database, if enabled
      dsCfg.remove("createDatabase");
    } else if (DbManagerSql.isTypeMysql(dsClassName)) {
      // Map portNumber to just port for MySQL
      dsCfg.put("port", dsCfg.get("portNumber"));
      dsCfg.remove("portNumber");
    }

    if (StringUtil.isNullString(dsCfg.get("databaseName"))) {
      if (DbManagerSql.isTypeDerby(dsClassName)) {
        // Too late to reconstruct this from migration target config
        throw new DbException("Incomplete Derby configuration");
      } else if (DbManagerSql.isTypePostgresql(dsClassName) ||
                 DbManagerSql.isTypeMysql(dsClassName)) {
        dsCfg.put("databaseName", dsCfg.get("user"));
      }
    }

    return dsCfg;
  }

  public static boolean validateDataSourceConfig(Configuration dsCfg) throws DbException {
    try {
      DataSource ds = getDataSource(dsCfg);
      String dsClassName = dsCfg.get("className");
      String databaseName = dsCfg.get("databaseName");

      Connection conn = getConnection(ds, maxRetryCount, retryDelay, true);

      if (DbManagerSql.isTypePostgresql(dsClassName)) {
        return getSqlDatabaseExists(conn, databaseName, FIND_DATABASE_QUERY_PG);
      } else if (DbManagerSql.isTypeMysql(dsClassName)) {
        return getSqlDatabaseExists(conn, databaseName, FIND_DATABASE_QUERY_MYSQL);
      } else {
        // TODO: How to check if a Derby database exists?
      }
    } catch (Throwable t) {
      throw new DbException("DataSource configuration validation failed", t);
    }

    return false;
  }

  private static boolean getSqlDatabaseExists(Connection conn, String databaseName, String sqlQuery)
      throws SQLException {

    if (conn == null) {
      throw new IllegalArgumentException("Null connection.");
    }

    boolean result;
    PreparedStatement findDb = null;
    ResultSet resultSet = null;

    try {
      findDb = JdbcBridge.prepareStatement(conn, sqlQuery,
          Statement.NO_GENERATED_KEYS, maxRetryCount, retryDelay, fetchSize);

      findDb.setString(1, databaseName);

      resultSet = JdbcBridge.executeQuery(findDb, maxRetryCount, retryDelay);
      result = resultSet.next();
    } catch (SQLException sqle) {
      log.error("Cannot find database", sqle);
      log.error("databaseName = '" + databaseName + "'.");
      log.error("SQL = '" + FIND_DATABASE_QUERY_PG + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot find database", re);
      log.error("databaseName = '" + databaseName + "'.");
      log.error("SQL = '" + FIND_DATABASE_QUERY_PG + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
      JdbcBridge.safeCloseStatement(findDb);
    }

    return result;
  }

  private static Connection getConnection(DataSource ds, int maxRetryCount, long retryDelay,
                                  boolean autoCommit) throws SQLException {
    if (ds == null) {
      throw new IllegalArgumentException("Null datasource");
    }

    return JdbcBridge.getConnection(ds, maxRetryCount, retryDelay, autoCommit);
  }

  private static DataSource getDataSource(Configuration dsCfg) throws DbException {
    // Get input from database configuration
    String dsClassName = dsCfg.get("className");
    String dsUser = dsCfg.get("user");
    String dsPassword = dsCfg.get("password");

    // Validate input: Check whether authentication is required and it is not available.
    if (StringUtil.isNullString(dsUser)
        || (DbManagerSql.isTypeDerby(dsClassName)
        && !"org.apache.derby.jdbc.EmbeddedDataSource"
        .equals(dsClassName)
        && !"org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource"
        .equals(dsClassName)
        && StringUtil.isNullString(dsPassword))) {
      // Yes: Report the problem.
      throw new DbException("Missing required authentication");
    }

    // Create datasource
    DataSource ds = createDataSource(dsCfg.get("className"));

    // Initialize the datasource properties
    initializeDataSourceProperties(dsCfg, ds);

    try {
      // Q: Why was this necessary? Likely because we aren't able to open a connection
      //  to a database that doesn't exist yet, but need a connection to create the DB
//      if (DbManagerSql.isTypePostgresql(dsClassName)) {
//        BeanUtils.setProperty(ds, "databaseName", "information_schema");
//      } else if (DbManagerSql.isTypeMysql(dsClassName)) {
//        BeanUtils.setProperty(ds, "databaseName", "template1");
//      }

      // Q: I don't think we need this? We expect the database to exist so can use the
      //  database name as configured by the initializeDataSourceProperties call above
//      if (DbManagerSql.isTypePostgresql(dsClassName) ||
//          DbManagerSql.isTypeMysql(dsClassName)) {
//        String dsDatabaseName = dsCfg.get("databaseName");
//        BeanUtils.setProperty(ds, "databaseName", dsDatabaseName);
//      }
    } catch (Throwable t) {
      throw new DbException("Could not initialize the datasource", t);
    }

    if (DbManagerSql.isTypeDerby(dsClassName)) {
      if (ds instanceof ClientDataSource) {
        ClientDataSource cds = (ClientDataSource) ds;
        startDerbyNetworkServerControl(cds);
      }
    }

    return ds;
  }

  private static void startDerbyNetworkServerControl(ClientDataSource cds)
      throws DbException {
    final String DEBUG_HEADER = "startDerbyNetworkServerControl(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Get the configured server name.
    String serverName = cds.getServerName();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "serverName = '" + serverName + "'.");

    // Determine the IP address of the server.
    InetAddress inetAddr;

    try {
      inetAddr = InetAddress.getByName(serverName);
    } catch (UnknownHostException uhe) {
      throw new DbException("Cannot determine the IP address of server '"
          + serverName + "'", uhe);
    }

    // Get the configured server port number.
    int serverPort = cds.getPortNumber();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "serverPort = " + serverPort + ".");

    // Create the network server control.
    NetworkServerControl networkServerControl;
    try {
      networkServerControl = new NetworkServerControl(inetAddr, serverPort);
    } catch (Exception e) {
      throw new DbException("Cannot create the Network Server Control", e);
    }

    // Start the network server control.
    try {
      networkServerControl.start(null);
    } catch (Exception e) {
      throw new DbException("Cannot start the Network Server Control", e);
    }

    // Wait for the network server control to be ready.
    for (int i = 0; i < 40; i++) { // At most 20 seconds.
      try {
        networkServerControl.ping();
        log.debug(DEBUG_HEADER + "Remote access to Derby database enabled");
        return;
      } catch (Exception e) {
        // Control is not ready: wait and try again.
        try {
          Deadline.in(500).sleep(); // About 1/2 second.
        } catch (InterruptedException ie) {
          break;
        }
      }
    }

    // At this point we give up.
    throw new DbException("Cannot enable remote access to Derby database");
  }

  private static void initializeDataSourceProperties(Configuration dsCfg,
                                                     DataSource ds) throws DbException {
    String dsClassName = dsCfg.get("className");

    for (String key : dsCfg.keySet()) {
      if (isApplicableDataSourceProperty(dsClassName, key)) {
        String value = dsCfg.get(key);
        try {
          BeanUtils.setProperty(ds, key, value);
        } catch (Throwable t) {
          throw new DbException("Cannot set value '" + value
              + "' for property '" + key
              + "' for instance of datasource class '" + dsClassName, t);
        }
      }
    }
  }

  private static boolean isApplicableDataSourceProperty(String dsClassName, String name) {
    // Properties that are applicable to all DataSources
    if ("serverName".equals(name)
        || "dataSourceName".equals(name) || "databaseName".equals(name)
        || "user".equals(name)
        || "description".equals(name)) {
      return true;
    }

    // Properties applicable to only the Derby database
    if (DbManagerSql.isTypeDerby(dsClassName)
        && ("createDatabase".equals(name) || "shutdownDatabase".equals(name)
        || "portNumber".equals(name) || "password".equals(name))) {
      return true;

    // Properties applicable to only the PostgreSQL database
    } else if (DbManagerSql.isTypePostgresql(dsClassName)
        && ("initialConnections".equals(name) || "maxConnections".equals(name)
        || "portNumber".equals(name) || "password".equals(name))) {
      return true;

    // Properties applicable to only the MySQL database
    } else if (DbManagerSql.isTypeMysql(dsClassName) && "port".equals(name)) {
      return true;
    }

    // Any other named property is not applicable
    return false;
  }
}
