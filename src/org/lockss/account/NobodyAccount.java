/*
 * $Id$
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

/** Least privileged user
 */
public class NobodyAccount extends UserAccount {

  public NobodyAccount() {
    this("Nobody");
  }

  @JsonCreator
  public NobodyAccount(@JsonProperty("userName") String name) {
    super(name);
  }

  public String getType() {
    return USER_ACCOUNT_TYPE;
  }

  public static final String USER_ACCOUNT_TYPE = "Nobody";

  @Override
  public boolean isEnabled() {
    return false;
  }

  protected int getMinPasswordLength() {
    return -1;
  }

  protected int getHistorySize() {
    return -1;
  }

  protected long getMinPasswordChangeInterval() {
    return -1;
  }

  protected long getMaxPasswordChangeInterval() {
    return -1;
  }

  protected long getPasswordChangeReminderInterval() {
    return -1;
  }

  public long getInactivityLogout() {
    return -1;
  }

  protected int getMaxFailedAttempts() {
    return -1;
  }

  protected long getFailedAttemptWindow() {
    return -1;
  }

  protected long getFailedAttemptResetInterval() {
    return -1;
  }

  protected String getDefaultHashAlgorithm() {
    return null;
  }

  @Override
  public String getRoles() {
    return "";
  }

  @Override
  public Set getRoleSet() {
    return Collections.EMPTY_SET;
  }
}
