/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.servlet;

import org.apache.commons.text.StringEscapeUtils;
import org.lockss.util.RandomUtil;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.db.DataSourceUtil;
import org.lockss.db.DbManager;
import org.lockss.db.DbManagerSql;
import org.lockss.laaws.MigrationManager;
import org.lockss.laaws.V2AuMover;
import org.lockss.protocol.*;
import org.lockss.protocol.LcapRouter;
import org.lockss.proxy.ProxyManager;
import org.lockss.repository.RepositoryManager;
import org.lockss.util.*;
import org.mortbay.html.*;
import org.mortbay.http.HttpRequest;

import javax.servlet.ServletException;
import java.io.*;
import java.net.*;
import java.util.*;

import static org.lockss.laaws.MigrationConstants.*;

/**
 * LOCKSS servlet that accepts migration settings from the user and
 * can enter the daemon into migration mode.
 */
public class MigrateSettings extends LockssServlet {
  static Logger log = Logger.getLogger(MigrateSettings.class);

  static final String RESET_CONFIRMATION_MSG = "If migration is resumed it will start over from the beginning.  %sDo you want to proceed?";
  static final String RESET_ALREADY_DELETED = "Any AUs already deleted after migration will not be included.  ";

  static final String DEFAULT_HOSTNAME = "localhost";
  static final String KEY_INITIALIZED_FROM_CONFIG = "initializedFromConfig";
  static final String KEY_HOSTNAME = "hostname";
  static final String KEY_CFGSVC_UI_PORT = "cfgUiPort";
  static final String KEY_V2_USERNAME = "v2Username";
  static final String KEY_V2_PASSWORD = "v2Password";
  static final String KEY_DATABASE_PASS = "dbPassword";
  static final String KEY_FETCHED_V2_CONFIG = "isTargetConfigFetched";
  static final String KEY_MIGRATION_CONFIG = "mCfg";

  static final String KEY_DELETED_AUS_DIR = "deletedAusDir";
  static final String KEY_DELETE_AUS_INTERVAL = "deleteAusInterval";
  static final String KEY_DRY_RUN_ENABLED = "dryRunEnabled";
  static final String KEY_DELETE_AUS = "enableDeleteAus";

  static final String KEY_ACTION = "action";
  static final String ACTION_LOAD_V2_CFG = "Load Configuration";
  static final String ACTION_NEXT = "Next";
  static final String ACTION_RESET_MIGRATION_STATE = "Reset Migration State";

  static final String HEADING_TARGET = "LOCKSS 2.x Target";
  static final String HEADING_DATABASE = "Metadata Database";
  static final String HEADING_OPTIONS = "Migration Options";

  static final String LABEL_TARGET_HOSTNAME = "Hostname";
  static final String LABEL_TARGET_PORT = "Configuration Service Web UI Port";
  static final String LABEL_TARGET_USERNAME = "Web UI Username";
  static final String LABEL_TARGET_PASSWORD = "Web UI Password";
  
  static final String LABEL_DATABASE_HOSTNAME = "Database Hostname";
  static final String LABEL_DATABASE_PORT = "Database Port";
  static final String LABEL_DATABASE_TYPE = "Database Type";
  static final String LABEL_DATABASE_NAME = "Database Name";
  static final String LABEL_DATABASE_USER = "Database User";
  static final String LABEL_DATABASE_PASSWORD = "Database Password";
  
  static final String LABEL_DRY_RUN = "Perform dry run migration";
  static final String LABEL_DELETE_EACH_AU = "Delete each AU after migration";

  static final String POPUP_DELETE_EACH_AU =
      "The \"" + LABEL_DELETE_EACH_AU + "\" option causes content to be " +
      "permanently deleted from LOCKSS 1.x as the migration progresses. It " +
      "should only be used in a same-host migration with insufficient " +
      "storage space to hold two copies of the content. See \"Same-Host " +
      "Migration With Incremental Reclamation\" in the LOCKSS 1.x to 2.x " +
      "Migration Guide.";
  
  LockssDaemon theDaemon;
  MigrationManager migrationMgr;

  String hostname;
  int cfgUiPort;
  String userName;
  String userPass;
  Configuration mCfg;
  String dbPass;

  boolean dryRunEnabled;
  boolean isDeleteAusEnabled;
  boolean isTargetConfigFetched;
  boolean isMigrationConfigured;

  String deletedAusDir = "MIGRATED";
  String deleteAusInterval = "1h";

  String fetchError;
  String dbError;
  String requestMethod;

  /**
   * Constructor.
   */
  public MigrateSettings() {
    theDaemon = LockssDaemon.getLockssDaemon();
    migrationMgr = theDaemon.getMigrationManager();
  }

