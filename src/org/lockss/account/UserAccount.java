/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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
import org.apache.oro.text.regex.*;
import org.apache.commons.lang3.*;
import org.apache.commons.lang3.time.*;
import org.mortbay.util.Credential;

import javax.servlet.http.*;

import org.lockss.config.*;
import org.lockss.util.*;
import org.lockss.jetty.*;
import org.lockss.alert.*;
import org.lockss.app.*;
import org.lockss.servlet.*;

/** User account data.
 */
public abstract class UserAccount implements LockssSerializable, Comparable {
  
  private static final Logger log = Logger.getLogger(UserAccount.class);

  // XXX enhance test/src/org/lockss/account/TestLCUserAccount.java

  private static FastDateFormat expireDf =
    FastDateFormat.getInstance("EEE dd MMM, HH:mm zzz");

  private static Map<HttpSession,UserAccount> active = new HashMap();
  private static TimerQueue.Request alerter = null;
  protected final String userName;
  protected int version;
  protected String email;
  protected String currentPassword;
  protected String[] passwordHistory;
  protected long[] failedAttemptHistory;
  protected String hashAlg;
  protected String roles;
  protected long lastLogin;
  // most recent password change
  protected long lastPasswordChange;
  // most recent password change by the user (used only for too-frequent
  // password change check
  protected long lastUserPasswordChange;
  protected long lastPasswordReminderTime;
  protected boolean isDisabled;
  protected String disableReason;

  protected static final long ALERTER_INTERVAL = Constants.MINUTE;

  protected transient AccountManager acctMgr;
  protected transient String fileName;

  protected transient Set roleSet = null;
  protected transient Credential credential = null;
  protected transient boolean isChanged = false;
  protected transient StringBuilder eventsToReport;

  public UserAccount(String name) {
    this.userName = name;
  }

  /** Setup configuration before first use.  Called by factory. */
  protected void init(AccountManager acctMgr, Configuration config) {
    commonInit(acctMgr, config);
    init(acctMgr);
  }

  /** Setup configuration before first use.  Called by factory. */
  protected void init(AccountManager acctMgr) {
    if (getHistorySize() > 0) {
      this.passwordHistory = new String[getHistorySize()];
    }
    if (getMaxFailedAttempts() > 0) {
      this.failedAttemptHistory = new long[getMaxFailedAttempts()];
    }
    hashAlg = getDefaultHashAlgorithm();
    lastPasswordChange = -1;
    lastUserPasswordChange = -1;
    version = 2;
  }

  /** Called after loading from file, not called by factory.
   * If not already version 2, update to version 2 and add
   * LockssServlet#ROLE_CONTENT_ACCESS} if necessary.
   */
  protected void postLoadInit(AccountManager acctMgr, Configuration config) {
    commonInit(acctMgr, config);
    if (version < 2) {
      Set r = new HashSet(getRoleSet());
      if (r.add(LockssServlet.ROLE_CONTENT_ACCESS)) {
	log.debug("Adding accessContentRole to " + getName());
	setRoles(r);
      }
      log.debug("Updating " + getName() + " to version 2");
      version = 2;
      storeUser(true);
    }
  }

  /** Setup configuration before first use.  Called by factory. */
  protected void commonInit(AccountManager acctMgr, Configuration config) {
    this.acctMgr = acctMgr;
  }

  /** Return the username */
  public String getName() {
    return userName;
  }

  /** Set the email address */
  public void setEmail(String val) {
    if (!StringUtil.equalStrings(email, val)) {
      addAuditableEvent("Changed email from: " + none(email) +
			" to: " + none(val)); 
      setChanged(true);
      email = val;
    }
  }

  /** Return the email address */
  public String getEmail() {
    return email;
  }

  /** Set the filename */
  void setFilename(String val) {
    fileName = val;
  }

  /** Get the filename */
  String getFilename() {
    return fileName;
  }

