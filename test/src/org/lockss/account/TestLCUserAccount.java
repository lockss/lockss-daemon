/*
 * $Id: TestLCUserAccount.java,v 1.4 2009-06-15 07:51:09 tlipkis Exp $
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

import org.lockss.account.UserAccount.IllegalPassword;
import org.lockss.account.UserAccount.IllegalPasswordChange;

import junit.framework.TestCase;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;

import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.alert.*;

/**
 * Test class for org.lockss.account.LCUserAccount
 */
public class TestLCUserAccount extends LockssTestCase {

  static String NAME1 = "User1";
  static int HIST_LEN = 5;

  LCUserAccount acct1;
  MyAccountManager acctMgr;

  public void setUp() throws Exception {
    super.setUp();
    acctMgr = new MyAccountManager();
    getMockLockssDaemon().setAccountManager(acctMgr);
    acct1 = (LCUserAccount)new LCUserAccount.Factory().newUser(NAME1, acctMgr);
  }
  
  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testFactory() {
    assertTrue(acct1 instanceof LCUserAccount);
  }

  public void testRoles() {
    assertEquals("", acct1.getRoles());
    assertEquals(Collections.EMPTY_SET, SetUtil.theSet(acct1.getRoleSet()));
    acct1.setRoles("FooRole,BarRole , LobsterRoll ");
    assertEquals(SetUtil.set("FooRole", "BarRole", "LobsterRoll"),
		 SetUtil.theSet(acct1.getRoleSet()));
    assertEquals("FooRole,BarRole , LobsterRoll ", acct1.getRoles());
  }

  public void testHasIllegalCharacter() {
    assertFalse(acct1.hasIllegalCharacter(""));
    assertFalse(acct1.hasIllegalCharacter("abcdefghijklmnopqratuvwxyz"));
    assertFalse(acct1.hasIllegalCharacter("ABCDEFGHIJKLMNOPQRATUVWXYZ"));
    assertFalse(acct1.hasIllegalCharacter("0123456789"));
    assertFalse(acct1.hasIllegalCharacter("!@#$%^&*()[]<>{}+=:;_,.?/~'`-|\\\""));
    assertTrue(acct1.hasIllegalCharacter(" "));
    assertTrue(acct1.hasIllegalCharacter("\t"));
    assertTrue(acct1.hasIllegalCharacter("\n"));
    assertTrue(acct1.hasIllegalCharacter("\r"));
  }

  public void testHasRequiredCharacterMix() {
    assertFalse(acct1.hasRequiredCharacterMix(""));
    assertFalse(acct1.hasRequiredCharacterMix("a"));
    assertFalse(acct1.hasRequiredCharacterMix("B"));
    assertFalse(acct1.hasRequiredCharacterMix("7"));
    assertFalse(acct1.hasRequiredCharacterMix("@"));
    assertFalse(acct1.hasRequiredCharacterMix("aB"));
    assertFalse(acct1.hasRequiredCharacterMix("B7"));
    assertFalse(acct1.hasRequiredCharacterMix("7*"));
    assertFalse(acct1.hasRequiredCharacterMix("@d"));
    assertFalse(acct1.hasRequiredCharacterMix("@d$"));
    assertFalse(acct1.hasRequiredCharacterMix("@dw"));
    assertFalse(acct1.hasRequiredCharacterMix("B78"));

    assertTrue(acct1.hasRequiredCharacterMix("a7W"));
    assertTrue(acct1.hasRequiredCharacterMix("a|W"));
    assertTrue(acct1.hasRequiredCharacterMix("b7+"));
    assertTrue(acct1.hasRequiredCharacterMix("8C*"));
    assertTrue(acct1.hasRequiredCharacterMix("ijq3xyz$#"));
    assertTrue(acct1.hasRequiredCharacterMix("ijq37Ala"));

    // check that all regexp special chars are quoted as necessary in
    // pattern
    assertTrue(acct1.hasRequiredCharacterMix("aB\\"));
    assertTrue(acct1.hasRequiredCharacterMix("aB*"));
    assertTrue(acct1.hasRequiredCharacterMix("aB+"));
    assertTrue(acct1.hasRequiredCharacterMix("aB-"));
    assertTrue(acct1.hasRequiredCharacterMix("aB."));
    assertTrue(acct1.hasRequiredCharacterMix("aB^"));
    assertTrue(acct1.hasRequiredCharacterMix("aB?"));
    assertTrue(acct1.hasRequiredCharacterMix("aB|"));
    assertTrue(acct1.hasRequiredCharacterMix("aB("));
    assertTrue(acct1.hasRequiredCharacterMix("aB)"));
    assertTrue(acct1.hasRequiredCharacterMix("aB["));
    assertTrue(acct1.hasRequiredCharacterMix("aB]"));
    assertTrue(acct1.hasRequiredCharacterMix("aB{"));
    assertTrue(acct1.hasRequiredCharacterMix("aB{"));
  }