  /**
   * Initializes fields for this servlet invocation.
   */
  private void initParams() throws IOException {
    requestMethod = req.getMethod();
    session = getSession();
    Configuration cfg = ConfigManager.getCurrentConfig();

    isMigrationConfigured = cfg.getBoolean(
        MigrationManager.PARAM_IS_MIGRATOR_CONFIGURED,
        MigrationManager.DEFAULT_IS_MIGRATOR_CONFIGURED);

    boolean isInitializedFromConfig =
        getSessionAttribute(KEY_INITIALIZED_FROM_CONFIG, false);

    if (!isInitializedFromConfig && isMigrationConfigured) {
      initParamsFromConfig(cfg);
      session.setAttribute(KEY_INITIALIZED_FROM_CONFIG, true);
      session.setAttribute(KEY_FETCHED_V2_CONFIG, true);
    } else {
      initParamsFromSession();
    }

    // Internal state
    isTargetConfigFetched = getSessionAttribute(KEY_FETCHED_V2_CONFIG, false);
  }

  private void initParamsFromConfig(Configuration cfg) throws IOException {
    // Migration target
    hostname = cfg.get(MigrateContent.PARAM_HOSTNAME);
    cfgUiPort = cfg.getInt(V2AuMover.PARAM_CFG_UI_PORT, V2AuMover.DEFAULT_CFG_UI_PORT);
    userName = cfg.get(MigrateContent.PARAM_USERNAME);
    userPass = cfg.get(MigrateContent.PARAM_PASSWORD);

    // Migration options
    dryRunEnabled = cfg.getBoolean(
        MigrationManager.PARAM_DRY_RUN_ENABLED,
        MigrationManager.DEFAULT_DRY_RUN_ENABLED);

    isDeleteAusEnabled = cfg.getBoolean(
        MigrateContent.PARAM_DELETE_AFTER_MIGRATION,
        MigrateContent.DEFAULT_DELETE_AFTER_MIGRATION);

    // Migration configuration
    ConfigManager cfgMgr = ConfigManager.getConfigManager();
    mCfg = cfgMgr.readCacheConfigFile(ConfigManager.CONFIG_FILE_MIGRATION).copy();
    isTargetConfigFetched = mCfg.containsKey(MigrationManager.PARAM_IS_MIGRATOR_CONFIGURED);
    dbPass = mCfg.get(V2_DOT + DbManager.PARAM_DATASOURCE_PASSWORD);
  }

  private void initParamsFromSession() {
    // Migration target
    hostname = getSessionAttribute(KEY_HOSTNAME, DEFAULT_HOSTNAME);
    cfgUiPort = getSessionAttribute(KEY_CFGSVC_UI_PORT, V2_DEFAULT_CFGSVC_UI_PORT);
    userName = getSessionAttribute(KEY_V2_USERNAME, null);
    userPass = getSessionAttribute(KEY_V2_PASSWORD, null);

    // Database
    dbPass = getSessionAttribute(KEY_DATABASE_PASS, null);

    // Migration options
    dryRunEnabled = getSessionAttribute(
        KEY_DRY_RUN_ENABLED, MigrationManager.DEFAULT_DRY_RUN_ENABLED);

    isDeleteAusEnabled = getSessionAttribute(
        KEY_DELETE_AUS, MigrateContent.DEFAULT_DELETE_AFTER_MIGRATION);

    // Migration configuration
    mCfg = getSessionAttribute(KEY_MIGRATION_CONFIG, ConfigManager.newConfiguration());
  }

  private void initParamsFromFormData() {
    // Migration target
    hostname = getParameter(KEY_HOSTNAME);
    String portStr = getParameter(KEY_CFGSVC_UI_PORT);
    if (StringUtil.isNullString(portStr)) {
      throw new IllegalArgumentException("Missing configuration service port");
    }
    try {
      cfgUiPort = Integer.parseInt(portStr);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid configuration service port");
    }
    userName = getParameter(KEY_V2_USERNAME);
    userPass = getParameter(KEY_V2_PASSWORD);

    // Database
    dbPass = getParameter(KEY_DATABASE_PASS);

    // Migration configuration
    dryRunEnabled = hasParameter(KEY_DRY_RUN_ENABLED);
    isDeleteAusEnabled = hasParameter(KEY_DELETE_AUS);
  }

  private void checkFetchConfigParams() {
    if (StringUtil.isNullString(hostname)) {
      throw new IllegalArgumentException("Missing hostname");
    }
    if (StringUtil.isNullString(userName)) {
      throw new IllegalArgumentException("Missing username");
    }
    if (StringUtil.isNullString(userPass)) {
      throw new IllegalArgumentException("Missing password");
    }
    if (cfgUiPort <= 0) {
      throw new IllegalArgumentException("Missing or illegal target configuration port");
    }
  }

  /**
   * Returns an HTTP session attribute, or the provided default if the attribute
   * doesn't exist.
   *
   * @param key A {@link String} containing the attribute name.
   * @param defaultVal The default value to return if the attribute doesn't exist
   *                   in the session.
   * @return The session attribute or default value if not found.
   * @param <T> The type of the session attribute.
   */
  private <T> T getSessionAttribute(String key, T defaultVal) {
    Object v = session.getAttribute(key);
    return v == null ? defaultVal : (T) v;
  }

