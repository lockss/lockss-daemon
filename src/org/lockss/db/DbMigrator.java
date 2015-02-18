/*
 * $Id$
 */

/*

 Copyright (c) 2013-2014 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.db;

import static org.lockss.db.SqlConstants.*;
import gnu.getopt.Getopt;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.sql.DataSource;
import org.apache.commons.beanutils.BeanUtils;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

/**
 * Database migrator.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class DbMigrator extends DbManager {
  private static final Logger log = Logger.getLogger(DbMigrator.class);

  // Name of the sequence translation table.
  private static final String SEQ_TRANSLATION_TABLE = "seq_translation";

  // Table name column.
  private static final String TABLE_NAME_COLUMN = "table_name";

  // Source sequence number column.
  private static final String SOURCE_SEQ_COLUMN = "source_seq";

  // Target sequence number column.
  private static final String TARGET_SEQ_COLUMN = "target_seq";

  // Length of the table name column.
  public static final int MAX_TABLE_NAME_COLUMN = 48;

  // Query to create the table used to translate sequences.
  private static final String CREATE_SEQ_TRANSLATION_TABLE_QUERY = "create "
      + "table " + SEQ_TRANSLATION_TABLE + " ("
      + TABLE_NAME_COLUMN + " varchar(" + MAX_TABLE_NAME_COLUMN + ") not null,"
      + SOURCE_SEQ_COLUMN + " bigint not null,"
      + TARGET_SEQ_COLUMN + " bigint not null"
      + ")";

  // Query to create the table for recording plugins.
  private static final String CREATE_PLUGIN_TABLE_QUERY = "create table "
      + PLUGIN_TABLE + " ("
      + PLUGIN_SEQ_COLUMN + " --BigintSerialPk--,"
      + PLUGIN_ID_COLUMN + " varchar(" + MAX_PLUGIN_ID_COLUMN + ") not null,"
      + PLATFORM_SEQ_COLUMN + " bigint not null references " + PLATFORM_TABLE
      + " (" + PLATFORM_SEQ_COLUMN + ") on delete cascade"
      + ")";

  // Query to find rows in the sequence translation table.
  private static final String FIND_TABLE_TRANSLATION_QUERY = "select "
      + SOURCE_SEQ_COLUMN
      + "," + TARGET_SEQ_COLUMN
      + " from " + SEQ_TRANSLATION_TABLE
      + " where " + TABLE_NAME_COLUMN + " = ?";

  // Query to add a translated sequence.
  private static final String INSERT_SEQ_TRANSLATION_QUERY = "insert into "
      + SEQ_TRANSLATION_TABLE
      + "(" + TABLE_NAME_COLUMN
      + "," + SOURCE_SEQ_COLUMN
      + "," + TARGET_SEQ_COLUMN
      + ") values (?,?,?)";

  // The database definitions.
  private String sourceDbDefinition = null;
  private String targetDbDefinition = null;

  // The data source class names.
  private String sourceClassName = null;
  private String targetClassName = null;

  // The data sources.
  private DataSource sourceDataSource = null;
  private DataSource targetDataSource = null;

  // The data source users.
  private String sourceUserName = null;
  private String targetUserName = null;

  // The SQL code executors.
  private DbManagerSql sourceDbManagerSql = null;
  private DbManagerSql targetDbManagerSql = null;

  // The sequence mapping storage.
  Map<String, Map<Long, Long>> sequenceTranslation =
      new HashMap<String, Map<Long, Long>>();

  /**
   * The main method.
   * 
   * @param args
   *          A String[] with the command line arguments.<br />
   * 
   *          Two arguments are needed, one with the option -s for the source
   *          database parameters (separated by semicolons) and another with the
   *          option -t for the target database parameters (also separated by
   *          semicolons).<br />
   * 
   *          Examples of the arguments needed to migrate from Derby to
   *          PostgreSQL look as follows:<br />
   *
   *          -sclassName=org.apache.derby.jdbc.EmbeddedDataSource;
   *                      databaseName=/path/to/dir/DbManager;
   *                      portNumber=1527;
   *                      serverName=localhost;
   *                      user=LOCKSS<br />
   *          -tclassName=org.postgresql.ds.PGSimpleDataSource;
   *                      databaseName=LOCKSS;
   *                      portNumber=5432;
   *                      serverName=localhost;
   *                      user=LOCKSS;
   *                      password=somePasswordIfNeeded
   * @throws DbMigratorException
   *           if there are problems migrating the data.
   */
  public static void main(String[] args) throws DbMigratorException {
    log.info("Started.");
    DbMigrator migrator = new DbMigrator();

    // Perform the migration process.
    migrator.process(args);
    log.info("Done.");
  }

  /**
   * Performs the migration process.
   * 
   * @param args
   *          A String[] with the command line arguments.
   * @throws DbMigratorException
   *           if there are problems migrating the data.
   */
  private void process(String args[]) throws DbMigratorException {
    final String DEBUG_HEADER = "process(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Get the migration parameters from the command line.
    parseCommandLine(args);

    // Do nothing more if any of the two database definitions are missing.
    if (StringUtil.isNullString(sourceDbDefinition)
	|| StringUtil.isNullString(targetDbDefinition)) {
      if (StringUtil.isNullString(sourceDbDefinition)) {
	log.error("No definition for the source database was found.");
      }

      if (StringUtil.isNullString(targetDbDefinition)) {
	log.error("No definition for the target database was found.");
      }

      return;
    }

    // Initialize the data sources and the target database, if necessary.
    initialize();

    // Migrate the database.
    migrate();

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Parses the command line arguments and saves their values.
   * 
   * @param args
   *          A String[] with the command line arguments.
   */
  private void parseCommandLine(String args[]) {
    final String DEBUG_HEADER = "parseCommandLine(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "args.length = " + args.length + ".");

    int optionLabel;
    String optionargument;

    // Specify the valid command line options.
    Getopt optionParser = new Getopt("DbMigrator", args, "s::t::");

    // Loop through all the command line arguments.
    while ((optionLabel = optionParser.getopt()) != -1) {
      switch (optionLabel) {
      	// Source database.
      	case 's':
      	  optionargument = optionParser.getOptarg();
      	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Source argument = '"
      	      + optionargument + "'.");

      	  sourceDbDefinition = optionargument;
      	  break;
      	// Target database.
      	case 't':
      	  optionargument = optionParser.getOptarg();
      	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Target argument = '"
      	      + optionargument + "'.");

      	  targetDbDefinition = optionargument;
      	  break;
      	// Any other option.
      	default:
      	  return;
      }
    }
  }

  /**
   * Initializes the data sources and the target database, if necessary.
   *
   * @throws DbMigratorException
   *           if there are initialization problems.
   */
  private void initialize() throws DbMigratorException {
    final String DEBUG_HEADER = "initialize(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    try {
      // Get a map of the source database definition properties.
      Map<String, String> sourceProperties =
	  mapDbDefinition(sourceDbDefinition);

      // Get the source database data source class name.
      sourceClassName = sourceProperties.get("className");

      // Get the source database user name.
      sourceUserName = sourceProperties.get("user");

      // Create the source database data source.
      sourceDataSource = createDataSource(sourceClassName);

      // Create the source SQL code executor.
      sourceDbManagerSql = new DbManagerSql(sourceDataSource, sourceClassName,
	  sourceUserName, DEFAULT_MAX_RETRY_COUNT, DEFAULT_RETRY_DELAY,
	  DEFAULT_FETCH_SIZE);

      // Initialize the source database data source properties.
      initializeDataSourceProperties(sourceProperties, sourceDataSource);

      // Get a map of the target database definition properties.
      Map<String, String> targetProperties =
	  mapDbDefinition(targetDbDefinition);

      // Get the target database data source class name.
      targetClassName = targetProperties.get("className");

      // Get the target database user name.
      targetUserName = targetProperties.get("user");

      // Create the target database data source.
      targetDataSource = createDataSource(targetClassName);

      // Create the target SQL code executor.
      targetDbManagerSql = new DbManagerSql(targetDataSource, targetClassName,
	  targetUserName, DEFAULT_MAX_RETRY_COUNT, DEFAULT_RETRY_DELAY,
	  DEFAULT_FETCH_SIZE);

      // Do nothing more if the source and target databases are not different.
      if (!(sourceDbManagerSql.isTypeDerby()
	  && !targetDbManagerSql.isTypeDerby()
	    || sourceDbManagerSql.isTypePostgresql()
	    && !targetDbManagerSql.isTypePostgresql())) {
	String message = "The source and target databases are not different.";
	log.error(message);
	throw new DbMigratorException(message);
      }

      // Check whether the PostgreSQL database is the target.
      if (targetDbManagerSql.isTypePostgresql()) {
	// Yes: Initialize the database, if necessary.
	initializePostgresqlDbIfNeeded(targetProperties);
      }

      // Initialize the target database data source properties.
      initializeDataSourceProperties(targetProperties, targetDataSource);

      // Check whether the PostgreSQL database is the target.
      if (targetDbManagerSql.isTypePostgresql()) {
	// Yes: Get the name of the database from the configuration.
	String databaseName = targetProperties.get("databaseName");
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "databaseName = " + databaseName);

	// Create the schema if it does not exist.
	targetDbManagerSql.createPostgresqlSchemaIfMissing(databaseName,
	    targetDataSource);
      }
    } catch (DbException dbe) {
      throw new DbMigratorException(dbe);
    } catch (SQLException sqle) {
      throw new DbMigratorException(sqle);
    } catch (RuntimeException re) {
      throw new DbMigratorException(re);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides a map of the properties of a database passed in the command line.
   * 
   * @param dbDefinition
   *          A String with the properties of a database passed in the command
   *          line.
   * @return a Map<String, String> with the map of the database properties.
   */
  private Map<String, String> mapDbDefinition(String dbDefinition) {
    final String DEBUG_HEADER = "mapDbDefinition(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "dbDefinition = '" + dbDefinition + "'.");

    Map<String, String> properties = new HashMap<String, String>();

    // Loop through all the properties in the database definition.
    for (String property : StringUtil.breakAt(dbDefinition, ";", true)) {
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "property = '" + property + "'.");

      // Get the property name and value.
      Vector<String> nameValue = StringUtil.breakAt(property, "=", true);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "nameValue = '" + nameValue + "'.");

      if (nameValue.size() == 2) {
	// Key the value by the name.
	properties.put(nameValue.get(0), nameValue.get(1));
      } else {
	log.error("Invalid property definition - '" + property + "'");
      }
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "properties.size() = " + properties.size());
    return properties;
  }

  /**
   * Initializes the properties of the data source using the specified database
   * properties.
   * 
   * @param dbProperties
   *          A Map<String, String> with the database properties.
   * @param ds
   *          A DataSource with the data source to be initialized.
   * @throws DbException
   *           if the data source properties could not be initialized.
   */
  private void initializeDataSourceProperties(Map<String, String> dbProperties,
      DataSource ds) throws DbException {
    final String DEBUG_HEADER = "initializeDataSourceProperties(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    String dsClassName = dbProperties.get("className");
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "dsClassName = '" + dsClassName + "'.");

    // Loop through all the configured data source property names.
    for (String key : dbProperties.keySet()) {
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "key = '" + key + "'.");

      // Get the value of the property.
      String value = dbProperties.get(key);
      if (log.isDebug3() && !"password".equals(key))
	log.debug3(DEBUG_HEADER + "value = '" + value + "'.");

      // Set the property value in the data source.
      try {
	BeanUtils.setProperty(ds, key, value);
      } catch (Throwable t) {
	throw new DbException("Cannot set value '" + value + "' for property '"
	    + key + "' for instance of datasource class '" + dsClassName, t);
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Initializes a PostreSQl database, if it does not exist already.
   * 
   * @param dbProperties
   *          A Map<String, String> with the database properties.
   * @throws DbException
   *           if the database discovery or initialization processes failed.
   */
  private void initializePostgresqlDbIfNeeded(Map<String, String> dbProperties)
      throws DbMigratorException {
    final String DEBUG_HEADER = "initializePostgresqlDbIfNeeded(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    try {
      // Create a data source.
      DataSource ds = createDataSource(dbProperties.get("className"));

      // Initialize the datasource properties.
      initializeDataSourceProperties(dbProperties, ds);

      // Get the configured database name.
      String databaseName = dbProperties.get("databaseName");
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "databaseName = " + databaseName);

      // Replace the database name with the standard connectable template.
      try {
	BeanUtils.setProperty(ds, "databaseName", "template1");
      } catch (Throwable t) {
	throw new DbMigratorException("Could not initialize the datasource", t);
      }

      // Create the database if it does not exist.
      targetDbManagerSql.createPostgreSqlDbIfMissing(ds, databaseName);
    } catch (DbException dbe) {
      throw new DbMigratorException(dbe);
    } catch (SQLException sqle) {
      throw new DbMigratorException(sqle);
    } catch (RuntimeException re) {
      throw new DbMigratorException(re);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Migrates the database.
   *
   * @throws DbMigratorException
   *           if there are initialization problems.
   */
  private void migrate() throws DbMigratorException {
    final String DEBUG_HEADER = "migrate(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    Connection sourceConn = null;
    Connection targetConn = null;

    try {
      // Get a connection to the source database.
      sourceConn = sourceDbManagerSql.getConnection(sourceDataSource);

      int dbVersion = -1;

      try {
	dbVersion = getDatabaseVersion(sourceConn, false);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "dbVersion = " + dbVersion);
      } finally {
	DbManagerSql.rollback(sourceConn, log);
      }

      // Check whether the source database needs to be updated before migrating
      // it.
      if (dbVersion < 5) {
        // Yes: Report the problem.
        throw new DbMigratorException("Database needs to be updated to at "
    	  + "least version 5 before migration");
      } else {
	log.info("Source database version = " + dbVersion);
      }

      // Get a connection to the target database.
      targetConn = targetDbManagerSql.getConnection(targetDataSource);

      boolean created = false;

      // Check whether the first execution of the migration process has not been
      // completed successfully yet.
      boolean isFirstExecution =
	  getDatabaseVersion(targetConn, true) != dbVersion;
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "isFirstExecution = " + isFirstExecution);

      if (isFirstExecution) {
	// Yes: Create the sequence number translation table in the target
	// database, if necessary.
	try {
	  created = createTableIfNeeded(targetConn, SEQ_TRANSLATION_TABLE,
	      CREATE_SEQ_TRANSLATION_TABLE_QUERY);
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "created = " + created);
	} finally {
	  DbManagerSql.commitOrRollback(targetConn, log);
	}
      }

      // Extract the source database metadata.
      Map<String, DbTable> tableMap =
	  extractDbMetadata(sourceConn, sourceUserName);

      // Populate the additional metadata.
      tableMap.get(PENDING_AU_TABLE)
      .setCreateQuery(DbManagerSql.CREATE_PENDING_AU_TABLE_QUERY);

      tableMap.get(COUNTER_REQUEST_TABLE)
      .setCreateQuery(DbManagerSql.REQUEST_TABLE_CREATE_QUERY);
      
      tableMap.get(COUNTER_REQUEST_TABLE).setRepeatedRowsAllowed(true);

      tableMap.get(UNCONFIGURED_AU_TABLE)
      .setCreateQuery(DbManagerSql.CREATE_UNCONFIGURED_AU_TABLE_QUERY);

      tableMap.get(AU_PROBLEM_TABLE)
      .setCreateQuery(DbManagerSql.CREATE_AU_PROBLEM_TABLE_QUERY);

      tableMap.get(PLATFORM_TABLE)
      .setCreateQuery(DbManagerSql.CREATE_PLATFORM_TABLE_QUERY);

      tableMap.get(PLUGIN_TABLE).setCreateQuery(CREATE_PLUGIN_TABLE_QUERY);

      tableMap.get(AU_TABLE).setCreateQuery(DbManagerSql.CREATE_AU_TABLE_QUERY);

      tableMap.get(AU_MD_TABLE)
      .setCreateQuery(DbManagerSql.CREATE_AU_MD_TABLE_QUERY);

      tableMap.get(MD_ITEM_TYPE_TABLE)
      .setCreateQuery(DbManagerSql.CREATE_MD_ITEM_TYPE_TABLE_QUERY);

      tableMap.get(MD_ITEM_TABLE)
      .setCreateQuery(DbManagerSql.CREATE_MD_ITEM_TABLE_QUERY);

      tableMap.get(MD_ITEM_NAME_TABLE)
      .setCreateQuery(DbManagerSql.CREATE_MD_ITEM_NAME_TABLE_QUERY);

      tableMap.get(MD_KEY_TABLE)
      .setCreateQuery(DbManagerSql.CREATE_MD_KEY_TABLE_QUERY);

      tableMap.get(MD_TABLE).setCreateQuery(DbManagerSql.CREATE_MD_TABLE_QUERY);

      tableMap.get(BIB_ITEM_TABLE)
      .setCreateQuery(DbManagerSql.CREATE_BIB_ITEM_TABLE_QUERY);

      tableMap.get(URL_TABLE)
      .setCreateQuery(DbManagerSql.CREATE_URL_TABLE_QUERY);

      tableMap.get(AUTHOR_TABLE)
      .setCreateQuery(DbManagerSql.CREATE_AUTHOR_TABLE_QUERY);

      tableMap.get(KEYWORD_TABLE)
      .setCreateQuery(DbManagerSql.CREATE_KEYWORD_TABLE_QUERY);

      tableMap.get(DOI_TABLE)
      .setCreateQuery(DbManagerSql.CREATE_DOI_TABLE_QUERY);

      tableMap.get(ISSN_TABLE)
      .setCreateQuery(DbManagerSql.CREATE_ISSN_TABLE_QUERY);

      tableMap.get(ISBN_TABLE)
      .setCreateQuery(DbManagerSql.CREATE_ISBN_TABLE_QUERY);

      tableMap.get(PUBLISHER_TABLE)
      .setCreateQuery(DbManagerSql.CREATE_PUBLISHER_TABLE_QUERY);

      tableMap.get(PUBLICATION_TABLE)
      .setCreateQuery(DbManagerSql.CREATE_PUBLICATION_TABLE_QUERY);

      tableMap.get(COUNTER_BOOK_TYPE_AGGREGATES_TABLE)
      .setCreateQuery(DbManagerSql.BOOK_TYPE_AGGREGATES_TABLE_CREATE_QUERY);

      tableMap.get(COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE)
      .setCreateQuery(DbManagerSql.JOURNAL_TYPE_AGGREGATES_TABLE_CREATE_QUERY);

      tableMap.get(COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE)
      .setCreateQuery(DbManagerSql
	  .JOURNAL_PUBYEAR_AGGREGATE_TABLE_CREATE_QUERY);

      tableMap.get(SUBSCRIPTION_TABLE)
      .setCreateQuery(DbManagerSql.CREATE_SUBSCRIPTION_TABLE_QUERY);

      tableMap.get(SUBSCRIPTION_RANGE_TABLE)
      .setCreateQuery(DbManagerSql.CREATE_SUBSCRIPTION_RANGE_TABLE_QUERY);

      tableMap.get(VERSION_TABLE)
      .setCreateQuery(DbManagerSql.CREATE_VERSION_TABLE_QUERY);

      // Remove the table that must be the last to be migrated.
      DbTable lastTable = tableMap.get(VERSION_TABLE);
      tableMap.remove(VERSION_TABLE);

      // Remember the count of tables.
      int originalCount = tableMap.size();
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "originalCount = " + originalCount);

      // Sort the tables to migrate those that depend on others through foreign
      // keys after the tables that they depend on.
      List<DbTable> sortedTables = sortTablesByFkDependencies(tableMap);

      // Sanity check.
      if (originalCount != sortedTables.size()) {
        throw new DbMigratorException("Resulting count of sorted tables = "
            + sortedTables.size()
            + " does not match original count of metadata tables = "
            + originalCount);
      }

      // Loop through the tables to be migrated.
      for (DbTable sortedTable : sortedTables) {
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Migrating table '"
	    + sortedTable.getName() + "'...");

	// Migrate the table.
	migrateTable(sourceConn, targetConn, sortedTable);

	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Done migrating table '"
	    + sortedTable.getName() + "'.");
      }

      // Check whether this is the first execution of the migration process.
      if (isFirstExecution) {
	// Yes: Add the indices for version 3.
	targetDbManagerSql.updateDatabaseFrom2To3(targetConn);

	// Add the indices for version 4.
	targetDbManagerSql.createVersion4Indices(targetConn);

	// Add the indices for version 5.
	targetDbManagerSql.createVersion5Indices(targetConn);

	// Check whether the database version is at least 6.
	if (dbVersion >= 6) {
	  // Yes: Add the indices for version 6.
	  targetDbManagerSql.createVersion6Indices(targetConn);

	  // Check whether the database version is at least 7.
	  if (dbVersion >= 7) {
	    // Yes: Add the indices for version 7.
	    targetDbManagerSql.updateDatabaseFrom6To7(targetConn);
	  }
	}
      }

      // Migrate the last table.
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Migrating table '"
	  + lastTable.getName() + "'...");

      migrateTable(sourceConn, targetConn, lastTable);

      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Done migrating table '"
	  + lastTable.getName() + "'.");

      // Check whether this is not the first execution of the migration process.
      if (!isFirstExecution) {
	// Yes: Delete the sequence number translation table from the target
	// database.
	removeTargetTableIfPresent(targetConn, SEQ_TRANSLATION_TABLE);
	DbManagerSql.commitOrRollback(targetConn, log);
      }
    } catch (DbMigratorException dbme) {
      throw dbme;
    } catch (SQLException sqle) {
      throw new DbMigratorException(sqle);
    } catch (RuntimeException re) {
      throw new DbMigratorException(re);
    } finally {
      DbManagerSql.safeCloseConnection(sourceConn);
      DbManagerSql.safeCloseConnection(targetConn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Get the version of a database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param inTarget
   *          A boolean indicating whether the database is the target database.
   * @return an int with the database version.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private int getDatabaseVersion(Connection conn, boolean inTarget)
      throws DbMigratorException {
    final String DEBUG_HEADER = "getDatabaseVersion(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "inTarget = " + inTarget);

    if (conn == null) {
      throw new DbMigratorException("Null connection.");
    }

    int version = 0;

    // Check whether the version table exists in the database.
    if (tableExists(conn, VERSION_TABLE, inTarget)) {
      try {
	// Yes: Get the version from the version table in the database.
	if (inTarget) {
	  version = targetDbManagerSql.getHighestNumberedDatabaseVersion(conn);
	} else {
	  version = sourceDbManagerSql.getHighestNumberedDatabaseVersion(conn);
	}
      } catch (SQLException sqle) {
	throw new DbMigratorException(sqle);
      } catch (RuntimeException re) {
	throw new DbMigratorException(re);
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "version = " + version);
    return version;
  }

  /**
   * Creates a database table if it does not exist.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param tableName
   *          A String with the name of the table to create, if missing.
   * @param tableCreateSql
   *          A String with the SQL code used to create the table, if missing.
   * @return <code>true</code> if the table did not exist and it was created,
   *         <code>false</code> otherwise.
   * @throws DbMigratorException
   *           if any problem occurred creating the table.
   */
  private boolean createTableIfNeeded(Connection conn, String tableName,
      String tableCreateSql) throws DbMigratorException {
    final String DEBUG_HEADER = "createTableIfNeeded(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "tableName = '" + tableName + "'.");
      log.debug2(DEBUG_HEADER + "tableCreateSql = '" + tableCreateSql + "'.");
    }

    if (conn == null) {
      throw new DbMigratorException("Null connection.");
    }

    PreparedStatement statement = null;

    // Check whether the table needs to be created in the target database.
    if (!tableExists(conn, tableName, true)) {
      // Yes: Create it.
      try {
	statement = targetDbManagerSql.prepareStatement(conn,	
	    targetDbManagerSql.localizeCreateQuery(tableCreateSql));
	int count = targetDbManagerSql.executeUpdate(statement);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
      } catch (SQLException sqle) {
	throw new DbMigratorException(sqle);
      } catch (RuntimeException re) {
	throw new DbMigratorException(re);
      } finally {
	DbManagerSql.safeCloseStatement(statement);
      }

      if (log.isDebug2())
	log.debug2(DEBUG_HEADER + "Table '" + tableName + "' created.");

      try {
	targetDbManagerSql.logTableSchema(conn, tableName);
      } catch (SQLException sqle) {
	throw new DbMigratorException(sqle);
      } catch (RuntimeException re) {
	throw new DbMigratorException(re);
      }

      return true;
    } else {
      // No.
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Table '" + tableName
	  + "' exists - Not creating it.");
      return false;
    }
  }

  /**
   * Provides an indication of whether a table exists.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param tableName
   *          A String with name of the table to be checked.
   * @param inTarget
   *          A boolean indicating whether the table is to be searched in the
   *          target database.
   * @return <code>true</code> if the named table exists, <code>false</code>
   *         otherwise.
   * @throws DbMigratorException
   *           if any problem occurred.
   */
  private boolean tableExists(Connection conn, String tableName,
      boolean inTarget) throws DbMigratorException {
    final String DEBUG_HEADER = "tableExists(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "tableName = " + tableName);
      log.debug2(DEBUG_HEADER + "inTarget = " + inTarget);
    }

    if (conn == null) {
      throw new DbMigratorException("Null connection");
    }

    boolean result = false;
    ResultSet resultSet = null;

    try {
      // Get the database schema table data.
      if (!inTarget && sourceDbManagerSql.isTypeDerby()) {
	resultSet = DbManagerSql.getStandardTables(conn, null, sourceUserName,
	    tableName.toUpperCase());
      } else if (!inTarget && sourceDbManagerSql.isTypePostgresql()) {
	resultSet = DbManagerSql.getStandardTables(conn, null, sourceUserName,
	    tableName.toLowerCase());
      } else if (inTarget && targetDbManagerSql.isTypeDerby()) {
	resultSet = DbManagerSql.getStandardTables(conn, null, targetUserName,
	    tableName.toUpperCase());
      } else if (inTarget && targetDbManagerSql.isTypePostgresql()) {
	resultSet = DbManagerSql.getStandardTables(conn, null, targetUserName,
	    tableName.toLowerCase());
      }

      // Determine whether the table exists.
      result = resultSet.next();
    } catch (SQLException sqle) {
	throw new DbMigratorException("Cannot determine whether table '"
	    + tableName + "' exists", sqle);
    } catch (RuntimeException re) {
	throw new DbMigratorException("Cannot determine whether table '"
	    + tableName + "' exists", re);
    } finally {
      DbManagerSql.safeCloseResultSet(resultSet);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Extracts the metadata of the tables of a database schema.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param schema
   *          A String with the database schema.
   * 
   * @return a Map<String, DbTable> with the metadata of the tables.
   * @throws DbMigratorException
   *           if there are problems extracting the metadata.
   */
  private Map<String, DbTable> extractDbMetadata(Connection conn,
      String schema) throws DbMigratorException {
    final String DEBUG_HEADER = "populateDbMetadata(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "schema = " + schema);

    if (conn == null) {
      throw new DbMigratorException("Null connection");
    }

    Map<String, DbTable> tableMap = new HashMap<String, DbTable>();

    ResultSet tableResultSet = null;
    String tableName = null;
    ResultSet columnResultSet = null;
    ResultSet pkResultSet = null;
    ResultSet fkResultSet = null;

    try {
      DatabaseMetaData metadata = DbManagerSql.getMetadata(conn);

      // Get the database schema table data.
      tableResultSet = DbManagerSql.getStandardTables(conn, null, schema, null);

      // Loop through all the schema tables.
      while (tableResultSet.next()) {
	tableName = tableResultSet.getString("TABLE_NAME");
	log.debug2(DEBUG_HEADER + "TABLE_NAME = " + tableName);

	String tableType = tableResultSet.getString("TABLE_TYPE");
	log.debug2(DEBUG_HEADER + "TABLE_TYPE = " + tableType);
	log.debug2(DEBUG_HEADER + "");

	// Check that this is not a view, etc.
	if ("TABLE".equals(tableType)) {
	  // Yes: Get the table column metadata.
	  DbTable table = new DbTable(tableName.toLowerCase());
	  DbRow row = new DbRow(tableName.toLowerCase());
	  table.setRow(row);
	  List<DbColumn> columns = row.getColumns();
	  columnResultSet = metadata.getColumns(null, schema, tableName, null);

	  // Loop through each table column.
	  while (columnResultSet.next()) {
	    String columnName =
		columnResultSet.getString("COLUMN_NAME").toLowerCase();
	    log.debug2(DEBUG_HEADER + "columnName = '" + columnName + "'.");

	    int columnType = columnResultSet.getInt("DATA_TYPE");
	    log.debug2(DEBUG_HEADER + "columnType = '" + columnType + "'.");

	    int position = columnResultSet.getInt("ORDINAL_POSITION");
	    log.debug2(DEBUG_HEADER + "position = '" + position + "'.");

	    DbColumn column = new DbColumn(columnName, columnType, position);
	    columns.add(column);
	  }

	  // Remember any primary key the table may have.
	  pkResultSet = metadata.getPrimaryKeys(null, schema, tableName);

	  if (pkResultSet.next()) {
	    String pkColumnName =
		pkResultSet.getString("COLUMN_NAME").toLowerCase();
	    log.debug2(DEBUG_HEADER + "pkColumnName = '" + pkColumnName + "'.");

	    for (DbColumn column : columns) {
	      if (pkColumnName.equals(column.getName())) {
		column.setPk(true);
		break;
	      }
	    }
	  }

	  // Remember any foreign keys the table may have.
	  fkResultSet = metadata.getImportedKeys(null, schema, tableName);

	  while (fkResultSet.next()) {
	    String fkColumnName =
		fkResultSet.getString("FKCOLUMN_NAME").toLowerCase();
	    log.debug2(DEBUG_HEADER + "fkColumnName = '" + fkColumnName + "'.");

	    String fkTableName =
		fkResultSet.getString("PKTABLE_NAME").toLowerCase();
	    log.debug2(DEBUG_HEADER + "fkTableName = '" + fkTableName + "'.");

	    for (DbColumn column : columns) {
	      if (fkColumnName.equals(column.getName())) {
		column.setFkTable(fkTableName);
		break;
	      }
	    }
	  }

	  // Sort the columns by their ordinal position.
	  Collections.sort(columns);

	  if (log.isDebug3()) {
	    for (DbColumn column : columns) {
	      log.debug3(DEBUG_HEADER + "column = '" + column + "'.");
	    }
	  }

	  // Add  the table to the result.
	  tableMap.put(tableName.toLowerCase(), table);
	}
      }
    } catch (SQLException sqle) {
      String message = "Cannot populate DB metadata.";
      log.error(message);
      log.error("TABLE_NAME = " + tableName);
      throw new DbMigratorException(message, sqle);
    } catch (RuntimeException re) {
      String message = "Cannot populate DB metadata.";
      log.error(message);
      log.error("TABLE_NAME = " + tableName);
      throw new DbMigratorException(message, re);
    } finally {
      DbManagerSql.safeCloseResultSet(fkResultSet);
      DbManagerSql.safeCloseResultSet(pkResultSet);
      DbManagerSql.safeCloseResultSet(columnResultSet);
      DbManagerSql.safeCloseResultSet(tableResultSet);

      try {
	DbManagerSql.rollback(conn, log);
      } catch (SQLException sqle) {
	throw new DbMigratorException(sqle);
      } catch (RuntimeException re) {
	throw new DbMigratorException(re);
      }
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "tableMap.size() = " + tableMap.size());
    return tableMap;
  }

  /**
   * Sorts database tables so that tables that depend on other tables due to
   * foreign key constraints appear later than the tables on which they depend.
   * 
   * @param tableMap
   *          A Map<String, DbTable> with the metadata of the tables to be
   *          sorted.
   * 
   * @return a List<DbTable> with the sorted database tables.
   */
  private List<DbTable> sortTablesByFkDependencies (
      Map<String, DbTable> tableMap) {
    final String DEBUG_HEADER = "sortTablesByFkDependencies(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "tableMap.size() = " + tableMap.size());

    // Initialize the result.
    List<DbTable> sortedTables = new ArrayList<DbTable>();
    Set<String> sortedTableNames = new HashSet<String>();

    // Continue for as long as there are tables to be sorted.
    while (tableMap.size() > 0) {
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "tableMap.size() = " + tableMap.size());

      // Get the keys of the current metadata map.
      Set<String> tableKeys = new HashSet<String>();
      tableKeys.addAll(tableMap.keySet());
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "tableKeys.size() = " + tableKeys.size());

      // Loop through all the keys.
      for (String key : tableKeys) {
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "key = " + key);
	DbTable dbTable = tableMap.get(key);
	boolean migratable = true;

	// Handle a table with a foreign key constraint.
	if (dbTable.getRow().getFkTables().size() > 0) {
	  // Loop through all the table foreign key constraints.
	  for (String fkTable : dbTable.getRow().getFkTables()) {
	    if (log.isDebug3())
	      log.debug3(DEBUG_HEADER + "fkTable = " + fkTable);

	    // Do not migrate this table if the foreign key table has not been
	    // placed in the migration list already.
	    if (!sortedTableNames.contains(fkTable) && !key.equals(fkTable)) {
	      migratable = false;
	      break;
	    }
	  }
	}

	// Check whether this table can be placed in the migration list now.
	if (migratable) {
	  // Yes: Move it from the original list to the result list.
	  sortedTables.add(dbTable);
	  sortedTableNames.add(key);
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Table " + key
	      + " added to the sorted list.");
	  tableMap.remove(key);
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Table " + key
	      + " removed from the map.");
	}
      }
    }

    if (log.isDebug3()) {
      for (DbTable sortedTable : sortedTables) {
	log.debug3(DEBUG_HEADER + "sortedTable = " + sortedTable.getName());
      }
    }
  
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "sortedTables.size() = " + sortedTables.size());
    return sortedTables;
  }

  /**
   * Migrates a table.
   * 
   * @param sourceConn
   *          A Connection with the connection to the source database.
   * @param targetConn
   *          A Connection with the connection to the target database.
   * @param columns
   *          A DbColumn[] with the definition of the table columns.
   * @throws DbMigratorException
   *           if any problem occurred migrating the table.
   */
  private void migrateTable(Connection sourceConn, Connection targetConn,
      DbTable table) throws DbMigratorException {
    final String DEBUG_HEADER = "migrateTable(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    try {
      String tableName = table.getName();
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "tableName = '" + tableName + "'");

      String createTableQuery = table.getCreateQuery();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "createTableQuery = '"
	  + createTableQuery + "'");

      boolean created = true;

      // Check whether any existing data in the target table needs to be
      // deleted because the table may have multiple identical rows.
      if (table.isRepeatedRowsAllowed()) {
	// Yes: Remove the table if it does exist.
	removeTargetTableIfPresent(targetConn, tableName);

	// Create the target table.
	targetDbManagerSql.createTable(targetConn, tableName,
	    createTableQuery);
      } else {
	// No: Create the target table, if necessary.
	created = createTableIfNeeded(targetConn, tableName, createTableQuery);
      }

      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "created = " + created);

      // Check whether there may be data in the target table.
      if (!created && !tableExists(targetConn, SEQ_TRANSLATION_TABLE, true)) {
	// Validate that the entire table has been successfully migrated and
	// finish.
	long rowCount =
	    validateTableRowCount(sourceConn, targetConn, table.getRow());
	log.info("Table '" + tableName + "' successfully migrated - " + rowCount
	    + " rows.");

	if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
	return;
      }

      boolean hasPrimaryKey = false;
      int pkIndex = -1;
      int index = 0;

      DbRow row = table.getRow();
      DbColumn[] columns = row.getColumnsAsArray();

      // Find out whether the table has a primary key by looping on its columns.
      for (DbColumn column : columns) {
	// Check whether this column is the table primary key.
	if (column.isPk()) {
	  if (!sequenceTranslation.containsKey(tableName)) {
	    // Yes: Initialize the primary key sequence translator data in
	    // memory.
	    sequenceTranslation.put(tableName, new HashMap<Long, Long>());
	  }

	  // Populate the primary key sequence translator data in memory with
	  // the data in the database.
	  populateTargetSequenceTranslation(targetConn, tableName);

	  // Remember the primary key of this table.
	  hasPrimaryKey = true;
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "hasPrimaryKey = " + hasPrimaryKey);

	  pkIndex = index;
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pkIndex = " + pkIndex);
	  break;
	} else {
	  // No: Try the next column.
	  index++;
	}
      }

      // Determine whether any foreign key columns in this table that need
      // translation can be translated.
      boolean canTranslate = true;

      for (DbColumn column : columns) {
	if (column.getFkTable() != null) {
	  String fkTable = column.getFkTable().toLowerCase();
	  log.debug3(DEBUG_HEADER + "fkTable = '" + fkTable + "'.");

	  if (!sequenceTranslation.containsKey(fkTable)) {
	    canTranslate = false;
	    if (log.isDebug3())
	      log.debug3(DEBUG_HEADER + "canTranslate = " + canTranslate);
	    break;
	  }
	}
      }

      String readSourceQuery = row.getReadRowSql();
      PreparedStatement readSource = null;
      ResultSet sourceResultSet = null;

      try {
	// Get the rows from the source table.
	readSource = sourceDbManagerSql.prepareStatement(sourceConn,
	    readSourceQuery);
	sourceResultSet =
	    sourceDbManagerSql.executeQuery(readSource);

	// Loop through all the rows from the source table.
	while (sourceResultSet.next()) {
	  // Get the values of the various columns for this row.
	  for (DbColumn column : columns) {
	    column.getValue(sourceResultSet);
	    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Read source "
		+ column.getName() + " = '" + column.getValue() + "'.");
	  }

	  // Check whether the table has not been created and it has a primary
	  // key.
	  if (!created && hasPrimaryKey) {
	    // Yes: Check whether the row has already been migrated.
	    if (sequenceTranslation.get(tableName).containsKey(
		columns[pkIndex].getValue())) {
	      // Yes: Continue with the next row.
	      if (log.isDebug3())
		log.debug3(DEBUG_HEADER + "Translated PK found.");
	      continue;
	    }
	  }

	  // Check whether the row cannot be translated.
	  if (!canTranslate) {
	    // Yes: Continue with the next row.
	    continue;
	  }

	  boolean translated = false;

	  // Check whether the table already existed and it is possible to
	  // identify this row in it.
	  if (!created && !table.isRepeatedRowsAllowed()) {
	    for (DbColumn column : columns) {
	      if (column.getFkTable() != null) {
		// Translate this foreign key.
		Long translatedFk = sequenceTranslation
		    .get(column.getFkTable().toLowerCase())
		    .get(column.getValue());
		if (log.isDebug3()) log.debug3(DEBUG_HEADER + "FK conversion: "
		    + column.getValue() + " => " + translatedFk);
		column.setValue(translatedFk);
	      }
	    }

	    translated = true;

	    // Try to find this row in the existing target table.
	    long rowCount = countMatchingTargetRows(targetConn, table);
	    if (log.isDebug3())
	      log.debug3(DEBUG_HEADER + "rowCount = " + rowCount);

	    // Determine whether this row had already been migrated.
	    if (rowCount == 1) {
	      // Yes: Do nothing more with this row from the source table.
	      continue;
	    }
	  }

	  // Write the row to the target table.
	  String writeTargetQuery = row.getWriteRowSql();
	  PreparedStatement writeTarget = null;
	  ResultSet targetResultSet = null;

	  try {
	    // Handle a table with a primary key differently to be able to
	    // extract the generated primary key.
	    if (hasPrimaryKey) {
	      writeTarget =
		  targetDbManagerSql.prepareStatement(targetConn,
		  writeTargetQuery, Statement.RETURN_GENERATED_KEYS);
	    } else {
	      writeTarget =
		  targetDbManagerSql.prepareStatement(targetConn,
		  writeTargetQuery);
	    }

	    index = 1;

	    // Loop through all the columns in the table.
	    for (DbColumn column : columns) {
	      // Check whether this is a primary key.
	      if (column.isPk()) {
		// Yes: No parameter is set in this case, as the value is
		// generated.
		if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Skip write "
		    + "target parameter primary key '" + column.getName()
		    + "'.");
	      } else {
		// No: Check whether this is a foreign key.
		if (column.getFkTable() != null && !translated) {
		  // Yes: Translate this foreign key.
		  Long translatedFk = sequenceTranslation
		      .get(column.getFkTable().toLowerCase())
		      .get(column.getValue());
		  if (log.isDebug3()) log.debug3(DEBUG_HEADER
		      + "FK conversion: " + column.getValue() + " => "
		      + translatedFk);
		  column.setValue(translatedFk);
		}

		// Set the parameter for this column in the prepared statement.
		if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Set write "
		    + "target parameter " + index + " with '"
		    + column.getValue() + "'.");
		column.setParameter(writeTarget, index++);
	      }
	    }

	    // Write the row.
	    int addedCount =
		targetDbManagerSql.executeUpdate(writeTarget);
	    if (log.isDebug3())
	      log.debug3(DEBUG_HEADER + "addedCount = " + addedCount);

	    // Check whether this column is the primary key.
	    if (hasPrimaryKey) {
	      // Yes: Get the generated primary key.
	      targetResultSet = writeTarget.getGeneratedKeys();

	      if (!targetResultSet.next()) {
		throw new DbMigratorException("No primary key created.");
	      }

	      Long targetPkSeq = targetResultSet.getLong(1);
	      if (log.isDebug3())
		log.debug3(DEBUG_HEADER + "targetPkSeq = " + targetPkSeq);

	      // Save the translation of the primary key of this row.
	      saveSequenceTranslation(targetConn, tableName,
		  (Long)columns[pkIndex].getValue(), targetPkSeq);
	    }
	  } catch (SQLException sqle) {
	    String message = "Cannot write the target '" + tableName
		+ "' table";
	    log.error(message, sqle);
	    log.error("SQL = '" + writeTargetQuery + "'.");
	    for (DbColumn column : columns) {
	      log.error(column.getName() + " = '" + column.getValue() + "'.");
	    }
	    throw new DbMigratorException(message, sqle);
	  } catch (RuntimeException re) {
	    String message = "Cannot write the target '" + tableName
		+ "' table";
	    log.error(message, re);
	    log.error("SQL = '" + writeTargetQuery + "'.");
	    for (DbColumn column : columns) {
	      log.error(column.getName() + " = '" + column.getValue() + "'.");
	    }
	    throw new DbMigratorException(message, re);
	  } finally {
	    DbManagerSql.safeCloseStatement(writeTarget);
	  }
	}
      } catch (SQLException sqle) {
	String message = "Cannot read the source '" + tableName + "' table";
	log.error(message, sqle);
	log.error("SQL = '" + readSourceQuery + "'.");
	throw new DbMigratorException(message, sqle);
      } catch (RuntimeException re) {
	String message = "Cannot read the source '" + tableName + "' table";
	log.error(message, re);
	log.error("SQL = '" + readSourceQuery + "'.");
	throw new DbMigratorException(message, re);
      } finally {
	DbManagerSql.safeCloseResultSet(sourceResultSet);
	DbManagerSql.safeCloseStatement(readSource);
      }

      // Compare the rows in both tables.
      long rowCount = validateTableRowCount(sourceConn, targetConn, row);
      log.info("Table '" + tableName + "' successfully migrated - " + rowCount
	  + " rows.");
    } catch (SQLException sqle) {
      throw new DbMigratorException(sqle);
    } catch (RuntimeException re) {
      throw new DbMigratorException(re);
    } finally {
      try {
	DbManagerSql.commitOrRollback(targetConn, log);
	DbManagerSql.rollback(sourceConn, log);
      } catch (SQLException sqle) {
	throw new DbMigratorException(sqle);
      } catch (RuntimeException re) {
	throw new DbMigratorException(re);
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Deletes a target database table if it does exist.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param tableName
   *          A String with the name of the table to delete, if present.
   * @return <code>true</code> if the table did exist and it was removed,
   *         <code>false</code> otherwise.
   * @throws DbMigratorException
   *           if any problem occurred removing the table.
   */
  private boolean removeTargetTableIfPresent(Connection conn, String tableName)
      throws DbMigratorException {
    final String DEBUG_HEADER = "removeTargetTableIfPresent(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "tableName = '" + tableName + "'.");

    if (conn == null) {
      throw new DbMigratorException("Null connection.");
    }

    PreparedStatement statement = null;

    // Check whether the table needs to be removed.
    if (tableExists(conn, tableName, true)) {
      // Yes: Delete it.
      String sql = null;

      try {
	sql = DbManagerSql.dropTableQuery(tableName);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "sql = '" + sql + "'.");

	statement = targetDbManagerSql.prepareStatement(conn, sql);
	int count = targetDbManagerSql.executeUpdate(statement);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
      } catch (SQLException sqle) {
	String message = "Cannot drop table '" + tableName + "'";
	log.error(message, sqle);
	log.error("SQL = '" + sql + "'.");
	throw new DbMigratorException(sqle);
      } catch (RuntimeException re) {
	String message = "Cannot drop table '" + tableName + "'";
	log.error(message, re);
	log.error("SQL = '" + sql + "'.");
	throw new DbMigratorException(re);
      } finally {
	DbManagerSql.safeCloseStatement(statement);
      }

      if (log.isDebug2())
	log.debug2(DEBUG_HEADER + "Dropped table '" + tableName + "'.");
      return true;
    } else {
      // No.
      log.debug2(DEBUG_HEADER + "Table '" + tableName
	  + "' does not exist - Not dropping it.");
      return false;
    }
  }

  /**
   * Validates that the row counts in the original and migrated tables match.
   * 
   * @param sourceConn
   *          A Connection with the connection to the source database.
   * @param targetConn
   *          A Connection with the connection to the target database.
   * @param tableName
   *          A String with the name of the table to be validated.
   * @return a long with the matching row counts.
   * @throws DbMigratorException
   *           if the row counts in the original and migrated tables match or if
   *           any problem occurred obtaining the row counts.
   */
  private long validateTableRowCount(Connection sourceConn,
      Connection targetConn, DbRow row) throws DbMigratorException {
    final String DEBUG_HEADER = "compareTableRowCount(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Get the count of rows in the source database table.
    long sourceCount = countRows(sourceConn, row, false);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "sourceCount = " + sourceCount);

    // Get the count of rows in the target database table.
    long targetCount = countRows(targetConn, row, true);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "targetCount = " + targetCount);

    // Report the problem if both counts do not match.
    if (sourceCount != targetCount) {
      throw new DbMigratorException("The migrated row counts for table "
	  + row.getTableName() + " do not match: sourceCount = " + sourceCount
	  + ", targetCount = " + targetCount);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "rowCount = " + targetCount);
    return targetCount;
  }

  /**
   * Provides the number of rows in a table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param tableName
   *          A String with the name of the table with the rows to be counted.
   * @param inTarget
   *          A boolean indicating whether the table rows are to be counted in
   *          the target database.
   * @return a long with the number of rows in the table.
   * @throws DbMigratorException
   *           if any problem occurred counting the rows.
   */
  private long countRows(Connection conn, DbRow row, boolean inTarget)
      throws DbMigratorException {
    final String DEBUG_HEADER = "countRows(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new DbMigratorException("Null connection.");
    }

    long rowCount = -1;
    String sql = row.getRowCountSql();
    PreparedStatement statement = null;
    ResultSet resultSet = null;

    // Get the row count.
    try {
      if (inTarget) {
	statement = targetDbManagerSql.prepareStatement(conn, sql);
	resultSet = targetDbManagerSql.executeQuery(statement);
      } else {
	statement = sourceDbManagerSql.prepareStatement(conn, sql);
	resultSet = sourceDbManagerSql.executeQuery(statement);
      }
      resultSet.next();
      rowCount = resultSet.getLong(1);
    } catch (SQLException sqle) {
      log.error("Cannot get the count of rows", sqle);
      log.error("SQL = '" + sql + "'.");
      throw new DbMigratorException("Cannot get the count of rows", sqle);
    } catch (RuntimeException re) {
      log.error("Cannot get the count of rows", re);
      log.error("SQL = '" + sql + "'.");
      throw new DbMigratorException("Cannot get the count of rows", re);
    } finally {
      DbManagerSql.safeCloseResultSet(resultSet);
      DbManagerSql.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "rowCount = " + rowCount);
    return rowCount;
  }

  /**
   * Populates the sequence translation map for a table with values from the
   * database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param tableName
   *          A String with the name of the table.
   * @throws DbMigratorException
   *           if any problem occurred populating the sequence translation map.
   */
  private void populateTargetSequenceTranslation(Connection conn,
      String tableName) throws DbMigratorException {
    final String DEBUG_HEADER = "populateTargetSequenceTranslation(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "tableName = " + tableName);

    if (conn == null) {
      throw new DbMigratorException("Null connection.");
    }

    PreparedStatement statement = null;
    ResultSet resultSet = null;

    try {
      // Get the sequence translations for the table.
      statement = targetDbManagerSql.prepareStatement(conn,
	  FIND_TABLE_TRANSLATION_QUERY);
      statement.setString(1, tableName);

      resultSet = targetDbManagerSql.executeQuery(statement);

      // Loop through all the sequence translations for the table.
      while (resultSet.next()) {
	// Get the original sequence.
	Long sourceSeq = resultSet.getLong(SOURCE_SEQ_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "sourceSeq = " + sourceSeq);

	// Get the corresponding migrated sequence.
	Long targetSeq = resultSet.getLong(TARGET_SEQ_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "targetSeq = " + targetSeq);

	// Add the translation to the map.
	sequenceTranslation.get(tableName).put(sourceSeq, targetSeq);
      }
    } catch (SQLException sqle) {
      String message = "Cannot read the sequence translation table";
      log.error(message, sqle);
      log.error("SQL = '" + FIND_TABLE_TRANSLATION_QUERY + "'.");
      throw new DbMigratorException(message, sqle);
    } catch (RuntimeException re) {
      String message = "Cannot read the sequence translation table";
      log.error(message, re);
      log.error("SQL = '" + FIND_TABLE_TRANSLATION_QUERY + "'.");
      throw new DbMigratorException(message, re);
    } finally {
      DbManagerSql.safeCloseResultSet(resultSet);
      DbManagerSql.safeCloseStatement(statement);
    }
  }

  /**
   * Counts the number of rows in a database table that match a given table row.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param dbTable
   *          A DbTable with the table row to be matched.
   * 
   * @return a long with the number of matching rows.
   * @throws DbMigratorException
   *           if there are problems counting the rows.
   */
  private long countMatchingTargetRows(Connection conn, DbTable dbTable)
      throws DbMigratorException {
    final String DEBUG_HEADER = "countMatchingTargetRows(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "tableName = " + dbTable.getName());

    if (conn == null) {
      throw new DbMigratorException("Null connection.");
    }

    // Yes: Try to find this row in the existing target table.
    PreparedStatement readTarget = null;
    ResultSet targetResultSet = null;
    long rowCount = 0;

    String findTargetQuery = dbTable.getRow().getMatchingRowCountSql();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "findTargetQuery = '" + findTargetQuery + "'");

    try {
      readTarget = targetDbManagerSql.prepareStatement(conn,
	  findTargetQuery);

      int index = 1;
      for (DbColumn column : dbTable.getRow().getColumnsAsArray()) {
	if (column.getValue() != null) {
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "Set read target parameter " + index
		+ " to '" + column.getValue() + "'.");
	  column.setParameter(readTarget, index++);
	}
      }

      targetResultSet = targetDbManagerSql.executeQuery(readTarget);
      targetResultSet.next();
      rowCount = targetResultSet.getLong(1);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "rowCount = " + rowCount);
    } catch (SQLException sqle) {
      String message = "Cannot read the target '" + dbTable.getName()
	  + "' table";
      log.error(message, sqle);
      log.error("SQL = '" + findTargetQuery.toString() + "'.");
      for (DbColumn column : dbTable.getRow().getColumnsAsArray()) {
	log.error(column.getName() + " = '" + column.getValue() + "'.");
      }
      throw new DbMigratorException(message, sqle);
    } catch (RuntimeException re) {
      String message = "Cannot read the target '" + dbTable.getName()
	  + "' table";
      log.error(message, re);
      log.error("SQL = '" + findTargetQuery.toString() + "'.");
      for (DbColumn column : dbTable.getRow().getColumnsAsArray()) {
	log.error(column.getName() + " = '" + column.getValue() + "'.");
      }
      throw new DbMigratorException(message, re);
    } finally {
      DbManagerSql.safeCloseResultSet(targetResultSet);
      DbManagerSql.safeCloseStatement(readTarget);
    }

    return rowCount;
  }

  /**
   * Saves a sequence value translation.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param tableName
   *          A String with the name of table to which the sequence value
   *          belongs.
   * @param sourceSeq
   *          A Long with the source sequence value.
   * @param targetSeq
   *          A Long with the target sequence value.
   * @return an int with the number of rows inserted.
   * @throws DbMigratorException
   *           if any problem occurred adding the translation.
   */
  private void saveSequenceTranslation(Connection conn, String tableName,
      Long sourceSeq, Long targetSeq) throws DbMigratorException {
    final String DEBUG_HEADER = "saveSequenceTranslation(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "tableName = " + tableName);
      log.debug2(DEBUG_HEADER + "sourceSeq = " + sourceSeq);
      log.debug2(DEBUG_HEADER + "targetSeq = " + targetSeq);
    }

    if (conn == null) {
      throw new DbMigratorException("Null connection.");
    }

    // Add to the database the translation of the primary key of this row.
    addSequenceTranslation(conn, tableName, sourceSeq, targetSeq);

    // Save it in memory.
    sequenceTranslation.get(tableName).put(sourceSeq, targetSeq);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Adds to the database a sequence value translation.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param tableName
   *          A String with the name of table to which the sequence value
   *          belongs.
   * @param sourceSeq
   *          A Long with the source sequence value.
   * @param targetSeq
   *          A Long with the target sequence value.
   * @return an int with the number of rows inserted.
   * @throws DbMigratorException
   *           if any problem occurred adding the translation.
   */
  private int addSequenceTranslation(Connection conn, String tableName,
      Long sourceSeq, Long targetSeq) throws DbMigratorException {
    final String DEBUG_HEADER = "addSequenceTranslation(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "tableName = " + tableName);
      log.debug2(DEBUG_HEADER + "sourceSeq = " + sourceSeq);
      log.debug2(DEBUG_HEADER + "targetSeq = " + targetSeq);
    }

    if (conn == null) {
      throw new DbMigratorException("Null connection.");
    }

    PreparedStatement insert = null;
    int count = 0;

    try {
      // Add the row to the database.
      insert = targetDbManagerSql.prepareStatement(conn,
	  INSERT_SEQ_TRANSLATION_QUERY);

      insert.setString(1, tableName);
      insert.setLong(2, sourceSeq);
      insert.setLong(3, targetSeq);

      count = targetDbManagerSql.executeUpdate(insert);
    } catch (SQLException sqle) {
      String message = "Cannot add sequence translation";
	log.error(message, sqle);
	log.error("tableName = " + tableName);
	log.error("sourceSeq = " + sourceSeq);
	log.error("targetSeq = " + targetSeq);
	log.error("SQL = '" + INSERT_SEQ_TRANSLATION_QUERY + "'.");
	throw new DbMigratorException(message, sqle);
    } catch (RuntimeException re) {
      String message = "Cannot add sequence translation";
	log.error(message, re);
	log.error("tableName = " + tableName);
	log.error("sourceSeq = " + sourceSeq);
	log.error("targetSeq = " + targetSeq);
	log.error("SQL = '" + INSERT_SEQ_TRANSLATION_QUERY + "'.");
	throw new DbMigratorException(message, re);
    } finally {
      DbManagerSql.safeCloseStatement(insert);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "count = " + count);
    return count;
  }

  /**
   * Exception specific to database migration.
   * 
   * @version 1.0
   */
  @SuppressWarnings("serial")
  private class DbMigratorException extends Exception {
    private DbMigratorException(String message) {
      super(message);
    }

    private DbMigratorException(Throwable cause) {
      super(cause);
    }

    private DbMigratorException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
