/*
 * $Id: AccountManager.java,v 1.2 2009-06-01 23:38:10 tlipkis Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.config.*;
import org.lockss.servlet.*;
import org.lockss.util.*;

/** Manage user accounts
 */
public class AccountManager
  extends BaseLockssDaemonManager implements ConfigurableManager  {

  protected static Logger log = Logger.getLogger("AccountManager");

  public static final UserAccount NOBODY_ACCOUNT = new NobodyAccount();

  static final String PREFIX = Configuration.PREFIX + "accounts.";

  /** Enable account management */
  static final String PARAM_ENABLED = PREFIX + "enabled";
  static final boolean DEFAULT_ENABLED = false;

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


  // Predefined account policies.  See ConfigManager.setConfigMacros()

  /** <code>LC</code>: SSL, form auth, Library of Congress password rules */
  public static String[] POLICY_LC = {
    PARAM_ENABLED, "true",
    PARAM_NEW_ACCOUNT_TYPE, "LC",
    PARAM_CONDITIONAL_PLATFORM_USER, "true",
    "org.lockss.ui.authType", "Form",
    "org.lockss.ui.debugUser.enable", "false",
    "org.lockss.ui.useSSL", "true",
  };

  /** <code>SSL</code>: SSL, form auth, configurable password rules */
  public static String[] POLICY_SSL = {
    PARAM_ENABLED, "true",
    PARAM_NEW_ACCOUNT_TYPE, "Basic",
    PARAM_CONDITIONAL_PLATFORM_USER, "false",
    "org.lockss.ui.authType", "Form",
    "org.lockss.ui.useSSL", "true",
  };

  /** <code>Form</code>: HTTP, form auth */
  public static String[] POLICY_FORM = {
    PARAM_ENABLED, "true",
    PARAM_NEW_ACCOUNT_TYPE, "Basic",
    PARAM_CONDITIONAL_PLATFORM_USER, "false",
    "org.lockss.ui.authType", "Form",
    "org.lockss.ui.useSSL", "false",
  };

  /** <code>Basic</code>: HTTP, basic auth */
  public static String[] POLICY_BASIC = {
    PARAM_ENABLED, "true",
    PARAM_NEW_ACCOUNT_TYPE, "Basic",
    PARAM_CONDITIONAL_PLATFORM_USER, "false",
    "org.lockss.ui.authType", "Basic",
    "org.lockss.ui.useSSL", "false",
  };

  private boolean isEnabled = DEFAULT_ENABLED;
  private ConfigManager configMgr;
  private String acctRelDir;
  private UserAccount.Factory acctFact;

  // Maps account name to UserAccount
  Map<String,UserAccount> accountMap = new HashMap<String,UserAccount>();

  public void startService() {
    super.startService();
    configMgr = getDaemon().getConfigManager();
    resetConfig();
    isEnabled = configMgr.getCurrentConfig().getBoolean(PARAM_ENABLED,
							DEFAULT_ENABLED);
    if (isEnabled) {
      ensureAcctDir();
      loadUsers();
    }
  }

  public synchronized void stopService() {
    super.stopService();
  }

  public synchronized void setConfig(Configuration config,
				     Configuration prevConfig,
				     Configuration.Differences changedKeys) {

    if (changedKeys.contains(PREFIX)) {
      acctRelDir = config.get(PARAM_ACCT_DIR, DEFAULT_ACCT_DIR);
      acctFact =
	getUserFactory(config.get(PARAM_NEW_ACCOUNT_TYPE,
				     DEFAULT_NEW_ACCOUNT_TYPE));
    }
  }

  public boolean isEnabled() {
    return isEnabled;
  }

  UserAccount.Factory getUserFactory(String type) {
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
    log.warning("Using basic accounts");
    return new BasicUserAccount.Factory();
  }    

  /** Create a new UserAccount of the configured type.  The account must be
   * added before becoming active. */
  public UserAccount createUser(String name) {
    return acctFact.newUser(name, this);
  }

  /** Add the user account, if doesn't conflict with an existing user and
   * it has a password. */
  UserAccount internalAddUser(UserAccount acct) throws NotAddedException {
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
    try {
      return addUser(acct);
    } catch (NotStoredException e) {
      // impossible with static user
      throw new RuntimeException("Shouldn't happen", e);
    }
  }

  /** Add platform user.  If {@link #PARAM_CONDITIONAL_PLATFORM_USER} is
   * true, the user is installed only if no other admin users exist */
  public void installPlatformUser(String platUser, String platPass) {
    if (!StringUtil.isNullString(platUser) &&
	!StringUtil.isNullString(platPass)) {
      if (shouldInstallPlatformUser()) {
	log.info("Installing platform user");
	try {
	  UserAccount acct = addStaticUser(platUser, platPass);
	  acct.setRoles(SetUtil.set(LockssServlet.ROLE_USER_ADMIN));
	} catch (NotAddedException e) {
	  log.error("Can't install platform user", e);
	}
      }
    }
  }

  /** Delete the user */
  public boolean deleteUser(String name) {
    UserAccount acct = getUser(name);
    if (acct == null) {
      return true;
    }
    return deleteUser(acct);
  }

  /** Delete the user */
  public boolean deleteUser(UserAccount acct) {
    boolean res = true;
    String filename = acct.getFilename();
    if (filename != null) {
      File file = new File(getAcctDir(), filename);
      res = file.delete();
    }
    if (res) {
      accountMap.remove(acct.getName());
    }
    return res;
  }

  /** Store the current state of the user account on disk */
  public void storeUser(UserAccount acct) throws NotStoredException {
    if (acct.isStaticUser()) {
      throw new IllegalArgumentException("Can't store static account: " + acct);
    }
    String filename = acct.getFilename();
    if (filename == null) {
      filename = generateFilename(acct);
    }
    File file =  new File(getAcctDir(), filename);
    try {
      storeUser(acct, file);
    } catch (SerializationException e) {
      throw new NotStoredException("Error storing user in databasee", e);
    } catch (IOException e) {
      throw new NotStoredException("Error storing user in databasee", e);
    }

    acct.setFilename(file.getName());
  }

  void storeUser(UserAccount acct, File file)
      throws IOException, SerializationException {
    if (acct.isStaticUser()) {
      throw new IllegalArgumentException("Can't store static account: " + acct);
    }
    if (getUser(acct.getName()) != acct) {
      throw new IllegalArgumentException("Can't store uninstalled account: "
					 + acct);
    }
    if (acct.isChanged()) {
      if (log.isDebug2()) log.debug2("Storing account in " + file);
      makeObjectSerializer().serialize(file, acct);
      acct.notChanged();
      return;
    }
  }

  boolean shouldInstallPlatformUser() {
    if (!CurrentConfig.getBooleanParam(PARAM_CONDITIONAL_PLATFORM_USER,
				       DEFAULT_CONDITIONAL_PLATFORM_USER)) {
      return true;
    }
    for (UserAccount acct : getAccounts()) {
      if (!acct.isStaticUser()
	  && acct.isUserInRole(LockssServlet.ROLE_USER_ADMIN)) {
	return false;
      }
    }
    return true;
  }

  /** Load realm users from properties file.
   * The property file maps usernames to password specs followed by
   * an optional comma separated list of role names.
   *
   * @param config Filename or url of user properties file.
   * @exception IOException
   */
  public void loadFromProps(String propsUrl) throws IOException {
    if(log.isDebug())log.debug("Load "+this+" from "+propsUrl);
    Properties properties = new Properties();
    InputStream ins = getClass().getResourceAsStream(propsUrl);
    properties.load(ins);

    for (Map.Entry ent : properties.entrySet()) {
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
  public Collection<UserAccount> getAccounts() {
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
//     return res != null ? res : NOBODY_ACCOUNT;
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

  /** Should be called whenever a change has been made to the account, in a
   * context where AccountManager doesn't otherwise know to save it */
  public void notifyAccountChange(UserAccount acct, String msg) {
    try {
      storeUser(acct);
    } catch (NotStoredException e) {
      log.error("Failed to store account: " + acct.getName(), e);
    }
    // if msg != null send mail 
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
    File files[] = acctDir.listFiles();
    return files;
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
      return acct;
    } catch (Exception e) {
      log.error("Unable to load account data from " + file, e);
      return null;
    }
  }

  String sanitizeName(String name) {
    name = name.toLowerCase();
    StringBuilder sb = new StringBuilder();
    for (int ix = 0; ix < name.length(); ix++) {
      char ch = name.charAt(ix);
      if (Character.isJavaIdentifierPart(ch)) {
	sb.append(ch);
      }
    }
    return sb.toString();
  }

  String generateFilename(UserAccount acct) {
    String name = sanitizeName(acct.getName());
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