  /**
   * Returns a boolean indicating whether a parameter exists in the request's
   * URL-encoded form or query arguments.
   *
   * @param key A {@link String} containing the parameter name or key.
   * @return A boolean indicating the presence of the parameter in the request.
   */
  private boolean hasParameter(String key) {
    return req.getParameterMap().containsKey(key);
  }

  /**
   * Resets the local fields after a servlet invocation.
   */
  @Override
  protected void resetLocals() {
    super.resetLocals();
    mCfg = null;

    hostname = null;
    cfgUiPort = V2_DEFAULT_CFGSVC_UI_PORT;
    userName = null;
    userPass = null;

    dbPass = null;
    isDeleteAusEnabled = MigrateContent.DEFAULT_DELETE_AFTER_MIGRATION;
    isTargetConfigFetched = false;

//    fetchError = null;
    dbError = null;
    requestMethod = null;
  }

  private void sanityCheckTargetConfig(Configuration targetCfg)
      throws IOException {
    if (StringUtil.isNullString(targetCfg.get("org.lockss.app.serviceBindings"))) {
      throw new IOException("Target config fails sanity check");
    }
  }

  /**
   * Request handler for this {@link LockssServlet}.
   */
  @Override
  protected void lockssHandleRequest() throws ServletException, IOException {
    initParams();

    if (requestMethod.equals(HttpRequest.__POST)) {
      String action = getParameter(KEY_ACTION);
      if (StringUtil.isNullString(action)) {
        action = getParameter(ACTION_TAG);
      }
      switch (action) {
        case ACTION_LOAD_V2_CFG:
          try {
            initParamsFromFormData();
            checkFetchConfigParams();
            Configuration targetCfg =
              migrationMgr.getConfigFromMigrationTarget(hostname, cfgUiPort,
                                                        userName, userPass);
            sanityCheckTargetConfig(targetCfg);
            mCfg = getMigrationConfig(hostname, targetCfg);
            isTargetConfigFetched = true;
            fetchError = null;
          } catch (IOException | IllegalArgumentException e) {
            fetchError = "Could not fetch migration target configuration: " +
              e.getMessage();
            log.error(fetchError, e);
            session.removeAttribute(KEY_FETCHED_V2_CONFIG);
            isTargetConfigFetched = false;
          } catch (MigrationManager.UnsupportedConfigurationException e) {
            fetchError = "Unsupported configuration: " + e.getMessage();
            log.error(fetchError, e);
            session.removeAttribute(KEY_FETCHED_V2_CONFIG);
            isTargetConfigFetched = false;
          } finally {
            if (fetchError != null) {
              mCfg = mCfg.copy();
              mCfg.removeConfigTree(V2_PREFIX);
            }
            migrationMgr.setInMigrationMode(false);
            dbPass = null;
          }

          break;

        case ACTION_NEXT:
          if (migrationMgr.isInMigrationMode()) {
            redirectToMigrateContent();
          } else {
            initParamsFromFormData();
            if (!isTargetConfigFetched) {
              errMsg = "Missing database configuration";
            } else if (DbManagerSql.isTypeDerby(mCfg.get(DbManager.PARAM_DATASOURCE_CLASSNAME))) {
              dbError = "Derby not supported";
            } else if (StringUtil.isNullString(dbPass)) {
              dbError = "Missing database password";
            } else if (!dryRunEnabled &&
                       !migrationMgr.isTargetInMigrationMode(hostname, cfgUiPort,
                                                             userName, userPass)) {
              errMsg = "Non-dry run migration cannot be performed when target is not in migration mode";
            } else if (dryRunEnabled &&
                       migrationMgr.isTargetInMigrationMode(hostname, cfgUiPort,
                                                            userName, userPass)) {
              errMsg = "Dry run migration cannot be performed when target is in migration mode";
            } else if (!migrationMgr.isInMigrationMode()) {
              // Populate remaining migration configuration parameters
              mCfg.put(V2_DOT + DbManager.PARAM_DATASOURCE_PASSWORD, dbPass);
              mCfg.put(MigrationManager.PARAM_DRY_RUN_ENABLED,
                       String.valueOf(dryRunEnabled));
              mCfg.put(MigrateContent.PARAM_DELETE_AFTER_MIGRATION, String.valueOf(isDeleteAusEnabled));

              if (isDeleteAusEnabled) {
                // Migration options (provided by user input)
                mCfg.put(V2_DOT + RepositoryManager.PARAM_MOVE_DELETED_AUS_TO, "MIGRATED");
                mCfg.put(V2_DOT + RepositoryManager.PARAM_DELETEAUS_INTERVAL,
                         String.valueOf(RepositoryManager.DEFAULT_DELETEAUS_INTERVAL));
              }

              // Test database connection
              if (!migrationMgr.isSkipDbCopy()) {
                try {
                  Configuration dsCfg = DataSourceUtil.getRuntimeDataSourceConfig(mCfg.getConfigTree(V2_PREFIX));
                  DataSourceUtil.validateDataSourceConfig(dsCfg);
                } catch (Throwable e) {
                  dbError = "Could not connect to database";
                  log.error(dbError, e);
                  break;
                }
              }
              // Write migration configuration to file
              migrationMgr.writeMigrationConfigFile(mCfg);

              // If the DB datasource has changed (now points to V2),
              // restart DbManager
              Configuration cfg = ConfigManager.getCurrentConfig();
              Configuration dsBefore =
                cfg.getConfigTree(DbManager.DATASOURCE_ROOT);
              ConfigManager.getConfigManager().reloadAndWait();
              cfg = ConfigManager.getCurrentConfig();
              Configuration dsAfter =
                cfg.getConfigTree(DbManager.DATASOURCE_ROOT);
              if (!java.util.Objects.equals(dsBefore, dsAfter)) {
                DbManager dbMgr = theDaemon.getDbManager();
                dbMgr.restartService();
              }
              redirectToMigrateContent();
            }
          }
          break;
        case ACTION_RESET_MIGRATION_STATE:
          try {
            migrationMgr.resetAllMigrationState();
            statusMsg = "All AUs and databases reset to \"not migrated\" state";
          } catch (IllegalStateException e) {
            errMsg = e.getMessage();
          } catch (IOException e) {
            errMsg = "An error occurred while resetting migration state: " + e;
          }
          break;
        default:
          errMsg = "Unknown action: " + action;
      }
    }

    displayPage();

    // Remember state
    session.setAttribute(KEY_FETCHED_V2_CONFIG, isTargetConfigFetched);
    session.setAttribute(KEY_HOSTNAME, hostname);
    session.setAttribute(KEY_CFGSVC_UI_PORT, cfgUiPort);
    session.setAttribute(KEY_V2_USERNAME, userName);
    session.setAttribute(KEY_V2_PASSWORD, userPass);
    session.setAttribute(KEY_DRY_RUN_ENABLED, dryRunEnabled);
    session.setAttribute(KEY_DELETE_AUS, isDeleteAusEnabled);
    session.setAttribute(KEY_MIGRATION_CONFIG, mCfg);
  }