  /** Return the hash algorithm name */
  public String getHashAlgorithm() {
    return hashAlg;
  }

  /** Get the time the user last logged in */
  public long getLastLogin() {
    return lastLogin;
  }

  public boolean isEditable() {
    return !isStaticUser();
  }

  public boolean isStaticUser() {
    return false;
  }

  // Must be implemented by subclasses

  /** Return the account type */
  abstract public String getType();

  /** Return the minimum password length */
  abstract protected int getMinPasswordLength();
  /** Return the number of previous passwords that may not be reused */
  abstract protected int getHistorySize();
  /** Return the shortest interval before which a user may again change his
   * password */
  abstract protected long getMinPasswordChangeInterval();
  /** Return the interval before which a user must change his password */
  abstract protected long getMaxPasswordChangeInterval();
  /** Return the interval before password expiration when a password
   * reminder message should be generated */
  abstract protected long getPasswordChangeReminderInterval();
  /** Return the amount of time after which an inactive user must re-login */
  abstract public long getInactivityLogout();
  /** Number of consecutive failed password attempts after which the
   * account is disabled */
  abstract protected int getMaxFailedAttempts();
  /** Interval within which consecutive failed attempts are counted. */
  abstract protected long getFailedAttemptWindow();
  /** Time after last failed attempt that disabled account is reenabled */
  abstract protected long getFailedAttemptResetInterval();
  /** Return the hash algorithm to be used for new accounts */
  abstract protected String getDefaultHashAlgorithm();

  // Roles

  /** Set the user's roles */
  public void setRoles(String val) {
    setRoles(val, false);
  }

  /** Set the user's roles */
  public void setRoles(String val, boolean isInteractive) {
    if (log.isDebug2()) log.debug2(userName + ".setRoles(" + val + ")");
    if (!StringUtil.equalStrings(roles, val)) {
      addAuditableEvent("Changed roles from: " + none(roles) +
			" to: " + none(val));
      setChanged(true);
      roles = val;
      roleSet = null;
    }
  }

  private String none(String s) {
    return StringUtil.isNullString(s) ? "(none)" : s;
  }

  /** Set the user's roles */
  public void setRoles(Set roleSet) {
    if (log.isDebug2()) log.debug2(userName + ".setRoles(" + roleSet + ")");
    if (!roleSet.equals(getRoleSet())) {
      addAuditableEvent("Changed roles from: " + getRoleSet() +
			" to: " + roleSet);
      setChanged(true);
      this.roles = StringUtil.separatedString(roleSet, ",");
      this.roleSet = roleSet;
    }
  }

  /** Return the user's roles as a string */
  public String getRoles() {
    return roles == null ? "" : roles;
  }

  /** Return true if the user has the named role  */
  public boolean isUserInRole(String role) {
    return getRoleSet().contains(role);
  }

  /** Return a collection of the user's roles  */
  public Set getRoleSet() {
    if (roleSet == null) {
      roleSet =
	SetUtil.theSet(StringUtil.breakAt(getRoles(), ',', -1, true, true));
    }
    return roleSet;
  }

  // Password management

  /** Return the encrypted password */
  public String getPassword() {
    return currentPassword;
  }

  /** Return the encrypted password */
  public boolean hasPassword() {
    return credential != null || currentPassword != null;
  }

  /** Get the time the password was last changed */
  public long getLastPasswordChange() {
    return lastPasswordChange;
  }

  /** Get the time the password was last changed by the user */
  public long getLastUserPasswordChange() {
    return lastUserPasswordChange;
  }

  /** Change the password
   * @throws IllegalPassword if the new password is not legal
   * @throws IllegalPasswordChange
   */
  public void setPassword(String newPwd) throws IllegalPasswordChange {
    setPassword(newPwd, false);
  }