  void checkLegalPassword(UserAccount acct, String pwd)
      throws IllegalPasswordChange {
    acct.checkLegalPassword(pwd, acct.hashPassword(pwd), false);
  }

  void checkLegalPassword(UserAccount acct, String pwd, boolean isAdmin)
      throws IllegalPasswordChange {
    acct.checkLegalPassword(pwd, acct.hashPassword(pwd), isAdmin);
  }

  void assertIllegal(String pwd, String pat) {
    try {
      checkLegalPassword(acct1, pwd);
      fail(pwd + " should be illegal");
    } catch (IllegalPasswordChange e) {
      assertMatchesRE(pat, e.getMessage());
    }
  }

  public void testCheckLegalPassword() throws Exception {
    checkLegalPassword(acct1, "ijq3xyz$#");
    checkLegalPassword(acct1, "ijq37Ala");
    assertIllegal(null, "may not be empty");
    assertIllegal("", "Password must be at least 8 characters");
    assertIllegal("ijq37Al", "Password must be at least 8 characters");
    assertIllegal("ijq37ala", "upper.*lower.*special");
    assertIllegal("ijq#$ala", "upper.*lower.*special");
    assertIllegal("ijqXYala", "upper.*lower.*special");
    assertIllegal("IJQ78ALA", "upper.*lower.*special");
    assertIllegal("IJQ()ALA", "upper.*lower.*special");
    assertIllegal("123#$987", "upper.*lower.*special");
    assertIllegal("imm37Ala", "consecutive repeated");
    assertIllegal("iim37Ala", "consecutive repeated");
    assertIllegal("ijq37All", "consecutive repeated");
    assertIllegal("ijq37\u00E7ll", "Password must contain only");
  }

  public void testSetPassword() throws Exception {
    String pwd = "1jq3xyz$#";
    acct1.setPassword(pwd);
    assertEquals(acct1.hashPassword(pwd), acct1.getPassword());
    assertEquals("SHA-256", acct1.getHashAlgorithm());
  }

  public void testCantChangePasswordMoreThanOncePerDay() throws Exception {
    TimeBase.setSimulated(Constants.DAY);
    acct1.setPassword("0jq3xyz$#");
    String pwd = acct1.getPassword();
    try {
      acct1.setPassword("1jq3xyz$#");
      fail("Too soon to be able to change password");
    } catch (IllegalPasswordChange e) {
    }
    assertEquals(pwd, acct1.getPassword());
    TimeBase.step(23 * Constants.HOUR);
    try {
      acct1.setPassword("2jq3xyz$#");
      fail("Too soon to be able to change password");
    } catch (IllegalPasswordChange e) {
    }
    assertEquals(pwd, acct1.getPassword());
    TimeBase.step(59 * Constants.MINUTE);
    try {
      acct1.setPassword("3jq3xyz$#");
      fail("Too soon to be able to change password");
    } catch (IllegalPasswordChange e) {
    }
    assertEquals(pwd, acct1.getPassword());
    TimeBase.step(59 * Constants.SECOND);
    try {
      acct1.setPassword("4jq3xyz$#");
      fail("Too soon to be able to change password");
    } catch (IllegalPasswordChange e) {
    }
    assertEquals(pwd, acct1.getPassword());
    TimeBase.step(1 * Constants.SECOND);
    acct1.setPassword("5jq3xyz$#");
    assertNotEquals(pwd, acct1.getPassword());
  }