  private void redirectToMigrateContent() throws IOException {
    // Redirect to Migrate Content page
    String redir = srvURL(AdminServletManager.SERVLET_MIGRATE_CONTENT);
    resp.sendRedirect(redir);
  }

  private void displayPage() throws IOException {
    Page page = newPage();
    addJavaScript(page);
    addCssLocations(page);
    layoutErrorBlock(page);
    ServletUtil.layoutExplanationBlock(page, "");
    page.add(migrationMgr.isRealMigrationMode() ?
        makeDisplayCfg() : makeForm());
    endPage(page);
  }

  private static final String CENTERED_CELL = "align=\"center\" colspan=3";
  private static final String ERRORBLOCK_ERROR_AFTER =
      "</font></center><br>";
  private static final String ERRORBLOCK_ERROR_BEFORE =
      "<center><font color=\"red\" size=\"+1\">";

  private Element makeForm() {
    Composite comp = new Composite();
    Form frm = new Form(srvURL(myServletDescr()));
    frm.method("POST");
    frm.add(new Input(Input.Hidden, ACTION_TAG));
    Table tbl = new Table(0, "align=center cellspacing=2 cellpadding=0");

    addSection(tbl, HEADING_TARGET);

    addInputToTable(tbl,
        LABEL_TARGET_HOSTNAME,
        KEY_HOSTNAME, hostname, 40);
    addInputToTable(tbl,
        LABEL_TARGET_PORT,
        KEY_CFGSVC_UI_PORT, String.valueOf(cfgUiPort), 20);
    addInputToTable(tbl,
        LABEL_TARGET_USERNAME,
        KEY_V2_USERNAME, userName, 20);
    addHiddenInputToTable(tbl,
        LABEL_TARGET_PASSWORD,
        KEY_V2_PASSWORD, userPass, 20);

    tbl.newRow();
    tbl.newCell(CENTERED_CELL);
    tbl.add(submitButton(ACTION_LOAD_V2_CFG, ACTION_LOAD_V2_CFG));

    if (!StringUtil.isNullString(fetchError)) {
      displayInputError(tbl, fetchError);
    }

    addSubsection(tbl, HEADING_DATABASE);

    Configuration v2cfg = mCfg.getConfigTree(V2_PREFIX);

    addFieldToTable(tbl, LABEL_DATABASE_HOSTNAME, v2cfg.get(DbManager.PARAM_DATASOURCE_SERVERNAME));
    addFieldToTable(tbl, LABEL_DATABASE_PORT, v2cfg.get(DbManager.PARAM_DATASOURCE_PORTNUMBER));
    addFieldToTable(tbl, LABEL_DATABASE_TYPE,
        getHumanReadableDatabaseType(v2cfg.get(DbManager.PARAM_DATASOURCE_CLASSNAME)));
    addFieldToTable(tbl, LABEL_DATABASE_NAME,
        getShortenedDatabaseName(v2cfg.get(DbManager.PARAM_DATASOURCE_DATABASENAME)));
    addFieldToTable(tbl, LABEL_DATABASE_USER, v2cfg.get(DbManager.PARAM_DATASOURCE_USER));

    // Only prompt for a database password if we've fetched its other details
    if (isTargetConfigFetched) {
      addHiddenInputToTable(tbl,
          LABEL_DATABASE_PASSWORD,
          KEY_DATABASE_PASS, v2cfg.get(DbManager.PARAM_DATASOURCE_PASSWORD, dbPass), 20);
    } else {
      addFieldToTable(tbl, LABEL_DATABASE_PASSWORD, v2cfg.get(DbManager.PARAM_DATASOURCE_PASSWORD));
    }

    if (!StringUtil.isNullString(dbError)) {
      displayInputError(tbl, dbError);
    }

    addSection(tbl, HEADING_OPTIONS);

    Input dryRunCheckbox = addCheckboxToTable(tbl, LABEL_DRY_RUN,
        KEY_DRY_RUN_ENABLED, dryRunEnabled);

    Input deleteAfterCheckbox = addCheckboxToTable(tbl, LABEL_DELETE_EACH_AU,
        KEY_DELETE_AUS, isDeleteAusEnabled);
    deleteAfterCheckbox.attribute("onclick", "if(this.checked){alert(\"" + StringEscapeUtils.escapeEcmaScript(POPUP_DELETE_EACH_AU) + "\")}");

    // Do not allow user to select dry run once migration has started
    if (migrationMgr.isRealMigrationMode()) {
      dryRunCheckbox.attribute("disabled");
    }
    if (dryRunEnabled) {
      deleteAfterCheckbox.attribute("disabled");
    }

    setupToggleGroup(dryRunCheckbox, deleteAfterCheckbox);

    // Advanced migration options - only in debug mode
    if (migrationMgr.isMigrationInDebugMode()) {
      addInputToTable(tbl,
          "Move Deleted AUs To",
          KEY_DELETED_AUS_DIR, deletedAusDir, 20);
      addInputToTable(tbl,
          "Delete AUs interval",
          KEY_DELETE_AUS_INTERVAL, deleteAusInterval, 20);
    }

    addCommonFormElements(tbl);
    frm.add(tbl);
    comp.add(frm);
    return comp;
  }

