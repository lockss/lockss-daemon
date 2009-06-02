/*
 * $Id: BasicUserAccount.java,v 1.2 2009-06-02 07:10:22 tlipkis Exp $
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

import org.lockss.config.*;

/** User account data with parameters for standard rules set from config
 */
public class BasicUserAccount extends UserAccount {

  static final String PREFIX = AccountManager.PREFIX;

  /** Default password hash algorithm */
  static final String PARAM_HASH_ALGORITHM = PREFIX + "hashAlgorithm";
  static final String DEFAULT_HASH_ALGORITHM = "SHA-256";

  /** Mininum password length */
  static final String PARAM_MIN_PASSWORD_LENGTH = PREFIX + "minPasswordLength";
  static final int DEFAULT_MIN_PASSWORD_LENGTH = 6;

  /** Users may not change their own password more often than this */
  static final String PARAM_MIN_PASSWORD_CHANGE_INTERVAL =
    PREFIX + "minPasswordChangeInterval";
  static final long DEFAULT_MIN_PASSWORD_CHANGE_INTERVAL = -1;

  /** Users must change  their password at least this often */
  static final String PARAM_MAX_PASSWORD_CHANGE_INTERVAL =
    PREFIX + "maxPasswordChangeInterval";
  static final long DEFAULT_MAX_PASSWORD_CHANGE_INTERVAL = -1;

  /** Users are reminded to change their password this long before it
   * expires */
  static final String PARAM_PASSWORD_CHANGE_REMINDER_INTERVAL =
    PREFIX + "passwordChangeReminderInterval";
  static final long DEFAULT_PASSWORD_CHANGE_REMINDER_INTERVAL = -1;

  /** Users are logged out after this much inactivity */
  static final String PARAM_INACTIVITY_LOGOUT = PREFIX + "inactivityLogout";
  static final long DEFAULT_INACTIVITY_LOGOUT = -1;

  /** May not reuse this many previous passwords */
  static final String PARAM_PASSWORD_HISTORY_SIZE =
    PREFIX + "passwordHistorySize";
  static final int DEFAULT_PASSWORD_HISTORY_SIZE = -1;

  /** Number of consecutive failed password attempts after which the
   * account is disabled */
  static final String PARAM_MAX_FAILED_ATTEMPTS = PREFIX + "maxFailedAttempts";
  static final int DEFAULT_MAX_FAILED_ATTEMPTS = -1;

  private transient Configuration config;

  public BasicUserAccount(String name) {
    super(name);
  }

  protected void postLoadInit(AccountManager acctMgr, Configuration config) {
    this.config = config;
    super.postLoadInit(acctMgr, config);
  }

  public String getType() {
    return "Basic";
  }

  protected int getMinPasswordLength() {
    return config.getInt(PARAM_MIN_PASSWORD_LENGTH,
			 DEFAULT_MIN_PASSWORD_LENGTH);
  }

  protected int getHistorySize() {
    return config.getInt(PARAM_PASSWORD_HISTORY_SIZE,
			 DEFAULT_PASSWORD_HISTORY_SIZE);
  }

  protected long getMinPasswordChangeInterval() {
    return config.getTimeInterval(PARAM_MIN_PASSWORD_CHANGE_INTERVAL,
				  DEFAULT_MIN_PASSWORD_CHANGE_INTERVAL);
  }

  protected long getMaxPasswordChangeInterval() {
    return config.getTimeInterval(PARAM_MAX_PASSWORD_CHANGE_INTERVAL,
				  DEFAULT_MAX_PASSWORD_CHANGE_INTERVAL);
  }

  protected long getPasswordChangeReminderInterval() {
    return config.getTimeInterval(PARAM_PASSWORD_CHANGE_REMINDER_INTERVAL,
				  DEFAULT_PASSWORD_CHANGE_REMINDER_INTERVAL);
  }

  public long getInactivityLogout() {
    return config.getTimeInterval(PARAM_INACTIVITY_LOGOUT,
				  DEFAULT_INACTIVITY_LOGOUT);
  }

  protected int getMaxFailedAttempts() {
    return config.getInt(PARAM_MAX_FAILED_ATTEMPTS,
			 DEFAULT_MAX_FAILED_ATTEMPTS);
  }

  protected String getDefaultHashAlgorithm() {
    return config.get(PARAM_HASH_ALGORITHM, DEFAULT_HASH_ALGORITHM);
  }


  public static class Factory extends UserAccount.Factory {
    public UserAccount newUser(String name, AccountManager acctMgr,
			       Configuration config) {
      if (config == null) {
	throw new NullPointerException();
      }
      BasicUserAccount acct = new BasicUserAccount(name);
      acct.init(acctMgr, config);
      return acct;
    }
  }
}
