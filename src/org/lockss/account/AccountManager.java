/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.account;

import java.io.*;
import java.util.*;
import java.security.*;

import org.lockss.app.*;
import org.lockss.daemon.status.*;
import org.lockss.config.*;
import org.lockss.servlet.*;
import org.lockss.util.*;
import org.lockss.alert.*;
import org.lockss.mail.*;

import static org.lockss.servlet.BaseServletManager.SUFFIX_AUTH_TYPE;
import static org.lockss.servlet.BaseServletManager.SUFFIX_ENABLE_DEBUG_USER;
import static org.lockss.servlet.BaseServletManager.SUFFIX_USE_SSL;

/** Manage user accounts
 */
public class AccountManager
  extends BaseLockssDaemonManager implements ConfigurableManager  {

  private static final Logger log = Logger.getLogger(AccountManager.class);

  public static final UserAccount NOBODY_ACCOUNT = new NobodyAccount();

  static final String PREFIX = Configuration.PREFIX + "accounts.";

  /** Enable account management */
  static final String PARAM_ENABLED = PREFIX + "enabled";
  static final boolean DEFAULT_ENABLED = false;

  /** Enable sending password change reminders */
  static final String PARAM_MAIL_ENABLED = PREFIX + "mailEnabled";
  static final boolean DEFAULT_MAIL_ENABLED = false;

  /** Select a preconfigured user account policy, one of LC, SSL, FORM,
   * BASIC  */
  public static final String PARAM_POLICY = PREFIX + "policy";
  public static final String DEFAULT_POLICY = null;

  /** Config subdir holding account info */
  static final String PARAM_ACCT_DIR = PREFIX + "acctDir";
  static final String DEFAULT_ACCT_DIR = "accts";

  /** Type of account to create for new users */
  public static final String PARAM_NEW_ACCOUNT_TYPE = PREFIX + "newUserType";
  public static final String DEFAULT_NEW_ACCOUNT_TYPE =
    "org.lockss.account.BasicUserAccount";

  /** If true, platform user is enabled on startup only if there are no
   * other enabled users with ROLE_USER_ADMIN */
  public static final String PARAM_CONDITIONAL_PLATFORM_USER =
    PREFIX + "conditionalPlatformUser";
  public static final boolean DEFAULT_CONDITIONAL_PLATFORM_USER = false;

  /** If true, alerts for users who have no email address will be sent to
   * the admin email */
  public static final String PARAM_MAIL_ADMIN_IF_NO_USER_EMAIL =
    PREFIX + "mailAdminIfNoUserEmail";
  public static final boolean DEFAULT_MAIL_ADMIN_IF_NO_USER_EMAIL = false;

  /** Frequency to check for password change reminders to send: daily,
   * weekly or monthly */
  public static final String PARAM_PASSWORD_CHECK_FREQ =
    PREFIX + "passwordCheck.frequency";
  public static final String DEFAULT_PASSWORD_CHECK_FREQ = "daily";

  /** Alertconfig set by AccountManager */
  public static final String PARAM_PASSWORD_REMINDER_ALERT_CONFIG =
    AlertManagerImpl.PARAM_CONFIG + ".acct";

  private static String PASSWORD_REMINDER_ALERT_CONFIG =
    "<org.lockss.alert.AlertConfig>" +
    "  <filters>" +
    "    <org.lockss.alert.AlertFilter>" +
    "      <pattern class=\"org.lockss.alert.AlertPatterns-Predicate\">" +
    "        <attribute>name</attribute>" +
    "        <relation>CONTAINS</relation>" +
    "        <value class=\"list\">" +
    "          <string>PasswordReminder</string>" +
    "          <string>AccountDisabled</string>" +
    "        </value>" +
    "      </pattern>" +
    "      <action class=\"org.lockss.alert.AlertActionMail\"/>" +
    "    </org.lockss.alert.AlertFilter>" +
    "    <org.lockss.alert.AlertFilter>" +
    "      <pattern class=\"org.lockss.alert.AlertPatterns-Predicate\">" +
    "        <attribute>name</attribute>" +
    "        <relation>CONTAINS</relation>" +
    "        <value class=\"list\">" +
    "          <string>AuditableEvent</string>" +
    "        </value>" +
    "      </pattern>" +
    "      <action class=\"org.lockss.alert.AlertActionSyslog\">" +
    "        <fixedLevel>-1</fixedLevel>" +
    "      </action>" +
    "    </org.lockss.alert.AlertFilter>" +
    "  </filters>" +
    "</org.lockss.alert.AlertConfig>";


  // Predefined account policies.  See ConfigManager.setConfigMacros()

  private static String UI_PREFIX = AdminServletManager.PREFIX;

  /** <code>LC</code>: SSL, form auth, Library of Congress password rules */
  public static String[] POLICY_LC = {
    PARAM_ENABLED, "true",
    PARAM_NEW_ACCOUNT_TYPE, "LC",
    PARAM_CONDITIONAL_PLATFORM_USER, "true",
    PARAM_PASSWORD_REMINDER_ALERT_CONFIG, PASSWORD_REMINDER_ALERT_CONFIG,
    PARAM_MAIL_ENABLED, "true",
    MailService.PARAM_ENABLED, "true",
    AlertManager.PARAM_ALERTS_ENABLED, "true",
    AlertActionMail.PARAM_ENABLED, "true",
    UI_PREFIX + SUFFIX_AUTH_TYPE, "Form",
    UI_PREFIX + SUFFIX_ENABLE_DEBUG_USER, "false",
    UI_PREFIX + SUFFIX_USE_SSL, "true",
  };

  /** <code>SSL</code>: SSL, form auth, configurable password rules */
  public static String[] POLICY_SSL = {
    PARAM_ENABLED, "true",
    PARAM_NEW_ACCOUNT_TYPE, "Basic",
    PARAM_PASSWORD_REMINDER_ALERT_CONFIG, PASSWORD_REMINDER_ALERT_CONFIG,
    UI_PREFIX + SUFFIX_AUTH_TYPE, "Form",
    UI_PREFIX + SUFFIX_USE_SSL, "true",
  };

  /** <code>Form</code>: HTTP, form auth, configurable password rules */
  public static String[] POLICY_FORM = {
    PARAM_ENABLED, "true",
    PARAM_NEW_ACCOUNT_TYPE, "Basic",
    PARAM_PASSWORD_REMINDER_ALERT_CONFIG, PASSWORD_REMINDER_ALERT_CONFIG,
    UI_PREFIX + SUFFIX_AUTH_TYPE, "Form",
    UI_PREFIX + SUFFIX_USE_SSL, "false",
  };

  /** <code>Basic</code>: HTTP, basic auth */
  public static String[] POLICY_BASIC = {
    PARAM_ENABLED, "true",
    PARAM_NEW_ACCOUNT_TYPE, "Basic",
    PARAM_PASSWORD_REMINDER_ALERT_CONFIG, PASSWORD_REMINDER_ALERT_CONFIG,
    UI_PREFIX + SUFFIX_AUTH_TYPE, "Basic",
    UI_PREFIX + SUFFIX_USE_SSL, "false",
  };

  /** <code>Compat</code>: HTTP, basic auth, no account management */
  public static String[] POLICY_COMPAT = {
    PARAM_ENABLED, "false",
    PARAM_NEW_ACCOUNT_TYPE, "Basic",
    PARAM_PASSWORD_REMINDER_ALERT_CONFIG, PASSWORD_REMINDER_ALERT_CONFIG,
    UI_PREFIX + SUFFIX_AUTH_TYPE, "Basic",
    UI_PREFIX + SUFFIX_USE_SSL, "false",
  };

  private boolean isEnabled = DEFAULT_ENABLED;
  private boolean mailEnabled = DEFAULT_MAIL_ENABLED;
  private boolean mailAdminIfNoUserEmail = DEFAULT_MAIL_ADMIN_IF_NO_USER_EMAIL;
  private String adminEmail = null;

  private ConfigManager configMgr;
  private String acctRelDir;
  private UserAccount.Factory acctFact;
  private String acctType;

  // Maps account name to UserAccount
  Map<String,UserAccount> accountMap = new HashMap<String,UserAccount>();

  public void startService() {
    super.startService();
    LockssDaemon daemon = getDaemon();
    configMgr = daemon.getConfigManager();
    resetConfig();
    if (isEnabled) {
      ensureAcctDir();
      loadUsers();
      try {
	AdminServletManager adminMgr = 
	  (AdminServletManager)daemon.getManager(LockssDaemon.SERVLET_MANAGER);
	if (adminMgr.hasUserSessions()) {
	  StatusService statusServ = getDaemon().getStatusService();
	  statusServ.registerStatusAccessor(UserStatus.USER_STATUS_TABLE,
					    new UserStatus(adminMgr));
	}
      } catch (IllegalArgumentException e) {
	log.warning("No AdminServletManager, not installing UserStatus table");
      }
    }
  }

  public void stopService() {
    StatusService statusServ = getDaemon().getStatusService();
    statusServ.unregisterStatusAccessor(UserStatus.USER_STATUS_TABLE);
    super.stopService();
  }

  public synchronized void setConfig(Configuration config,
				     Configuration prevConfig,
				     Configuration.Differences changedKeys) {

    if (changedKeys.contains(PREFIX)) {
      isEnabled = config.getBoolean(PARAM_ENABLED, DEFAULT_ENABLED);
      acctRelDir = config.get(PARAM_ACCT_DIR, DEFAULT_ACCT_DIR);
      acctType = config.get(PARAM_NEW_ACCOUNT_TYPE, DEFAULT_NEW_ACCOUNT_TYPE);
      acctFact = getUserFactory(acctType);

      mailEnabled = config.getBoolean(PARAM_MAIL_ENABLED, DEFAULT_MAIL_ENABLED);
      mailAdminIfNoUserEmail =
	config.getBoolean(PARAM_MAIL_ADMIN_IF_NO_USER_EMAIL,
			  DEFAULT_MAIL_ADMIN_IF_NO_USER_EMAIL);
      adminEmail = config.get(ConfigManager.PARAM_PLATFORM_ADMIN_EMAIL);
    }
  }

  public boolean isEnabled() {
    return isEnabled;
  }

  public String getDefaultAccountType() {
    return acctType;
  }

  UserAccount.Factory getUserFactory(String type) {
    if (!StringUtil.isNullString(type)) {
      if (type.equalsIgnoreCase("basic")) {
	return new BasicUserAccount.Factory();
      }
      if (type.equalsIgnoreCase("LC")) {
	return new LCUserAccount.Factory();
      }
      String clsName = type + "$Factory";
      try {
	Class cls = Class.forName(clsName);
	try {
	  return (UserAccount.Factory)cls.newInstance();
	} catch (Exception e) {
	  log.error("Can't instantiate new account factory: " + cls, e);
	}
      } catch (ClassNotFoundException e) {
	log.error("New account factory not found: " + clsName);
      }
    }
    log.warning("No factory of type '" + type + "', using basic accounts");
    return new BasicUserAccount.Factory();
  }    

  /** Create a new UserAccount of the configured type.  The account must be
   * added before becoming active. */
  public UserAccount createUser(String name) {
    return acctFact.newUser(name, this);
  }

  /** Add the user account, if doesn't conflict with an existing user and
   * it has a password. */
  synchronized UserAccount internalAddUser(UserAccount acct)
      throws NotAddedException {
    if (!acct.hasPassword()) {
      throw new NotAddedException("Can't add user without a password");
    }
    UserAccount old = accountMap.get(acct.getName());
    if (old != null && old != acct) {
      throw new UserExistsException("User already exists: " + acct.getName());
    }
    if (log.isDebug2()) {
      log.debug2("Add user " + acct.getName());
    }
    accountMap.put(acct.getName(), acct);
    return acct;
  }

  /** Add the user account, if doesn't conflict with an existing user and
   * it has a password. */
  public UserAccount addUser(UserAccount acct)
      throws NotAddedException, NotStoredException {
    internalAddUser(acct);
    if (acct.isEditable()) {
      storeUser(acct);
    }
    return acct;
  }

  /** Add a static, uneditable user account.  Used for compatibility with
   * platform-generated and old manually configured accounts.. */
  public UserAccount addStaticUser(String name, String credentials)
      throws NotAddedException {
    UserAccount acct = new StaticUserAccount.Factory().newUser(name, this);
    try {
      acct.setCredential(credentials);
    } catch (NoSuchAlgorithmException e) {
      log.error("Static user ( "  + acct.getName() + ") not installed", e);
    }
    return internalAddUser(acct);
  }

  /** Add platform user.  If {@link #PARAM_CONDITIONAL_PLATFORM_USER} is
   * true, the user is installed only if no other admin users exist */
  public void installPlatformUser(String platUser, String platPass) {
    if (!StringUtil.isNullString(platUser) &&
	!StringUtil.isNullString(platPass)) {
      String msg = null;
      if (!CurrentConfig.getBooleanParam(PARAM_CONDITIONAL_PLATFORM_USER,
					 DEFAULT_CONDITIONAL_PLATFORM_USER)) {
	log.info("Installing platform user");
      } else {
	// install only if no existing admin user
	for (UserAccount acct : getUsers()) {
	  if (!acct.isStaticUser()
	      && acct.isUserInRole(LockssServlet.ROLE_USER_ADMIN)) {
	    return;
	  }
	}
	msg = "platform admin account enabled because no other admin user";
      }
      try {
	UserAccount acct = addStaticUser(platUser, platPass);
	acct.setRoles(SetUtil.set(LockssServlet.ROLE_USER_ADMIN));
	if (msg != null) {
	  log.info("User " + acct.getName() + " " + msg);
	  acct.auditableEvent(msg);
	}
      } catch (NotAddedException e) {
	log.error("Can't install platform user", e);
      }
    }
  }

  boolean shouldInstallPlatformUser() {
    if (!CurrentConfig.getBooleanParam(PARAM_CONDITIONAL_PLATFORM_USER,
				       DEFAULT_CONDITIONAL_PLATFORM_USER)) {
      return true;
    }
    for (UserAccount acct : getUsers()) {
      if (!acct.isStaticUser()
	  && acct.isUserInRole(LockssServlet.ROLE_USER_ADMIN)) {
	return false;
      }
    }
    return true;
  }

  /** Delete the user */
  public boolean deleteUser(String name) {
    UserAccount acct = getUser(name);
    if (acct.isStaticUser()) {
      throw new IllegalArgumentException("Can't delete static account: "
					 + acct);
    }
    if (acct == null) {
      return true;
    }
    return deleteUser(acct);
  }

  static String DELETED_REASON = "Deleted";

  /** Delete the user */
  public synchronized boolean deleteUser(UserAccount acct) {
    boolean res = true;
    String filename = acct.getFilename();
    if (filename != null) {
      File file = new File(getAcctDir(), filename);
      // done this way so will return true if file is gone, whether we
      // deleted it or not
      file.delete();
      res = !file.exists();
    }
    if (res) {
      acct.disable(DELETED_REASON);	// paranoia, in case someone holds
					// onto object.
      accountMap.remove(acct.getName());
    }
    return res;
  }

  /** Store the current state of the user account on disk */
  public void storeUser(UserAccount acct) throws NotStoredException {
    if (acct.isStaticUser()) {
      throw new IllegalArgumentException("Can't store static account: " + acct);
    }
    if (getUser(acct.getName()) != acct) {
      throw new IllegalArgumentException("Can't store uninstalled account: "
					 + acct);
    }
    storeUserInternal(acct);
  }

  /** Store the current state of the user account on disk */
  public void storeUserInternal(UserAccount acct) throws NotStoredException {
    String filename = acct.getFilename();
    if (filename == null) {
      filename = generateFilename(acct);
    }
    File file =  new File(getAcctDir(), filename);
    try {
      storeUserInternal(acct, file);
    } catch (SerializationException e) {
      throw new NotStoredException("Error storing user in database", e);
    } catch (IOException e) {
      throw new NotStoredException("Error storing user in database", e);
    }

    acct.setFilename(file.getName());
  }

  void storeUserInternal(UserAccount acct, File file)
      throws IOException, SerializationException {
    if (acct.isChanged()) {
      if (log.isDebug2()) log.debug2("Storing account in " + file);
      makeObjectSerializer().serialize(file, acct);
      acct.notChanged();
      return;
    }
  }

  /** Load realm users from properties file.
   * The property file maps usernames to password specs followed by
   * an optional comma separated list of role names.
   *
   * @param propsUrl Filename or url of user properties file.
   * @exception IOException
   */
  public void loadFromProps(String propsUrl) throws IOException {
    if (log.isDebug()) log.debug("Load "+this+" from "+propsUrl);
    Properties props = new Properties();
    InputStream ins = getClass().getResourceAsStream(propsUrl);
    props.load(ins);
    loadFromProps(props);
  }

  public void loadFromProps(Properties props) throws IOException {
    for (Map.Entry ent : props.entrySet()) {
      String username = ent.getKey().toString().trim();
      String credentials = ent.getValue().toString().trim();
      String roles = null;
      int c = credentials.indexOf(',');
      if (c > 0) {
	roles = credentials.substring(c+1).trim();
	credentials = credentials.substring(0,c).trim();
      }
      if (!StringUtil.isNullString(username) &&
	  !StringUtil.isNullString(credentials)) {
	try {
	  UserAccount acct = addStaticUser(username, credentials);
	  if (!StringUtil.isNullString(roles)) {
	    acct.setRoles(roles);
	  }
	} catch (NotAddedException e) {
	  log.error("Can't install user: " + e.getMessage());
	}
      }
    }
  }

  /** Return collection of all user accounts */
  public Collection<UserAccount> getUsers() {
    return accountMap.values();
  }

  /** Return true if named user exists */
  public boolean hasUser(String username) {
    return accountMap.containsKey(username);
  }

  /** Return named UserAccount or null */
  public UserAccount getUserOrNull(String username) {
    UserAccount res = accountMap.get(username);
    log.debug2("getUser("+username + "): " + res);
    return res;
  }

  /** Return named UserAccount or Nobody user */
  public UserAccount getUser(String username) {
    UserAccount res = getUserOrNull(username);
    return res != null ? res : NOBODY_ACCOUNT;
  }

  /** Return parent dir of all user account files */
  public File getAcctDir() {
    return new File(configMgr.getCacheConfigDir(), acctRelDir);
  }

  File ensureAcctDir() {
    File acctDir = getAcctDir();
    if ((!acctDir.exists() && !acctDir.mkdir()) ||
	!acctDir.canWrite()) {
      throw new IllegalArgumentException("Account data directory " +
					 acctDir +
					 " does not exist or cannot be " +
					 "written to.");
    }
    log.debug2("Account dir: " + acctDir);
    return acctDir;
  }

  File[] findUserFiles() {
    File acctDir = getAcctDir();
    File files[] = acctDir.listFiles(new UserAccountFileFilter());
    return files;
  }

  // accept regular files with names that could have been generated by
  // sanitizeName().  This avoids trying to load, e.g., renamed failed
  // deserialization files (*.deser.old)
  private static final class UserAccountFileFilter implements FileFilter {
    public boolean accept(File file) {
      if (!file.isFile()) {
	return false;
      }
      String name = file.getName();
      for (int ix = name.length() - 1; ix >= 0; --ix) {
	char ch = name.charAt(ix);
	if (!Character.isJavaIdentifierPart(ch)) {
	  return false;
	}
      }
      return true;
    }
  }

  void loadUsers() {
    for (File file : findUserFiles()) {
      UserAccount acct = loadUser(file);
      if (acct != null) {
	try {
	  internalAddUser(acct);
	} catch (NotAddedException e) {
	  log.error("Can't install user: " + e.getMessage());
	}
      }
    }
  }

  UserAccount loadUser(File file) {
    try {
      UserAccount acct = (UserAccount)makeObjectSerializer().deserialize(file);
      acct.setFilename(file.getName());
      acct.postLoadInit(this, configMgr.getCurrentConfig());
      log.debug2("Loaded user " + acct.getName() + " from " + file);
      return acct;
    } catch (Exception e) {
      log.error("Unable to load account data from " + file, e);
      return null;
    }
  }

  String generateFilename(UserAccount acct) {
    String name = StringUtil.sanitizeToIdentifier(acct.getName()).toLowerCase();
    File dir = getAcctDir();
    if (!new File(dir, name).exists()) {
      return name;
    }
    for (int ix = 1; ix < 10000; ix++) {
      String s = name + "_" + ix;
      if (!new File(dir, s).exists()) {
	return s;
      }
    }
    throw new RuntimeException("Can't generate unique file to store account: "
			       + acct.getName());
  }

  /** Called by {@link org.lockss.daemon.Cron.SendPasswordReminder} */
  public boolean checkPasswordReminders() {
    for (UserAccount acct : getUsers()) {
      if (!acct.isStaticUser()) {
	acct.checkPasswordReminder();
      }
    }
    return true;
  }

  // Entry points for externally-initiated actions (by UI or other external
  // agent) which should generate audit events.  The first arg is always
  // the UserAccount of the user performing the action.

  /** Add the user account, if doesn't conflict with an existing user and
   * it has a password. */
  public UserAccount userAddUser(UserAccount actor, UserAccount acct)
      throws NotAddedException, NotStoredException {
    UserAccount res = addUser(acct);
    acct.reportCreateEventBy(actor);
    return res;
  }

  /** Store the current state of the user account on disk */
  public void userStoreUser(UserAccount actor, UserAccount acct)
      throws NotStoredException {
    storeUser(acct);
    acct.reportEditEventBy(actor);
  }

  /** Delete the user */
  public  boolean userDeleteUser(UserAccount actor, UserAccount acct) {
    boolean res = deleteUser(acct);
    if (res) {
      acct.reportEventBy(actor, "deleted");
    } else {
      acct.reportEventBy(actor, "failed to delete");
    }
    return res;
  }

  public void auditableEvent(String msg) {
    AlertManager alertMgr = getDaemon().getAlertManager();
    if (alertMgr != null) {
      Alert alert = Alert.cacheAlert(Alert.AUDITABLE_EVENT);
      alertMgr.raiseAlert(alert, msg);
    } else {
      log.warning(msg);
    }
  }

  /** Send an alert email to the owner of the account */
  void alertUser(UserAccount acct, Alert alert, String text) {
    if (!mailEnabled) {
      return;
    }
    try {
      String to = acct.getEmail();
      if (to == null && mailAdminIfNoUserEmail) {
	to = adminEmail;
      }
      if (StringUtil.isNullString(to)) {
	log.warning("Can't find address to send alert: " + alert);
	return;
      }
      alert.setAttribute(Alert.ATTR_EMAIL_TO, to);
      AlertManager alertMgr = getDaemon().getAlertManager();
      alertMgr.raiseAlert(alert, text);
    } catch (Exception e) {
      // ignored, expected during testing
    }
  }

  private ObjectSerializer makeObjectSerializer() {
    return new XStreamSerializer();
  }

  public class NotAddedException extends Exception {
    public NotAddedException(String msg) {
      super(msg);
    }
    public NotAddedException(String msg, Throwable cause) {
      super(msg, cause);
    }
  }

  public class NotStoredException extends Exception {
    public NotStoredException(String msg) {
      super(msg);
    }
    public NotStoredException(String msg, Throwable cause) {
      super(msg, cause);
    }
  }

  public class UserExistsException extends NotAddedException {
    public UserExistsException(String msg) {
      super(msg);
    }
  }
}