  public void setPassword(String newPwd, boolean isAdmin)
      throws IllegalPasswordChange {
    String hash = hashPassword(newPwd);
    checkLegalPassword(newPwd, hash, isAdmin);
    if (currentPassword != null && passwordHistory != null) {
      shiftArrayUp(passwordHistory);
      passwordHistory[0] = currentPassword;
    }
    boolean isChange = (currentPassword != null
			&& !currentPassword.equals(hash));

    currentPassword = hash;
    lastPasswordChange = TimeBase.nowMs();
    if (isAdmin) {
      lastUserPasswordChange = -1;
    } else {
      lastUserPasswordChange = lastPasswordChange;
    }
    boolean isReenable = isDisabled;
    enable();
    setChanged(true);
    clearCaches();
    if (isChange) {
      addAuditableEvent("Changed password" +
			(isReenable ? " and reenabled" : ""));
    }
  }

  /** Account has logged in */
  public void loggedIn(HttpSession session) {
    synchronized (active) {
      if (alerter == null) {
	if (active.size() != 0) {
	  log.error("alerter null but " + active.size() + " active");
	}
	LockssDaemon daemon = acctMgr.getDaemon();
	AdminServletManager adminMgr = null;
	try {
	  adminMgr =
	    (AdminServletManager)daemon.getManager(LockssDaemon.SERVLET_MANAGER);
	} catch (IllegalArgumentException e) {
	  log.warning("No AdminServletManager, not installing Alerter task");
	  return;
	}
	// Start alerting task
	alerter = TimerQueue.schedule(Deadline.in(ALERTER_INTERVAL),
				      ALERTER_INTERVAL, new Alerter(), adminMgr);
      }
      if (!active.containsKey(session)) {
	active.put(session, this);
	auditableEvent("logged in");
      } else {
	log.debug("Redundant nowAuthenticated()");
      }
    }
  }

  /** Account has logged out */
  public void loggedOut(HttpSession session) {
    synchronized (active) {
      if (active.containsKey(session)) {
	active.remove(session);
	auditableEvent("logged out");
	if (alerter != null) {
	  if (active.size() == 0) {
	    TimerQueue.cancel(alerter);
	    alerter = null;
	  }
	} else {
	  log.error("loggedOut but alerter null");
	}
      } else {
	log.error("loggedOut() but not active: " + this);
      }
    }
  }

  public synchronized void addAuditableEvent(String text) {
    if (eventsToReport == null) {
      eventsToReport = new StringBuilder();
    }
    eventsToReport.append(text);
    eventsToReport.append("\n");
  }

  public void reportEventBy(UserAccount editor, String verbed) {
    reportEventBy(editor, verbed, null);
  }

  public void reportEventBy(UserAccount editor, String verbed, String body) {
    StringBuilder sb = new StringBuilder();
    sb.append("was ");
    sb.append(verbed);
    sb.append(" by user ");
    sb.append(editor.getName());
    if (!StringUtil.isNullString(body)) {
      sb.append(":\n\n");
      sb.append(body);
    }
    auditableEvent(sb.toString());
  }


  public void reportEditEventBy(UserAccount editor) {
    if (eventsToReport != null) {
      reportEventBy(editor, "edited",
		    "User: " + getName() + "\n" +
		    eventsToReport.toString());
      eventsToReport = null;
    }
  }

  public void reportCreateEventBy(UserAccount editor) {
      reportEventBy(editor, "created",
		    "User: " + getName() + "\n" +
		    "Email: " + none(getEmail()) + "\n" +
		    "Roles: " + none(getRoles()) + "\n");
    eventsToReport = null;
  }

  public void auditableEvent(String text) {
    if (acctMgr != null) {
      AlertManager alertMgr = acctMgr.getDaemon().getAlertManager();
      String msg = "User " + userName + " " + text;
      if (alertMgr != null) {
	Alert alert = Alert.cacheAlert(Alert.AUDITABLE_EVENT);
	alertMgr.raiseAlert(alert, msg);
      } else {
	log.warning(msg);
      }
    }
  }