  public void testAdminCanChangePasswordMoreThanOncePerDay() throws Exception {
    TimeBase.setSimulated(Constants.DAY);
    acct1.setPassword("0jq3xyz$#");
    String pwd = acct1.getPassword();
    try {
      acct1.setPassword("2jq3xyz$#");
      fail("Too soon for user to change password");
    } catch (IllegalPasswordChange e) {
    }
    assertEquals(pwd, acct1.getPassword());
    acct1.setPassword("3jq3xyz$#", true);
    assertNotEquals(pwd, acct1.getPassword());
    pwd = acct1.getPassword();
    // Now user should be able to change
    acct1.setPassword("2jq3xyz$#", true);
    assertNotEquals(pwd, acct1.getPassword());
  }

  public void testPasswordExpiration() throws IllegalPasswordChange {
    TimeBase.setSimulated(10000);
    acct1.setPassword("ijq3xyz$#");
    acct1.checkPasswordReminder();
    assertEmpty(acctMgr.alerts);
    TimeBase.step(53 * Constants.DAY - 500);
    acct1.checkPasswordReminder();
    assertFalse(acct1.isPasswordExpired());
    assertEmpty(acctMgr.alerts);
    assertEmpty(acctMgr.stored);
    TimeBase.step(500);
    acct1.checkPasswordReminder();
    assertFalse(acct1.isPasswordExpired());
    assertEquals(1, acctMgr.alerts.size());
    AlertEvent ev = acctMgr.alerts.get(0);
    assertMatchesRE("The password for user 'User1' will expire at ",
		    ev.text);
    assertEquals(ListUtil.list(acct1), acctMgr.stored);

    TimeBase.step(6 * Constants.DAY);
    acct1.checkPasswordReminder();
    assertFalse(acct1.isPasswordExpired());
    assertEquals(1, acctMgr.alerts.size());

    TimeBase.step(1 * Constants.DAY);
    assertTrue(acct1.isPasswordExpired());
    acct1.checkPasswordReminder();
    assertTrue(acct1.isPasswordExpired());
    assertEquals(2, acctMgr.alerts.size());

    TimeBase.step(50);
    assertTrue(acct1.isPasswordExpired());
    AlertEvent ev2 = acctMgr.alerts.get(1);
    assertMatchesRE("User 'User1' disabled because password has expired.",
		    ev2.text);
    assertEquals(ListUtil.list(acct1, acct1), acctMgr.stored);

    TimeBase.step(70 * Constants.DAY);
    assertTrue(acct1.isPasswordExpired());
    acct1.checkPasswordReminder();
    assertTrue(acct1.isPasswordExpired());
    assertEquals(2, acctMgr.alerts.size());
    assertEquals(2, acctMgr.stored.size());
  }

  void incrDateAndSetPassword(String pwd) throws IllegalPasswordChange {
    TimeBase.step(Constants.DAY);
    acct1.setPassword(pwd);
  }

  String[] passwds11 = {
    "0jq3xyz$#",
    "1jq3xyz$#",
    "2jq3xyz$#",
    "3jq3xyz$#",
    "4jq3xyz$#",
    "5jq3xyz$#",
    "6jq3xyz$#",
    "7jq3xyz$#",
    "8jq3xyz$#",
    "9jq3xyz$#",
    "Ajq3xyz$#",
    "Bjq3xyz$#",
    "Cjq3xyz$#",
  };

  public void testCheckLegalPasswordHist() throws Exception {
    TimeBase.setSimulated(Constants.DAY);
    for (int ix0 = 0; ix0 < passwds11.length * 2; ix0++) {
      int ix = ix0 % 13;
      incrDateAndSetPassword(passwds11[ix]);
      assertEquals(acct1.hashPassword(passwds11[ix]), acct1.getPassword());
      for (int jx0 = ix0; jx0 >= Math.max(0, ix0 - 11); jx0--) {
	  int jx = jx0 % 13;
	try {
	  incrDateAndSetPassword(passwds11[jx]);
	  fail("Shouldn't be able to repeat passwd " + passwds11[jx]
	       + " on iteration " + ix + ", " + jx);
	} catch (IllegalPasswordChange e) {
	}
	assertEquals(acct1.hashPassword(passwds11[ix]), acct1.getPassword());
      }
    }
    incrDateAndSetPassword(passwds11[0]);
    assertEquals(acct1.hashPassword(passwds11[0]), acct1.getPassword());
  }

