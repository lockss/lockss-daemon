/*
 * $Id: LCUserAccount.java,v 1.2.2.2 2009-06-13 08:51:56 tlipkis Exp $
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

import org.apache.oro.text.regex.*;

import org.lockss.config.*;
import org.lockss.util.*;

/** Library of Congress password rules:
 *
 * - The system must enforce a minimum password length of 8 characters for
 *   user passwords.
 *
 * - The system must enforce at least three (3) of the following password
 *   complexity rules for user passwords: (a) at least 1 upper case
 *   alphabetic character, (b) at least 1 lower case alphabetic character,
 *   (c) at least 1 numeric character, (d) at least 1 special character.
 *
 * - The system must prevent passwords with consecutive repeated characters
 *   for user accounts.
 *
 * - The system must prevent the reuse of the 11 most recently used
 *   passwords for a particular user account for user accounts.
 *
 * - The system must prevent the user from changing his or her password
 *   more than one time per day.
 *
 * - The system must ensure that user passwords expire if not changed every
 *   sixty (60) days with the exception of Public E-Authentication Level 1
 *   or 2 systems, per NIST SP 800-63, which ensure that user passwords
 *   expire if not changed annually.
 *
 * - The system must provide a warning message seven (7) days before the
 *   user password expires
 */

public class LCUserAccount extends UserAccount {

  static final long MIN_PASSWORD_CHANGE_INTERVAL = 1 * Constants.DAY;
  static final long MAX_PASSWORD_CHANGE_INTERVAL = 60 * Constants.DAY;
  static final long PASSWORD_CHANGE_REMINDER_INTERVAL = 7 * Constants.DAY;

  static final long INACTIVITY_LOGOUT = 15 * Constants.MINUTE;
  static final int HISTORY_SIZE = 11;
  static final int MAX_FAILED_ATTEMPTS = 3;
  static final long MAX_FAILED_ATTEMPT_WINDOW = 15 * Constants.MINUTE;
  static final long MAX_FAILED_ATTEMPT_RESET_INTERVAL = 15 * Constants.MINUTE;

  static final String HASH_ALGORITHM = "SHA-256";

  public LCUserAccount(String name) {
    super(name);
  }

  public String getType() {
    return "LC";
  }

  protected int getHistorySize() {
    return HISTORY_SIZE;
  }

  protected String getDefaultHashAlgorithm() {
    return HASH_ALGORITHM;
  }

  public long getInactivityLogout() {
    return INACTIVITY_LOGOUT;
  }

  protected int getMinPasswordLength() {
    return 8;
  }

  protected long getMinPasswordChangeInterval() {
    return MIN_PASSWORD_CHANGE_INTERVAL;
  }

  protected long getMaxPasswordChangeInterval() {
    return MAX_PASSWORD_CHANGE_INTERVAL;
  }

  protected long getPasswordChangeReminderInterval() {
    return PASSWORD_CHANGE_REMINDER_INTERVAL;
  }

  protected int getMaxFailedAttempts() {
    return MAX_FAILED_ATTEMPTS;
  }

  protected long getFailedAttemptWindow() {
    return MAX_FAILED_ATTEMPT_WINDOW;
  }

  protected long getFailedAttemptResetInterval() {
    return MAX_FAILED_ATTEMPT_RESET_INTERVAL;
  }

  /** Return the hash algorithm to be used for new accounts */

  @Override
  protected void checkLegalPassword(String newPwd, String hash, boolean isAdmin)
      throws IllegalPasswordChange {
    super.checkLegalPassword(newPwd, hash, isAdmin);

    if (StringUtil.hasRepeatedChar(newPwd)) {
      throw new IllegalPassword("Password may not contain consecutive repeated characters");
    }
    if (!hasRequiredCharacterMix(newPwd)) {
      throw new IllegalPassword("Password must contain mix of upper, lower, numeric and special characters");
    }
  }

  static Pattern[] charPats = {
    RegexpUtil.uncheckedCompile("[a-z]", Perl5Compiler.READ_ONLY_MASK),
    RegexpUtil.uncheckedCompile("[A-Z]", Perl5Compiler.READ_ONLY_MASK),
    RegexpUtil.uncheckedCompile("[0-9]", Perl5Compiler.READ_ONLY_MASK),
    RegexpUtil.uncheckedCompile("[" + SPECIAL_CHARS + "]",
				Perl5Compiler.READ_ONLY_MASK)
  };

  boolean hasRequiredCharacterMix(String str) {
    // Must have at least 3 of: upper alpha, lower alpha, numeric, special
    int cnt = 0;
    Perl5Matcher matcher = RegexpUtil.getMatcher();
    for (Pattern pat : charPats) {
      if (matcher.contains(str, pat)) {
	cnt++;
      }
    }
    return cnt >= 3;
  }

  public static class Factory extends UserAccount.Factory {
    public UserAccount newUser(String name,
			       AccountManager acctMgr,
			       Configuration config) {
      UserAccount acct = new LCUserAccount(name);
      acct.init(acctMgr, config);
      return acct;
    }
  }
}