  void shiftArrayUp(String[] array) {
    System.arraycopy(array, 0, array, 1, array.length-1);
  }

  void shiftArrayUp(long[] array) {
    System.arraycopy(array, 0, array, 1, array.length-1);
  }

  String hashPassword(String pwd) throws IllegalPassword {
    if (pwd == null) {
      throw new IllegalPassword("Password may not be empty");
    }
    try {
      MessageDigest md = MessageDigest.getInstance(hashAlg);
      md.update(pwd.getBytes(Constants.DEFAULT_ENCODING));

      return ByteArray.toHexString(md.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Unsupported hash algorithm: " + hashAlg);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Unsupported Encoding: " +
				 Constants.DEFAULT_ENCODING +
				 " (shouldn't happen)");
    }
  }

  protected void checkLegalPassword(String newPwd, String hash, boolean isAdmin)
      throws IllegalPasswordChange {
    if (!isAdmin && lastUserPasswordChange > 0 &&
	(TimeBase.msSince(lastUserPasswordChange)
	 < getMinPasswordChangeInterval())) {
      String msg = "Cannot change password more than once every "
	+ StringUtil.timeIntervalToLongString(getMinPasswordChangeInterval());
      throw new IllegalPasswordChange(msg);
    }

    if (newPwd == null || newPwd.length() < getMinPasswordLength()) {
      throw new IllegalPassword("Password must be at least "
				+ getMinPasswordLength() + " characters");
    }
    if (hasIllegalCharacter(newPwd)) {
      throw new IllegalPassword("Password must contain only ascii alphanumeric and special chars");
    }
    if ((currentPassword != null && currentPassword.equals(hash))) {
      throw new IllegalPassword("May not repeat previous password");
    } else if (getHistorySize() > 0
	       && ArrayUtils.contains(passwordHistory, hash)) {
      throw new IllegalPassword("May not repeat any of the previous "
				+ passwordHistory.length + " passwords");
    }
  }

  static String SPECIAL_CHARS = "!@#$%^&*()+=:;_<>,.?/{}~'`\\-\\|\\\\\"\\[\\]";

  static Pattern passwdCharPat =
    RegexpUtil.uncheckedCompile("[^a-zA-Z0-9" + SPECIAL_CHARS + "]",
				Perl5Compiler.READ_ONLY_MASK);

  boolean hasIllegalCharacter(String str) {
    Perl5Matcher matcher = RegexpUtil.getMatcher();
    return matcher.contains(str, passwdCharPat);
  }

  // Password rotation, expiration

  public boolean isPasswordExpired() {
    return isPasswordExpired(getMaxPasswordChangeInterval());
  }

  private long getPasswordExpiration() {
    return lastPasswordChange + getMaxPasswordChangeInterval();
  }

  private boolean isPasswordExpired(long expireInterval) {
    boolean res = lastPasswordChange > 0 && expireInterval > 0
      && TimeBase.nowMs() >= getPasswordExpiration();
    return res;
  }

  public long getLastPasswordReminderTime() {
    return lastPasswordReminderTime;
  }

  public void setLastPasswordReminderTime(long val) {
    lastPasswordReminderTime = val;
  }

  public void checkPasswordReminder() {
    long expireInterval = getMaxPasswordChangeInterval();
    // Do nothing if no password expiration
    if (expireInterval < 0) return;
    // If expired and haven't sent mail since it expired, send now
    if (isPasswordExpired()) {
      if (lastPasswordReminderTime < getPasswordExpiration()) {
	// send now disabled
	alertAndUpdate(Alert.cacheAlert(Alert.ACCOUNT_DISABLED),
		       "User '" + getName()
		       + "' disabled because password has expired.");
      }
    } else {
      long reminderInterval = getPasswordChangeReminderInterval();
      if (reminderInterval > 0) {
	// If reminders are enabled ...
	long reminderTime =
	  lastPasswordChange + expireInterval - reminderInterval;
	// If past reminder time and haven't sent mail since reminder time,
	// send reminder now
	if (TimeBase.nowMs() >= reminderTime
	    && lastPasswordReminderTime < reminderTime) {
	  alertAndUpdate(Alert.cacheAlert(Alert.PASSWORD_REMINDER),
			 "The password for user '" + getName()
			 + "' will expire at "
			 + expireDf.format(lastPasswordChange
					   + expireInterval)
			 + ".  Please change it before then.");
	}
      }
    }
  }

