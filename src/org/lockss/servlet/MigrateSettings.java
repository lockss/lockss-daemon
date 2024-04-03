package org.lockss.servlet;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.lang.RandomStringUtils;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.db.DataSourceUtil;
import org.lockss.db.DbManager;
import org.lockss.db.DbManagerSql;
import org.lockss.laaws.MigrationManager;
import org.lockss.laaws.V2AuMover;
import org.lockss.protocol.IdentityManager;
import org.lockss.protocol.LcapRouter;
import org.lockss.proxy.ProxyManager;
import org.lockss.repository.RepositoryManager;
import org.lockss.util.*;
import org.lockss.util.urlconn.LockssUrlConnection;
import org.mortbay.html.*;
import org.mortbay.http.HttpRequest;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.URL;
import java.util.Properties;

import static org.lockss.laaws.MigrationConstants.*;

/**
 * LOCKSS servlet that accepts migration settings from the user and
 * can enter the daemon into migration mode.
 */
public class MigrateSettings extends LockssServlet {
  static Logger log = Logger.getLogger("MigrateSettings");

  private static final String FOOTNOTE_HOST_URL =
      "The V2 REST Service host name (localhost by default).";
  private static final String FOOTNOTE_USERNAME =
      "The username used to connect to the rest interface of the V2 services.";
  private static final String FOOTNOTE_PASSWORD =
      "The password used to connect to the rest interface of the V2 services.";

  static final String DEFAULT_HOSTNAME = "localhost";
  static final String KEY_INITIALIZED_FROM_CONFIG = "initializedFromConfig";
  static final String KEY_HOSTNAME = "hostname";
  static final String KEY_CFGSVC_UI_PORT = "cfgUiPort";
  static final String KEY_USERNAME = "username";
  static final String KEY_PASSWORD = "password";
  static final String KEY_DATABASE_PASS = "dbPassword";
  static final String KEY_FETCHED_V2_CONFIG = "isTargetConfigFetched";
  static final String KEY_MIGRATION_CONFIG = "mCfg";

  static final String KEY_DELETED_AUS_DIR = "deletedAusDir";
  static final String KEY_DELETE_AUS_INTERVAL = "deleteAusInterval";
  static final String KEY_IRREVOCABLE_MIGRATION_ENABLED = "irrevocableMigrationEnabled";
  static final String KEY_DELETE_AUS = "enableDeleteAus";

  static final String KEY_ACTION = "action";
  static final String ACTION_LOAD_V2_CFG = "Load Configuration";
  static final String ACTION_NEXT = "Next";

  LockssDaemon theDaemon;
  MigrationManager migrationMgr;

  String hostname;
  int cfgUiPort;
  String userName;
  String userPass;
  Configuration mCfg;
  String dbPass;

  boolean irrevocableMigrationEnabled;
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
    irrevocableMigrationEnabled = cfg.getBoolean(
        MigrationManager.PARAM_IRREVOCABLE_MIGRATION_ENABLED,
        MigrationManager.DEFAULT_IRREVOCABLE_MIGRATION_ENABLED);

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
    userName = getSessionAttribute(KEY_USERNAME, null);
    userPass = getSessionAttribute(KEY_PASSWORD, null);

    // Database
    dbPass = getSessionAttribute(KEY_DATABASE_PASS, null);

    // Migration options
    irrevocableMigrationEnabled = getSessionAttribute(
        KEY_IRREVOCABLE_MIGRATION_ENABLED, MigrationManager.DEFAULT_IRREVOCABLE_MIGRATION_ENABLED);

    isDeleteAusEnabled = getSessionAttribute(
        KEY_DELETE_AUS, MigrateContent.DEFAULT_DELETE_AFTER_MIGRATION);