  private void addCommonFormElements(Table tbl) {
    tbl.newRow();
    tbl.newCell(CENTERED_CELL);
    tbl.add(new Break());
    tbl.add(new Break(Break.Rule));
    tbl.add(new Break());

    Input nextButton = new Input(Input.Submit, KEY_ACTION, ACTION_NEXT);
    if (!isTargetConfigFetched) {
      nextButton.attribute("disabled");
    }
    tbl.add(nextButton);
    tbl.add("&nbsp;");
    if (migrationMgr.isMigrationInDebugMode()) {
      Input resetMigrationStateButton = new Input(Input.Submit, KEY_ACTION,
                                                  ACTION_RESET_MIGRATION_STATE);
      String confMsg = String.format(RESET_CONFIRMATION_MSG,
                                     (isDeleteAusEnabled ? RESET_ALREADY_DELETED : ""));
      resetMigrationStateButton.attribute("onclick", "return confirm(\"" +
                                          confMsg + "\");");
      tbl.add(resetMigrationStateButton);
    }
  }

  private void setupToggleGroup(Input toggleElem, Input... groupElems) {
    // Generate class name
    String cssClass = "toggleGroup" + RandomUtil.randomAlphabetic(5);

    // Setup onChange on toggle element
    toggleElem.attribute("onchange",
        "toggleGroupElements(this,'" + cssClass + "',true)");

    for (Input elem : groupElems) {
      elem.cssClass(cssClass);
    }
  }

  private Element makeDisplayCfg() {
    Composite comp = new Composite();
    Form frm = new Form(srvURL(myServletDescr()));
    frm.method("POST");
    Table tbl = new Table(0, "align=center cellspacing=2 cellpadding=0");

    addSection(tbl, HEADING_TARGET);

    addFieldToTable(tbl, LABEL_TARGET_HOSTNAME, hostname);
    addFieldToTable(tbl, LABEL_TARGET_PORT, String.valueOf(cfgUiPort));
    addFieldToTable(tbl, LABEL_TARGET_USERNAME, userName);
    addHiddenFieldToTable(tbl, LABEL_DATABASE_PASSWORD, "********");

    addSubsection(tbl, HEADING_DATABASE);

    Configuration v2cfg = mCfg.getConfigTree(V2_PREFIX);

    addFieldToTable(tbl, LABEL_DATABASE_HOSTNAME, v2cfg.get(DbManager.PARAM_DATASOURCE_SERVERNAME));
    addFieldToTable(tbl, LABEL_DATABASE_PORT, v2cfg.get(DbManager.PARAM_DATASOURCE_PORTNUMBER));
    addFieldToTable(tbl, LABEL_DATABASE_TYPE,
        getHumanReadableDatabaseType(v2cfg.get(DbManager.PARAM_DATASOURCE_CLASSNAME)));
    addFieldToTable(tbl, LABEL_DATABASE_NAME,
        getShortenedDatabaseName(v2cfg.get(DbManager.PARAM_DATASOURCE_DATABASENAME)));
    addFieldToTable(tbl, LABEL_DATABASE_USER, v2cfg.get(DbManager.PARAM_DATASOURCE_USER));
    addHiddenFieldToTable(tbl, LABEL_DATABASE_PASSWORD, "********");

    addSection(tbl, HEADING_OPTIONS);

    addDisabledCheckboxToTable(tbl, LABEL_DRY_RUN,
        KEY_DRY_RUN_ENABLED, dryRunEnabled);
    addDisabledCheckboxToTable(tbl, LABEL_DELETE_EACH_AU,
        KEY_DELETE_AUS, isDeleteAusEnabled);

    // Advanced migration options - only in debug mode
    if (migrationMgr.isMigrationInDebugMode()) {
      addFieldToTable(tbl, "Move Deleted AUs To", deletedAusDir);
      addFieldToTable(tbl, "Delete AUs interval", deleteAusInterval);
    }

    addCommonFormElements(tbl);
    frm.add(tbl);
    comp.add(frm);
    return comp;
  }