  // Credentials

  /** Set the credential to a string of the form HASH-ALG:encrypted-passwd */
  public void setCredential(String cred) throws NoSuchAlgorithmException {
    if (credential != null) {
      throw new UnsupportedOperationException("Can't reset credential");
    }
    credential = MDCredential.makeCredential(cred);
    setChanged(true);
  }

  /** Return the credential string (ALG:encrypted_pwd) */
  String getCredentialString() {
    if (currentPassword == null) {
      return null;
    }
    return hashAlg + ":" + currentPassword;
  }

  /** Return the credential string (ALG:encrypted_pwd */
  public Credential getCredential() {
    if (credential == null) {
      String credString = getCredentialString();
      if (credString == null) {
	return null;
      }
      try {
	credential = MDCredential.makeCredential(credString);
	if (log.isDebug2()) log.debug2("Made credential for "
				       + ": " + credential);
      } catch (NoSuchAlgorithmException e) {
	log.error("No credential; account disabled: " + getName(), e);
	credential = new NullCredential();
      } catch (RuntimeException e) {
	log.error("No credential; account disabled: " + getName(), e);
	credential = new NullCredential();
      }
    }
    return credential;
  }

  /** Check the credentials against this account's password. */
  public boolean check(Object credentials) {
    if (!isEnabled()) {
      return false;
    }
    Credential cred = getCredential();
    if (cred == null) {
      return false;
    }
    boolean res = cred.check(credentials);
    if (res) {
      handleSuccessfulLoginAttempt();
    } else {
      handleFailedLoginAttempt();
    }
    return res;
  }

  /** Respond appropriately to failed login attempt.  Default action is to
   * possibly disable if too many repeated failures within specified time
   * window */
  protected void handleFailedLoginAttempt() {
    if (failedAttemptHistory != null) {
      shiftArrayUp(failedAttemptHistory);
      failedAttemptHistory[0] = TimeBase.nowMs();
      storeUser();
    }
  }

  /** Respond appropriately to successful login attempt.  Default action is
   * to record the last login time */
  protected void handleSuccessfulLoginAttempt() {
    // Frequent AJAX requests cause significant time to be spent in
    // storeUser().  Hard to discern request context at this level so
    // just avoid too-frequent updates.
    if (TimeBase.msSince(lastLogin) > 10 * Constants.MINUTE) {
      lastLogin = TimeBase.nowMs();
      storeUser();
    }
  }

  private void clearCaches() {
    credential = null;
  }

  public void enable() {
    setChanged(isDisabled);
    isDisabled = false;
    disableReason = null;
  }

  public void disable(String reason) {
    setChanged(!isDisabled || !StringUtil.equalStrings(disableReason, reason));
    log.debug("Disabled account " + getName() + ": " + reason);
    if (!AccountManager.DELETED_REASON.equals(reason)) {
      auditableEvent("account disabled because: " + reason);
    }
    isDisabled = true;
    disableReason = reason;
  }

  public boolean isEnabled() {
    if (isDisabled) {
      return false;
    }
    if (isPasswordExpired()) {
      return false;
    }
    if (isExcessiveFailedAttempts()) {
      return false;
    }
    return true;
  }