  public void testMaxLoginInactivity() throws Exception {
    assertEquals(15 * Constants.MINUTE, acct1.getInactivityLogout());
  }

  String PWD1 = "1jq3xyz$#";
  String PWD2 = "2jq3xyz$#";
  String PWD3 = "3jq3xyz$#";

  public void testCheck() throws Exception {
    TimeBase.setSimulated(Constants.DAY);
    acct1.setPassword(PWD1);
    // test good and bad passwd
    assertNotEquals(TimeBase.nowMs(), acct1.getLastLogin());
    assertTrue(acct1.check(PWD1));
    assertEquals(TimeBase.nowMs(), acct1.getLastLogin());
    TimeBase.step(Constants.WEEK);
    assertFalse(acct1.check(PWD2));
    assertEquals(Constants.DAY, acct1.getLastLogin());
    assertTrue(acct1.check(PWD1));
  }

  public void testExpire() throws Exception {
    TimeBase.setSimulated(Constants.DAY);
    acct1.setPassword(PWD1);
    assertTrue(acct1.check(PWD1));
    // expire password
    TimeBase.step(100 * Constants.DAY);
    assertFalse(acct1.isEnabled());
    assertFalse(acct1.check(PWD1));
    assertMatchesRE("Password has expired", acct1.getDisabledMessage());
    // reset
    acct1.setPassword(PWD2);
    assertTrue(acct1.isEnabled());
    assertTrue(acct1.check(PWD2));
    assertFalse(acct1.check(PWD1));
  }

  public void testRepeatedFail() throws Exception {
    TimeBase.setSimulated(Constants.DAY);
    acct1.setPassword(PWD1);
    assertTrue(acct1.check(PWD1));
    assertFalse(acct1.check(PWD2));
    assertTrue(acct1.isEnabled());
    assertTrue(acct1.check(PWD1));
    TimeBase.step(8 * Constants.MINUTE);
    assertFalse(acct1.check(PWD2));
    assertTrue(acct1.isEnabled());
    assertTrue(acct1.check(PWD1));
    TimeBase.step(8 * Constants.MINUTE);
    assertFalse(acct1.check(PWD2));
    // 3 failures within 16 minutes, shouldn't be disabled
    assertTrue(acct1.check(PWD1));
    assertTrue(acct1.isEnabled());
    assertNull(acct1.getDisabledMessage());

    // 3rd failure within 15 minutes, should be disabled
    TimeBase.step(7 * Constants.MINUTE);
    assertFalse(acct1.check(PWD2));
    assertFalse(acct1.isEnabled());
    assertFalse(acct1.check(PWD1));
    assertMatchesRE("3 failed login attempts at", acct1.getDisabledMessage());

    // still disabled 7 minutes later, attempt fails but doesn't reset timer
    TimeBase.step(7 * Constants.MINUTE);
    assertFalse(acct1.isEnabled());
    assertFalse(acct1.check(PWD1));
    // now 16 minutes since last password failure, re-enabled
    TimeBase.step(9 * Constants.MINUTE);
    assertTrue(acct1.isEnabled());
    assertNull(acct1.getDisabledMessage());
    assertTrue(acct1.check(PWD1));
  }

  static class AlertEvent {
    UserAccount acct;
    Alert alert;
    String text;

    AlertEvent(UserAccount acct, Alert alert, String text) {
      this.acct = acct;
      this.alert = alert;
      this.text = text;
    }
  }


  static class MyAccountManager extends AccountManager {
    List<AlertEvent> alerts = new ArrayList<AlertEvent>();
    List<UserAccount> stored = new ArrayList<UserAccount>();

    @Override
    public void storeUser(UserAccount acct) {
      // suppress storing
      stored.add(acct);
    }

    @Override
    void alertUser(UserAccount acct, Alert alert, String text) {
      alerts.add(new AlertEvent(acct, alert, text));
    }
  }
}