  /**
   * Returns the basename of the Derby database name (a path); leaves the
   * database name unmodified for PostgreSQL and MySQL databases.
   */
  private String getShortenedDatabaseName(String databaseName) {
    if (!StringUtil.isNullString(databaseName))  {
      return new File(databaseName).getName();
    } else {
      return null;
    }
  }

  /**
   * Returns a human-readable database type, given the datasource class name.
   * @param clsName A {@link String} containing the datasource class name.
   * @return A {@link String} containing the human-readable database type.
   */
  private String getHumanReadableDatabaseType(String clsName) {
    if (DbManagerSql.isTypeDerby(clsName)) {
      return "Derby";
    } else if (DbManagerSql.isTypePostgresql(clsName)) {
      return "PostgreSQL";
    } else if (DbManagerSql.isTypeMysql(clsName)) {
      return "MySQL";
    } else {
      return clsName;
    }
  }

  /**
   * Appends a row to the table that displays an error message.
   * @param tbl The {@link Table} to append a row to.
   * @param errMsg A {@link String} containing the error message.
   */
  private void displayInputError(Table tbl, String errMsg) {
    tbl.newRow();
    tbl.newCell(CENTERED_CELL);
    Composite block = new Composite();
    block.add(ERRORBLOCK_ERROR_BEFORE);
    block.add(encodeText(errMsg));
    block.add(ERRORBLOCK_ERROR_AFTER);
    tbl.add(block);
  }

  /**
   * Adds a row to the table containing the section heading.
   * @param tbl The {@link Table} to append a row to.
   * @param heading A {@link String} containing the section heading.
   */
  private void addSection(Table tbl, String heading) {
    tbl.newRow();
    tbl.newCell();
    tbl.add(new Break());
    tbl.newRow();
    tbl.newCell();
    Heading headingElem = new Heading(3, heading);
    tbl.add(headingElem);
  }

  /**
   * Adds a row to the table containing the subsection heading.
   * @param tbl The {@link Table} to append a row to.
   * @param heading A {@link String} containing the subsection heading.
   */
  private void addSubsection(Table tbl, String heading) {
    tbl.newRow();
    tbl.newCell(CENTERED_CELL);
    Heading headingElem = new Heading(4, heading);
    tbl.add(new Break());
    tbl.add(headingElem);
  }

  private void addInputToTable(Table tbl, String label, String key, String init, int size) {
    Input in = new Input(Input.Text, key, init);
    in.setSize(size);
    addElementToTable(tbl, label, in);
  }

  private void addHiddenInputToTable(Table tbl, String label, String key, String init, int size) {
    Input in = new Input(Input.Password, key, init);
    in.setSize(size);
    in.attribute("autocomplete", "one-time-code");
    addElementToTable(tbl, label, in);
  }

  private Input addCheckboxToTable(Table tbl, String label, String key, boolean checked) {
    return addCheckboxToTable(tbl, label, key, checked, false);
  }

  private Input addDisabledCheckboxToTable(Table tbl, String label, String key, boolean checked) {
    return addCheckboxToTable(tbl, label, key, checked, true);
  }

  private Input addCheckboxToTable(Table tbl, String label, String key, boolean checked, boolean disabled) {
    Input in = new Input(Input.Checkbox, key);
    if (checked) {
      in.attribute("checked");
    }
    if (disabled) {
      in.attribute("disabled");
    }
    addElementToTable(tbl, label, in);
    return in;
  }

  private void addFieldToTable(Table tbl, String lbl, String val) {
    Composite c = new Composite();
    c.add(val);
    addElementToTable(tbl, lbl, c);
  }

  private void addHiddenFieldToTable(Table tbl, String lbl, String val) {
    Composite c = new Composite();
    c.add(val.replaceAll(".", "&bull;"));
    addElementToTable(tbl, lbl, c);
  }