  public String getDisabledMessage() {
    if (isEnabled()) {
      return null;
    }
    if (disableReason != null) {
      return disableReason;
    }
    if (isPasswordExpired()) {
      return "Disabled: Password has expired";
    }
    if (isExcessiveFailedAttempts()) {
      return ("Disabled: "
	      + failedAttemptHistory.length
	      + " failed login attempts at "
	      + expireDf.format(failedAttemptHistory[0]));
    }
    return "Disabled";
  }

  /** true iff a max repeated login failure has been set, the time between
   * the nth previous and most recent failure is less than the failed
   * attempt window, and the time since the most recent failed is less than
   * the failed attempt reset time */
  protected boolean isExcessiveFailedAttempts() {
    long window = getFailedAttemptWindow();
    return
      (window > 0
       && failedAttemptHistory != null
       && ( ( failedAttemptHistory[0]
	      - failedAttemptHistory[failedAttemptHistory.length - 1])
	    <= window)
       && ( TimeBase.msSince(failedAttemptHistory[0])
	    < getFailedAttemptResetInterval()));
  }

  void alertAndUpdate(Alert alert, String msg) {
    acctMgr.alertUser(this, alert, msg);
    lastPasswordReminderTime = TimeBase.nowMs();
    storeUser();
  }

  /** Should be called whenever a change has been made to the account, in a
   * context where AccountManager doesn't otherwise know to save it */
  public void storeUser() {
    storeUser(false);
  }

  /** Should be called whenever a change has been made to the account, in a
   * context where AccountManager doesn't otherwise know to save it */
  public void storeUser(boolean internal) {
    setChanged(true);
    try {
      if (internal) {
	acctMgr.storeUserInternal(this);
      } else {
	acctMgr.storeUser(this);
      }
    } catch (AccountManager.NotStoredException e) {
      log.error("Failed to store account: " + getName(), e);
    }
  }

  public boolean isChanged() {
    return isChanged;
  }

  public void notChanged() {
    isChanged = false;
  }

  void setChanged(boolean changed) {
    isChanged |= changed;
  }

  public int compareTo(Object o) {
    return compareTo((UserAccount)o);
  }

  public int compareTo(UserAccount other) {
    return getName().compareTo(other.getName());
  }

  public class NullCredential extends Credential {
    public boolean check(Object credentials) {
      return false;
    }
  }

  public class IllegalPasswordChange extends Exception {
    IllegalPasswordChange(String reason) {
      super(reason);
    }
  }

  public class IllegalPassword extends IllegalPasswordChange {
    IllegalPassword(String reason) {
      super(reason);
    }
  }

  public static abstract class Factory {
    public abstract UserAccount newUser(String name,
					AccountManager acctMgr,
					Configuration config);
    public UserAccount newUser(String name, AccountManager acctMgr) {
      return newUser(name, acctMgr, ConfigManager.getCurrentConfig());
    }
  }

  public class Alerter implements TimerQueue.Callback {

    public void timerExpired(Object cookie) {
      AdminServletManager adminMgr = (AdminServletManager)cookie;
      log.debug3("timerExpierd");
      if (adminMgr == null) {
	log.error("Null AdminServletManager cookie");
	return;
      }
      Collection zombies = adminMgr.getZombieSessions();
      log.debug3("Zombies: " + zombies.size());
      for (Iterator it = zombies.iterator(); it.hasNext(); ) {
	HttpSession sess = (HttpSession)it.next();
	boolean auth = LockssSessionManager.isAuthenticated(sess);
	if (auth && LockssSessionManager.isInactiveTimeout(sess)) {
	  UserAccount acct = LockssSessionManager.getUserAccount(sess);
	  if (acct != null) {
	    log.debug3("About to log out zombie: " + sess);
	    LockssFormAuthenticator.logout(sess, "Logged out for inactivity");
	  } else {
	    log.debug3("Zombie with null acct: " + sess);
	  }
	} else {
	  log.error("Inactive zombie: " + sess + " auth " + auth +
		    " timeout " + LockssSessionManager.isInactiveTimeout(sess));
	}
      }
    }
  }
}
