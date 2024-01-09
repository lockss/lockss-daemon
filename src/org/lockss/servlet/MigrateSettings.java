package org.lockss.servlet;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.db.DataSourceUtil;
import org.lockss.db.DbManager;
import org.lockss.db.DbManagerSql;
import org.lockss.laaws.MigrationManager;
import org.lockss.protocol.IdentityManager;
import org.lockss.protocol.LcapRouter;
import org.lockss.proxy.ProxyManager;
import org.lockss.repository.RepositoryManager;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.util.UrlUtil;
import org.lockss.util.XPathUtil;
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
  static final boolean DEFAULT_DELETE_AUS_ENABLED = true;

  static final String KEY_HOSTNAME = "hostname";
  static final String KEY_CFGSVC_UI_PORT = "cfgUiPort";
  static final String KEY_USERNAME = "username";
  static final String KEY_PASSWORD = "password";
  static final String KEY_DATABASE_PASS = "dbPassword";
  static final String KEY_FETCHED_CONFIG = "isTargetConfigFetched";
  static final String KEY_MIGRATION_CONFIG = "mCfg";

  static final String KEY_DELETED_AUS_DIR = "deletedAusDir";
  static final String KEY_DELETE_AUS_INTERVAL = "deleteAusInterval";
  static final String KEY_DELETE_AUS = "enableDeleteAus";

  static final String KEY_ACTION = "action";
  static final String ACTION_LOAD_V2_CFG = "Load Configuration";
  static final String ACTION_ENTER_MIGRATION_MODE = "Enter Migration Mode";
  static final String ACTION_EXIT_MIGRATION_MODE = "Exit Migration Mode";
  static final String ACTION_NEXT = "Next";

  LockssDaemon theDaemon;
  MigrationManager migrationMgr;

  String hostname;
  int cfgUiPort;
  String userName;
  String userPass;
  Configuration mCfg;
  String dbPass;

  boolean isDeleteAusEnabled;
  boolean isTargetConfigFetched;

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
  private void initParams() {
    requestMethod = req.getMethod();
    session = getSession();

    // Migration target
    hostname = getSessionAttribute(KEY_HOSTNAME, DEFAULT_HOSTNAME);
    cfgUiPort = getSessionAttribute(KEY_CFGSVC_UI_PORT, V2_DEFAULT_CFGSVC_UI_PORT);
    userName = getSessionAttribute(KEY_USERNAME, null);
    userPass = getSessionAttribute(KEY_PASSWORD, null);

    // Database
    dbPass = getSessionAttribute(KEY_DATABASE_PASS, null);

    // Migration options
    isDeleteAusEnabled = getSessionAttribute(KEY_DELETE_AUS, DEFAULT_DELETE_AUS_ENABLED);

    // Migration configuration
    mCfg = getSessionAttribute(KEY_MIGRATION_CONFIG, ConfigManager.newConfiguration());

    // Internal state
    isTargetConfigFetched = getSessionAttribute(KEY_FETCHED_CONFIG, false);
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
    isDeleteAusEnabled = DEFAULT_DELETE_AUS_ENABLED;
    isTargetConfigFetched = false;

    fetchError = null;
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

      hostname = getParameter(KEY_HOSTNAME);
      cfgUiPort = Integer.parseInt(getParameter(KEY_CFGSVC_UI_PORT));
      userName = getParameter(KEY_USERNAME);
      userPass = getParameter(KEY_PASSWORD);
      dbPass = getParameter(KEY_DATABASE_PASS);
      isDeleteAusEnabled = hasParameter(KEY_DELETE_AUS);

      switch (action) {
        case ACTION_LOAD_V2_CFG:
          try {
            Properties v2cfg = getConfigFromMigrationTarget();
            mCfg = getMigrationConfig(hostname, v2cfg);
            isTargetConfigFetched = true;
          } catch (IOException e) {
            fetchError = "Could not fetch migration target configuration";
            log.error(fetchError, e);
            mCfg.removeConfigTree(DbManager.DATASOURCE_ROOT);
            session.removeAttribute(KEY_FETCHED_CONFIG);
            isTargetConfigFetched = false;
          } finally {
            // Remember result of this action
            session.setAttribute(KEY_MIGRATION_CONFIG, mCfg);
            session.setAttribute(KEY_FETCHED_CONFIG, isTargetConfigFetched);

            // Remember form settings for display
            session.setAttribute(KEY_HOSTNAME, hostname);
            session.setAttribute(KEY_CFGSVC_UI_PORT, cfgUiPort);
            session.setAttribute(KEY_USERNAME, userName);
            session.setAttribute(KEY_PASSWORD, userPass);
            session.setAttribute(KEY_DELETE_AUS, isDeleteAusEnabled);
          }
          break;

        case ACTION_NEXT:
          if (!isTargetConfigFetched) {
            errMsg = "Missing database configuration";
//          } else if (hasMigrationTargetChanged()) {
          } else if (StringUtil.isNullString(dbPass)) {
            dbError = "Missing database password";
          } else if (!migrationMgr.isDaemonInMigrationMode()) {
            // Populate remaining migration configuration parameters
            mCfg.put(DbManager.PARAM_DATASOURCE_PASSWORD, dbPass);
            mCfg.put(MigrateContent.PARAM_DELETE_AFTER_MIGRATION, String.valueOf(isDeleteAusEnabled));

            // Test database connection
            try {
              Configuration dsCfg = DataSourceUtil.getRuntimeDataSourceConfig(mCfg);
              DataSourceUtil.validateDataSourceConfig(dsCfg);
            } catch (Throwable e) {
              dbError = "Could not connect to database";
              log.error(dbError, e);
              break;
            }

            // Write migration configuration to file
            writeMigrationConfigFile(
                addPrefixToSubtree(mCfg, DbManager.DATASOURCE_ROOT, "v2"));

            // Redirect to Migrate Content page
            String redir = srvURL(AdminServletManager.SERVLET_MIGRATE_CONTENT);
            resp.sendRedirect(redir);
          }
          break;

        default:
          errMsg = "Unknown action:" + action;
      }
    }

    displayPage();
  }

  private void displayPage() throws IOException {
    Page page = newPage();
    addCssLocations(page);
    layoutErrorBlock(page);
    ServletUtil.layoutExplanationBlock(page, "");
    page.add(makeForm());
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

    addFieldToTable(tbl, "Hostname", mCfg.get(DbManager.PARAM_DATASOURCE_SERVERNAME));
    addFieldToTable(tbl, "Port", mCfg.get(DbManager.PARAM_DATASOURCE_PORTNUMBER));
    addFieldToTable(tbl, "Database Type",
        getHumanReadableDatabaseType(mCfg.get(DbManager.PARAM_DATASOURCE_CLASSNAME)));
    addFieldToTable(tbl, "Database Name",
        getShortenedDatabaseName(mCfg.get(DbManager.PARAM_DATASOURCE_DATABASENAME)));
    addFieldToTable(tbl, "Database User", mCfg.get(DbManager.PARAM_DATASOURCE_USER));

    // Only prompt for a database password if we've fetched its other details
    if (isTargetConfigFetched) {
      addHiddenInputToTable(tbl,
          "Database Password",
          KEY_DATABASE_PASS, mCfg.get(DbManager.PARAM_DATASOURCE_PASSWORD, dbPass), 20);
    } else {
      addFieldToTable(tbl, "Database Password", mCfg.get(DbManager.PARAM_DATASOURCE_PASSWORD));
    }

    if (!StringUtil.isNullString(dbError)) {
      displayInputError(tbl, dbError);
    }

    addSection(tbl, "Migration Options");

    addCheckboxToTable(tbl, "Delete AUs after migration",
        KEY_DELETE_AUS, isDeleteAusEnabled);

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

//    Input toggleMigrationMode = new Input(Input.Submit, KEY_ACTION,
//        !migrationMgr.isDaemonInMigrationMode() ? ACTION_ENTER_MIGRATION_MODE : ACTION_EXIT_MIGRATION_MODE);
//    if (!isTargetConfigFetched && !migrationMgr.isDaemonInMigrationMode()) {
//      toggleMigrationMode.attribute("disabled");
//    }
//    tbl.add(toggleMigrationMode);

    Input nextButton = new Input(Input.Submit, KEY_ACTION, ACTION_NEXT);
    if (!isTargetConfigFetched) {
      nextButton.attribute("disabled");
    }

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
    log.info("databaseName = " + databaseName);
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
    addElementToTable(tbl, label, in);
  }

  private void addCheckboxToTable(Table tbl, String label, String key, boolean checked) {
    Input in = new Input(Input.Checkbox, key);
    if (checked) {
      in.attribute("checked");
    }
    addElementToTable(tbl, label, in);
  }

  private void addFieldToTable(Table tbl, String lbl, String val) {
    Composite c = new Composite();
    c.add(val);
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
  private Properties getConfigFromMigrationTarget() throws IOException {
    URL cfgStatUrl = new URL("http", hostname, cfgUiPort,
        "/DaemonStatus?table=ConfigStatus&output=csv");
    log.info("url = " + cfgStatUrl.toString());

    LockssUrlConnection conn = UrlUtil.openConnection(cfgStatUrl.toString());
    conn.setCredentials(userName, userPass);
    conn.setUserAgent("lockss");
    conn.execute();

    if (conn.getResponseCode() == 200) {
      try (InputStream csvInput = conn.getResponseInputStream()) {
        return propsFromCsv(csvInput);
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
   * @param targetProps
   * @return
   */
  private Configuration getMigrationConfig(String targetHost, Properties targetProps) {
    Configuration targetCfg = ConfigManager.fromProperties(targetProps);
    Properties mProps = new Properties();

    // Proxy forwarding settings
    if (targetCfg.getBoolean(ProxyManager.PARAM_START, false)) {
      mProps.put(ProxyManager.PARAM_FORWARD_PROXY,
          targetHost + ":" + targetCfg.getInt(
              V2_PARAM_PROXYMANAGER_PORT, V2_DEFAULT_PROXYMANAGER_PORT));
    }

    // ServeContent forwarding settings
    if (targetCfg.getBoolean(ContentServletManager.PARAM_START, false)) {
      mProps.put(ServeContent.PARAM_FORWARD_SERVE_CONTENT,
          targetHost + ":" + targetCfg.getInt(
              V2_PARAM_CONTENTSERVLETMANAGER_PORT, V2_DEFAULT_CONTENTSERVLETMANAGER_PORT));
    }

    // LCAP forwarding settings
    mProps.put(LcapRouter.PARAM_MIGRATE_TO,
        targetCfg.get(IdentityManager.PARAM_LOCAL_V3_IDENTITY));

    // AU migration settings (provided by user input)
    mProps.put(MigrateContent.PARAM_DELETE_AFTER_MIGRATION, String.valueOf(isDeleteAusEnabled));
    mProps.put(RepositoryManager.PARAM_MOVE_DELETED_AUS_TO, "MIGRATED");
    mProps.put(RepositoryManager.PARAM_DELETEAUS_INTERVAL,
        String.valueOf(RepositoryManager.DEFAULT_DELETEAUS_INTERVAL));

    // Database settings
    Configuration dsCfg = ConfigManager.newConfiguration();
    dsCfg.copyFrom(targetCfg.getConfigTree(V2_PARAM_METADATADBMANAGER_DATASOURCE_ROOT));
    String dsClassName = dsCfg.get("className");

    // If using Derby, we need to reconstruct the full database path
    if (DbManagerSql.isTypeDerby(dsClassName)) {
      String databaseName = dsCfg.get("databaseName",
          V2_DEFAULT_METADATADBMANAGER_DATASOURCE_DATABASENAME);
      String derbyBaseDir = targetCfg.get(V2_PARAM_DERBY_DB_DIR);

      File derbyDbDir = new File(derbyBaseDir, databaseName);

      dsCfg.remove("databaseName");
      dsCfg.put("databaseName", derbyDbDir.toString());

      if (!derbyDbDir.isAbsolute()) {
        try {
          dsCfg.remove("databaseName");
          derbyDbDir = new File(getCwdOfMigrationTarget(), derbyDbDir.toString()) ;
          dsCfg.put("databaseName", derbyDbDir.toString());
        } catch (IOException e) {
          // Leave databaseName unset (and allow getRuntimeDataSourceConfig to
          // throw a DbException)
        }
      }
    }

    String dsServerName = dsCfg.get("serverName");
    if (StringUtil.isNullString(dsServerName)) {
      dsCfg.put("serverName", targetHost);
    }

    String dsUser = dsCfg.get("user");
    if (StringUtil.isNullString(dsUser)) {
      dsCfg.put("user", V2_DEFAULT_METADATADBMANAGER_DATASOURCE_USER);
    }

//    mProps.put(DbManager.PARAM_DATASOURCE_CLASSNAME, dsClassName);
//
//    mProps.put(DbManager.PARAM_DATASOURCE_SERVERNAME,
//        targetCfg.get(V2_PARAM_METADATADBMANAGER_DATASOURCE_SERVERNAME,
//            V2_DEFAULT_METADATADBMANAGER_DATASOURCE_SERVERNAME));

    String dsPortNumber = dsCfg.get("portNumber");
    if (StringUtil.isNullString(dsPortNumber)) {
      String defaultDbPortNumber = getDefaultDatabasePortNumber(dsClassName);
      if (!StringUtil.isNullString(defaultDbPortNumber)) {
        dsCfg.put("portNumber", defaultDbPortNumber);
      }
    }

//    mProps.put(DbManager.PARAM_DATASOURCE_DATABASENAME,
//        targetCfg.get(V2_PARAM_METADATADBMANAGER_DATASOURCE_DATABASENAME,
//            V2_DEFAULT_METADATADBMANAGER_DATASOURCE_DATABASENAME));
//
//    mProps.put(DbManager.PARAM_DATASOURCE_USER,
//        targetCfg.get(V2_PARAM_METADATADBMANAGER_DATASOURCE_USER,
//            V2_DEFAULT_METADATADBMANAGER_DATASOURCE_USER));

    // Convert migration properties into a Configuration and merge the datasource
    // configuration as a subtree:
    Configuration res = ConfigManager.newConfiguration();
    res.copyFrom(ConfigManager.fromProperties(mProps));
    res.addAsSubTree(dsCfg, DbManager.DATASOURCE_ROOT);
    return res;
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
   * File (see {@link ConfigManager.CONFIG__FILE_MIGRATION}).
   *
   * @param mCfg
   * @throws IOException
   */
  private void writeMigrationConfigFile(Configuration mCfg) throws IOException {
    ConfigManager cfgMgr = ConfigManager.getConfigManager();
    File cfgFile = cfgMgr.getCacheConfigFile(ConfigManager.CONFIG_FILE_MIGRATION);
    try (FileOutputStream cfgOut = new FileOutputStream(cfgFile)) {
      // Timestamp header is included in the only implementation of this method
      // i.e., ConfigurationPropTreeImpl#store(OutputStream, String):
      mCfg.store(cfgOut, null);
    }
  }

  private static final String V2_PARAM_PROXYMANAGER_PORT =
      ProxyManager.PARAM_PORT;
  private static final String V2_PARAM_CONTENTSERVLETMANAGER_PORT =
      ContentServletManager.PARAM_PORT;
  private static final String V2_PARAM_METADATADBMANAGER_PREFIX =
      "org.lockss.metadataDbManager.";
  public static final String V2_PARAM_DERBY_DB_DIR = "org.lockss.db.derbyDbDir";
  private static final String V2_PARAM_METADATADBMANAGER_DATASOURCE_ROOT =
      V2_PARAM_METADATADBMANAGER_PREFIX + "datasource";
  private static final String V2_PARAM_METADATADBMANAGER_DATASOURCE_CLASSNAME =
      V2_PARAM_METADATADBMANAGER_DATASOURCE_ROOT + ".className";
  private static final String V2_PARAM_METADATADBMANAGER_DATASOURCE_SERVERNAME =
      V2_PARAM_METADATADBMANAGER_DATASOURCE_ROOT + ".serverName";
  private static final String V2_PARAM_METADATADBMANAGER_DATASOURCE_PORTNUMBER =
      V2_PARAM_METADATADBMANAGER_DATASOURCE_ROOT + ".portNumber";
  private static final String V2_PARAM_METADATADBMANAGER_DATASOURCE_DATABASENAME =
      V2_PARAM_METADATADBMANAGER_DATASOURCE_ROOT + ".databaseName";
  private static final String V2_PARAM_METADATADBMANAGER_DATASOURCE_USER =
      V2_PARAM_METADATADBMANAGER_DATASOURCE_ROOT + ".user";
  private static final String V2_PARAM_METADATADBMANAGER_DATASOURCE_PASSWORD =
      V2_PARAM_METADATADBMANAGER_DATASOURCE_ROOT + ".password";

  private static final int V2_DEFAULT_CFGSVC_UI_PORT = 24621;
  private static final int V2_DEFAULT_PROXYMANAGER_PORT = 24670;
  private static final int V2_DEFAULT_CONTENTSERVLETMANAGER_PORT = 24680;
  private static final String V2_DEFAULT_METADATADBMANAGER_DATASOURCE_CLASSNAME =
      EmbeddedDataSource.class.getCanonicalName();
  private static final String V2_DEFAULT_METADATADBMANAGER_DATASOURCE_SERVERNAME = "localhost";
  private static final String V2_DEFAULT_METADATADBMANAGER_DATASOURCE_PORTNUMBER = "1527";
  private static final String V2_DEFAULT_METADATADBMANAGER_DATASOURCE_PORTNUMBER_PG = "5432";
  private static final String V2_DEFAULT_METADATADBMANAGER_DATASOURCE_PORTNUMBER_MYSQL = "3306";
  private static final String V2_DEFAULT_METADATADBMANAGER_DATASOURCE_DATABASENAME = "LockssMetadataDbManager";
  private static final String V2_DEFAULT_METADATADBMANAGER_DATASOURCE_USER = "LOCKSS";
  private static final String V2_DEFAULT_METADATADBMANAGER_DATASOURCE_PASSWORD = "insecure";
}