  private void addElementToTable(Table tbl, String label, Element elem) {
    tbl.newRow();
    tbl.newCell("align=right");
    tbl.add(label);
    tbl.add(":");
    tbl.newCell();
    tbl.add("&nbsp;");
    tbl.newCell();
    tbl.add(elem);
  }

  /**
   * Constructs a {@link Configuration} containing the migration configuration. The database
   * parameters are prefixed with "v2" as to not override this daemon's database configuration.
   *
   * @param targetHost
   * @param targetCfg
   * @return
   */
  private Configuration getMigrationConfig(String targetHost, Configuration targetCfg) throws MigrationManager.UnsupportedConfigurationException {
    Configuration mCfg = ConfigManager.newConfiguration();

    // Internal migration control
    mCfg.put(MigrationManager.PARAM_IS_MIGRATOR_CONFIGURED, "true");
    mCfg.put(MigrationManager.PARAM_IS_IN_MIGRATION_MODE, "false");
    mCfg.put(MigrationManager.PARAM_IS_DB_MOVED, "false");

    // Migration target configuration
    mCfg.put(MigrateContent.PARAM_HOSTNAME, hostname);
    mCfg.put(V2AuMover.PARAM_CFG_UI_PORT, String.valueOf(cfgUiPort));
    mCfg.put(MigrateContent.PARAM_USERNAME, userName);
    mCfg.put(MigrateContent.PARAM_PASSWORD, userPass);


    Configuration v2Cfg = ConfigManager.newConfiguration();

    // Proxy forwarding settings
    v2Cfg.put(ProxyManager.PARAM_FORWARD_PROXY,
              targetHost + ":" +
              targetCfg.getInt(V2_PARAM_PROXY_PORT, V2_DEFAULT_PROXY_PORT));

    // ServeContent forwarding settings
    v2Cfg.put(ServeContent.PARAM_FORWARD_SERVE_CONTENT,
              targetHost + ":" +
              targetCfg.getInt(V2_PARAM_CONTENTSERVLET_PORT,
                               V2_DEFAULT_CONTENTSERVLET_PORT));

    // LCAP forwarding settings
    String targetIdentity = targetCfg.get(V2_PARAM_LOCAL_V3_IDENTITY);
    String targetIp = null;
    int targetLcapPort = targetCfg.getInt(V2_PARAM_ACTUAL_V3_LCAP_PORT, -1);
    if (targetLcapPort > 0) {
      targetIp = getLcapForwardAddr(targetHost, targetIdentity, targetCfg);
      String migTo = IDUtil.ipAddrToKey(targetIp, targetLcapPort);
      v2Cfg.put(LcapRouter.PARAM_MIGRATE_TO, migTo);
      log.info("Configuring to forward LCAP to: " + migTo);
    } else {
      log.error("Target didn't supply " + V2_PARAM_ACTUAL_V3_LCAP_PORT);
    }

    // If V2 will route crawls back through V1's HTTP proxy during
    // migration (so publisher allowlists keep recognizing V1's IP),
    // ensure V1's proxy is started on the agreed port and that V2's IP
    // is on the access list.  These v2.* keys are promoted to the
    // unprefixed params when V1 enters migration mode (see
    // ConfigManager.setMigrationParams).
    if (targetCfg.getBoolean(V2_PARAM_PROXY_IN_MIGRATION_MODE,
                             V2_DEFAULT_PROXY_IN_MIGRATION_MODE)) {
      int proxyPort = targetCfg.getInt(V2_PARAM_MIGRATION_PROXY_PORT,
                                       V2_DEFAULT_MIGRATION_PROXY_PORT);
      v2Cfg.put(ProxyManager.PARAM_START, "true");
      v2Cfg.put(ProxyManager.PARAM_PORT, String.valueOf(proxyPort));

      if (targetIp == null) {
        try {
          targetIp = getLcapForwardAddr(targetHost, targetIdentity, targetCfg);
        } catch (IllegalArgumentException e) {
          log.warning("Couldn't determine V2 IP for proxy access list: " +
                      e.getMessage());
        }
      }
      if (targetIp != null) {
        Configuration currentCfg = ConfigManager.getCurrentConfig();
        java.util.List<String> ips = new ArrayList<String>(
            currentCfg.getList(ProxyManager.PARAM_IP_INCLUDE));
        ips.add(targetIp);
        v2Cfg.put(ProxyManager.PARAM_IP_INCLUDE,
                  StringUtil.separatedString(ips, Constants.LIST_DELIM));

        // Also allow it to use the CONNECT method
        v2Cfg.put(ProxyManager.PARAM_CONNECT_ALLOWED_FROM, targetIp);
      }
    }

    // DbManager configuration
    v2Cfg.put(DbManager.PARAM_WAIT_FOR_EXTERNAL_SETUP, "true");
    v2Cfg.put(DbManager.PARAM_TARGET_DB_VERSION, V2_TARGET_DB_VERSION);

    // Datasource configuration
    Configuration dsCfg = ConfigManager.newConfiguration();
    dsCfg.copyFrom(targetCfg.getConfigTree(V2_PARAM_METADATADBMANAGER_DATASOURCE_ROOT));
    String dsClassName = dsCfg.get("className");

    // Throw if target is using anything other than PostgreSQL
    if (!DbManagerSql.isTypePostgresql(dsClassName)) {
      throw new MigrationManager.UnsupportedConfigurationException("Only PostgreSQL database supported");
    }

    String dsServerName = dsCfg.get("serverName");
    if (StringUtil.isNullString(dsServerName) ||
        dsServerName.equals(V2_POSTGRES_CONTAINER_NAME)) {
      dsCfg.put("serverName", targetHost);
    }

    String dsUser = dsCfg.get("user");
    if (StringUtil.isNullString(dsUser)) {
      dsCfg.put("user", V2_DEFAULT_METADATADBMANAGER_DATASOURCE_USER);
    }

    String dsDatabaseName = dsCfg.get("databaseName");
    if (DbManagerSql.isTypePostgresql(dsClassName) &&
        StringUtil.isNullString(dsDatabaseName)) {
      dsCfg.put("databaseName", V2_DEFAULT_METADATADBMANAGER_DATASOURCE_DATABASENAME);
    }

    String dsPortNumber = dsCfg.get("portNumber");
    if (StringUtil.isNullString(dsPortNumber)) {
      String defaultDbPortNumber = getDefaultDatabasePortNumber(dsClassName);
      if (!StringUtil.isNullString(defaultDbPortNumber)) {
        dsCfg.put("portNumber", defaultDbPortNumber);
      }
    }

    v2Cfg.addAsSubTree(dsCfg, DbManager.DATASOURCE_ROOT);
    mCfg.addAsSubTree(v2Cfg, V2_PREFIX);
    return mCfg;
  }