    // Migration configuration
    mCfg = getSessionAttribute(KEY_MIGRATION_CONFIG, ConfigManager.newConfiguration());
  }

  private void initParamsFromFormData() {
    // Migration target
    hostname = getParameter(KEY_HOSTNAME);
    cfgUiPort = Integer.parseInt(getParameter(KEY_CFGSVC_UI_PORT));
    userName = getParameter(KEY_USERNAME);
    userPass = getParameter(KEY_PASSWORD);

    // Database
    dbPass = getParameter(KEY_DATABASE_PASS);

    // Migration configuration
    irrevocableMigrationEnabled = hasParameter(KEY_IRREVOCABLE_MIGRATION_ENABLED);
    isDeleteAusEnabled = hasParameter(KEY_DELETE_AUS);
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

  /**
   * Request handler for this {@link LockssServlet}.
   */
  @Override
  protected void lockssHandleRequest() throws ServletException, IOException {
    initParams();

    if (requestMethod.equals(HttpRequest.__POST)) {
      String action = getParameter(KEY_ACTION);
      switch (action) {
        case ACTION_LOAD_V2_CFG:
          try {
            initParamsFromFormData();
            Configuration v2cfg = getConfigFromMigrationTarget();
            mCfg = getMigrationConfig(hostname, v2cfg);
            isTargetConfigFetched = true;
            fetchError = null;
          } catch (IOException e) {
            fetchError = "Could not fetch migration target configuration";
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
            migrationMgr.setIsMigrating(false);
            dbPass = null;
          }

          break;

        case ACTION_NEXT:
          initParamsFromFormData();
          if (!isTargetConfigFetched) {
            errMsg = "Missing database configuration";
          } else if (DbManagerSql.isTypeDerby(mCfg.get(DbManager.PARAM_DATASOURCE_CLASSNAME))) {
            dbError = "Derby not supported";
          } else if (StringUtil.isNullString(dbPass)) {
            dbError = "Missing database password";
          } else if (!migrationMgr.isDaemonMigrating() || migrationMgr.isMigrationInDebugMode()) {
            // Populate remaining migration configuration parameters
            mCfg.put(V2_DOT + DbManager.PARAM_DATASOURCE_PASSWORD, dbPass);
            mCfg.put(MigrationManager.PARAM_IRREVOCABLE_MIGRATION_ENABLED,
                String.valueOf(irrevocableMigrationEnabled));
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
            writeMigrationConfigFile(mCfg);
            ConfigManager.getConfigManager().reloadAndWait();
            redirectToMigrateContent();
          }
          break;

        default:
          errMsg = "Unknown action:" + action;
      }
    }

    displayPage();

    // Remember state
    session.setAttribute(KEY_FETCHED_V2_CONFIG, isTargetConfigFetched);
    session.setAttribute(KEY_HOSTNAME, hostname);
    session.setAttribute(KEY_CFGSVC_UI_PORT, cfgUiPort);
    session.setAttribute(KEY_USERNAME, userName);
    session.setAttribute(KEY_PASSWORD, userPass);
    session.setAttribute(KEY_IRREVOCABLE_MIGRATION_ENABLED, irrevocableMigrationEnabled);
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
    page.add(!migrationMgr.isMigrationInDebugMode() &&
             (irrevocableMigrationEnabled && migrationMgr.isDaemonMigrating()) ?
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
    Table tbl = new Table(0, "align=center cellspacing=2 cellpadding=0");

    addSection(tbl, "Migration Target");

    addInputToTable(tbl,
        "V2 Hostname" + addFootnote(FOOTNOTE_HOST_URL),
        KEY_HOSTNAME, hostname, 40);
    addInputToTable(tbl,
        "V2 Configuration Service Port",
        KEY_CFGSVC_UI_PORT, String.valueOf(cfgUiPort), 20);
    addInputToTable(tbl,
        "Username" + addFootnote(FOOTNOTE_USERNAME),
        KEY_USERNAME, userName, 20);
    addHiddenInputToTable(tbl,
        "Password" + addFootnote(FOOTNOTE_PASSWORD),
        KEY_PASSWORD, userPass, 20);

    tbl.newRow();
    tbl.newCell(CENTERED_CELL);
    tbl.add(new Input(Input.Submit, KEY_ACTION, ACTION_LOAD_V2_CFG));

    if (!StringUtil.isNullString(fetchError)) {
      displayInputError(tbl, fetchError);
    }

    addSubsection(tbl, "Metadata Database");

    Configuration v2cfg = mCfg.getConfigTree(V2_PREFIX);

    addFieldToTable(tbl, "Hostname", v2cfg.get(DbManager.PARAM_DATASOURCE_SERVERNAME));
    addFieldToTable(tbl, "Port", v2cfg.get(DbManager.PARAM_DATASOURCE_PORTNUMBER));
    addFieldToTable(tbl, "Database Type",
        getHumanReadableDatabaseType(v2cfg.get(DbManager.PARAM_DATASOURCE_CLASSNAME)));
    addFieldToTable(tbl, "Database Name",
        getShortenedDatabaseName(v2cfg.get(DbManager.PARAM_DATASOURCE_DATABASENAME)));
    addFieldToTable(tbl, "Database User", v2cfg.get(DbManager.PARAM_DATASOURCE_USER));

    // Only prompt for a database password if we've fetched its other details
    if (isTargetConfigFetched) {
      addHiddenInputToTable(tbl,
          "Database Password",
          KEY_DATABASE_PASS, v2cfg.get(DbManager.PARAM_DATASOURCE_PASSWORD, dbPass), 20);
    } else {
      addFieldToTable(tbl, "Database Password", v2cfg.get(DbManager.PARAM_DATASOURCE_PASSWORD));
    }

    if (!StringUtil.isNullString(dbError)) {
      displayInputError(tbl, dbError);
    }

    addSection(tbl, "Migration Options");

    Input toggleElement = addCheckboxToTable(tbl, "Perform irrevocable migration",
        KEY_IRREVOCABLE_MIGRATION_ENABLED, irrevocableMigrationEnabled);

    Input toggleGroupElement = addCheckboxToTable(tbl, "Delete AUs after migration",
        KEY_DELETE_AUS, isDeleteAusEnabled);

    if (!migrationMgr.isMigrationInDebugMode()) {
      // Do not allow user to disable irrevocable migration settings once enabled
      if (irrevocableMigrationEnabled && migrationMgr.isDaemonMigrating()) {
        toggleElement.attribute("disabled");
      } else if (!irrevocableMigrationEnabled) {
        toggleGroupElement.attribute("disabled");
      }
    }

    setupToggleGroup(toggleElement, toggleGroupElement);

    // Advanced migration options - only in debug mode
    if (migrationMgr.isMigrationInDebugMode()) {
      addInputToTable(tbl,
          "Move Deleted AUs To",
          KEY_DELETED_AUS_DIR, deletedAusDir, 20);
      addInputToTable(tbl,
          "Delete AUs interval",
          KEY_DELETE_AUS_INTERVAL, deleteAusInterval, 20);
    }

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

    frm.add(tbl);
    comp.add(frm);
    return comp;
  }

  private void setupToggleGroup(Input toggleElem, Input... groupElems) {
    // Generate class name
    String cssClass = "toggleGroup" + RandomStringUtils.randomAlphabetic(5);

    // Setup onChange on toggle element
    toggleElem.attribute("onchange",
        "toggleGroupElements(this,'" + cssClass + "')");

    for (Input elem : groupElems) {
      elem.cssClass(cssClass);
    }
  }

  private Element makeDisplayCfg() {
    Composite comp = new Composite();
    Form frm = new Form(srvURL(myServletDescr()));
    frm.method("POST");
    Table tbl = new Table(0, "align=center cellspacing=2 cellpadding=0");

    addSection(tbl, "Migration Target");

    addFieldToTable(tbl, "V2 Hostname", hostname);
    addFieldToTable(tbl, "V2 Configuration Service Port", String.valueOf(cfgUiPort));
    addFieldToTable(tbl, "Username", userName);
    addHiddenFieldToTable(tbl, "Password", "********");

    addSubsection(tbl, "Metadata Database");

    Configuration v2cfg = mCfg.getConfigTree(V2_PREFIX);

    addFieldToTable(tbl, "Hostname", v2cfg.get(DbManager.PARAM_DATASOURCE_SERVERNAME));
    addFieldToTable(tbl, "Port", v2cfg.get(DbManager.PARAM_DATASOURCE_PORTNUMBER));
    addFieldToTable(tbl, "Database Type",
        getHumanReadableDatabaseType(v2cfg.get(DbManager.PARAM_DATASOURCE_CLASSNAME)));
    addFieldToTable(tbl, "Database Name",
        getShortenedDatabaseName(v2cfg.get(DbManager.PARAM_DATASOURCE_DATABASENAME)));
    addFieldToTable(tbl, "Database User", v2cfg.get(DbManager.PARAM_DATASOURCE_USER));
    addHiddenFieldToTable(tbl, "Database Password", "********");

    addSection(tbl, "Migration Options");

    addDisabledCheckboxToTable(tbl, "Perform irrevocable migration",
        KEY_IRREVOCABLE_MIGRATION_ENABLED, irrevocableMigrationEnabled);
    addDisabledCheckboxToTable(tbl, "Delete AUs after migration",
        KEY_DELETE_AUS, isDeleteAusEnabled);

    // Advanced migration options - only in debug mode
    if (migrationMgr.isMigrationInDebugMode()) {
      addFieldToTable(tbl, "Move Deleted AUs To", deletedAusDir);
      addFieldToTable(tbl, "Delete AUs interval", deleteAusInterval);
    }

    tbl.newRow();
    tbl.newCell(CENTERED_CELL);
    tbl.add(new Break());
    tbl.add(new Break(Break.Rule));
    tbl.add(new Break());

    Input nextButton = new Input(Input.Submit, KEY_ACTION, ACTION_NEXT);
    tbl.add(nextButton);

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
    in.attribute("autocomplete", "new-password");
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
   * Fetches and returns the Configuration Status table of a remote machine. The set of
   * parameters returned by this method does not reflect the full set of parameters on
   * the remote machine since, for example, passwords are not included.
   *
   * @return A {@link Properties} containing the configuration from the remote machine.
   * @throws IOException Thrown if there were network errors, or if the server response was
   * not a 200.
   */
  private Configuration getConfigFromMigrationTarget() throws IOException {
    URL cfgStatUrl = new URL("http", hostname, cfgUiPort,
        "/DaemonStatus?table=ConfigStatus&output=csv");
    log.info("url = " + cfgStatUrl.toString());

    LockssUrlConnection conn = UrlUtil.openConnection(cfgStatUrl.toString());
    conn.setCredentials(userName, userPass);
    conn.setUserAgent("lockss");
    conn.execute();

    if (conn.getResponseCode() == 200) {
      try (InputStream csvInput = conn.getResponseInputStream()) {
        return ConfigManager.fromProperties(propsFromCsv(csvInput));
      }
    }

    throw new IOException(
        "Unexpected response from migration target: " + conn.getResponseCode());
  }

  /**
   * Fetches the Platform Status page of a remote machine and returns its current
   * working directory.
   * @return A {@link String} containing the remote machine's current working directory.
   * @throws IOException Thrown if there were network errors, or if the server response
   * was not a 200.
   */
  private String getCwdOfMigrationTarget() throws IOException {
    URL cfgStatUrl = new URL("http", hostname, cfgUiPort,
        "/DaemonStatus?table=PlatformStatus&output=xml");
    log.info("url = " + cfgStatUrl.toString());

    LockssUrlConnection conn = UrlUtil.openConnection(cfgStatUrl.toString());
    conn.setCredentials(userName, userPass);
    conn.setUserAgent("lockss");
    conn.execute();

    if (conn.getResponseCode() == 200) {
      try (InputStream xmlInput = conn.getResponseInputStream()) {
        // FIXME: The factory and XPath expression could be constants
        InputSource inputSource = new InputSource(xmlInput);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(inputSource);
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression cwdExp = xpath.compile("/table/summaryinfo/title[text()='Cwd']/following-sibling::value");
        return XPathUtil.evaluateString(cwdExp, doc);
      } catch (Throwable t) {
        throw new IOException("Error parsing platform status XML", t);
      }
    }

    throw new IOException(
        "Unexpected response from migration target: " + conn.getResponseCode());
  }

  /**
   * Transforms an {@link InputStream} containing the CSV output from a Configuration Status Table
   * into a {@link Properties}.
   *
   * @param csvStream
   * @return
   * @throws IOException
   */
  public static Properties propsFromCsv(InputStream csvStream) throws IOException {
    Properties result = new Properties();
    InputStreamReader csvReader = new InputStreamReader(csvStream);
    CSVParser csvParser = new CSVParser(csvReader, CSVFormat.DEFAULT.withHeader("Name", "Value"));
    csvParser.forEach(record -> result.put(record.get("Name"), record.get("Value")));
    return result;
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
    mCfg.put(MigrationManager.PARAM_IS_MIGRATING, "false");
    mCfg.put(MigrationManager.PARAM_IS_DB_MOVED, "false");

    // Migration target configuration
    mCfg.put(MigrateContent.PARAM_HOSTNAME, hostname);
    mCfg.put(V2AuMover.PARAM_CFG_UI_PORT, String.valueOf(cfgUiPort));
    mCfg.put(MigrateContent.PARAM_USERNAME, userName);
    mCfg.put(MigrateContent.PARAM_PASSWORD, userPass);


    Configuration v2Cfg = ConfigManager.newConfiguration();

    // Proxy forwarding settings
    if (targetCfg.getBoolean(ProxyManager.PARAM_START, V2_DEFAULT_PROXYMANAGER_START)) {
      v2Cfg.put(ProxyManager.PARAM_FORWARD_PROXY,
          targetHost + ":" + targetCfg.getInt(
              V2_PARAM_PROXYMANAGER_PORT, V2_DEFAULT_PROXYMANAGER_PORT));
    }

    // ServeContent forwarding settings
    if (targetCfg.getBoolean(ContentServletManager.PARAM_START, V2_DEFAULT_CONTENTSERVLETMANAGER_START)) {
      v2Cfg.put(ServeContent.PARAM_FORWARD_SERVE_CONTENT,
          targetHost + ":" + targetCfg.getInt(
              V2_PARAM_CONTENTSERVLETMANAGER_PORT, V2_DEFAULT_CONTENTSERVLETMANAGER_PORT));
    }

    // LCAP forwarding settings
    v2Cfg.put(LcapRouter.PARAM_MIGRATE_TO,
        targetCfg.get(IdentityManager.PARAM_LOCAL_V3_IDENTITY));

    // Datasource configuration
    Configuration dsCfg = ConfigManager.newConfiguration();
    dsCfg.copyFrom(targetCfg.getConfigTree(V2_PARAM_METADATADBMANAGER_DATASOURCE_ROOT));
    String dsClassName = dsCfg.get("className");

    // Throw if target is using anything other than PostgreSQL
    if (!migrationMgr.isMigrationInDebugMode() &&
        !DbManagerSql.isTypePostgresql(dsClassName)) {
      throw new MigrationManager.UnsupportedConfigurationException("Only PostgreSQL database supported");
    }

    String dsServerName = dsCfg.get("serverName");
    if (StringUtil.isNullString(dsServerName)) {
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

  /**
   * Writes a {@link Configuration} containing the Migration Configuration to the Migration Configuration
   * File (see {@link ConfigManager#CONFIG_FILE_MIGRATION}).
   *
   * @param mCfg A {@link Configuration} containing the migration configuration.
   * @throws IOException
   */
  private void writeMigrationConfigFile(Configuration mCfg) throws IOException {
    ConfigManager cfgMgr = ConfigManager.getConfigManager();
    cfgMgr.writeCacheConfigFile(mCfg, ConfigManager.CONFIG_FILE_MIGRATION,
        MigrationManager.CONFIG_FILE_MIGRATION_HEADER, true);
    cfgMgr.reloadAndWait();
  }
}