  // Prefer to get IP addr from target hostname as that's known to be
  // reachable from V1
  private String getLcapForwardAddr(String targetHost, String targetIdentity,
                                    Configuration targetCfg) {
    // If localhost, use actual IP addr instead.  V2 comm can't use
    // loopback because it's running in a container, and both ends
    // much match
    if (targetHost.equalsIgnoreCase("localhost") ||
        targetHost.equals("127.0.0.1")) {
      return targetCfg.get(IdentityManager.PARAM_LOCAL_IP);
    }
    String targetIp;
    try {
      IPAddr targetIPAddr = IPAddr.getByName(targetHost);
      targetIp = targetIPAddr.getHostAddress();
      log.debug("LCAP forward addr from hostname: " + targetIp);
      return targetIp;
    } catch (UnknownHostException e) {
      try {
        PeerAddress v2Pad = PeerAddress.makePeerAddress(targetIdentity);
        if (v2Pad instanceof PeerAddress.Tcp) {
          PeerAddress.Tcp v2Padv3 = (PeerAddress.Tcp)v2Pad;
          targetIp = v2Padv3.getIPAddr().toString();
          log.debug("LCAP forward addr from target identity: " + targetIp);
          return targetIp;
        } else {
          log.error("Target doesn't have LCAP V3 identity: " + targetIdentity);
          throw new IllegalArgumentException("Target not configured with an LCAP V@ identity");
        }
      } catch (IdentityManager.MalformedIdentityKeyException e2) {
        String msg = "Target LCAP identity is malformed, can't set up for migration: " + targetIdentity;
        log.error(msg);
        throw new IllegalArgumentException(msg);
      }
    }
  }


  /**
   * Returns the default port database under V2, given the data source class name.
   *
   * @param dsClassName A {@link String} containing the data source class name.
   * @return A {@link String} containing the default database port number, or {@code null}
   * if the data source class name is not recognized.
   */
  private String getDefaultDatabasePortNumber(String dsClassName) {
    String defaultDbPortNumber = null;

    if (DbManagerSql.isTypeDerby(dsClassName))
      defaultDbPortNumber = V2_DEFAULT_METADATADBMANAGER_DATASOURCE_PORTNUMBER;
    if (DbManagerSql.isTypePostgresql(dsClassName)) {
      defaultDbPortNumber = V2_DEFAULT_METADATADBMANAGER_DATASOURCE_PORTNUMBER_PG;
    } else if (DbManagerSql.isTypeMysql(dsClassName)) {
      defaultDbPortNumber = V2_DEFAULT_METADATADBMANAGER_DATASOURCE_PORTNUMBER_MYSQL;
    }

    return defaultDbPortNumber;
  }

  /**
   * Returns a copy of the {@link Configuration} with a subtree moved under a prefix.
   */
  public static Configuration addPrefixToSubtree(Configuration cfgSrc, String root, String prefix) {
    Configuration res = ConfigManager.newConfiguration();
    res.copyFrom(cfgSrc);
    res.removeConfigTree(root);
    res.addAsSubTree(cfgSrc.getConfigTree(root), prefix + "." + root);
    return res;
  }
}
