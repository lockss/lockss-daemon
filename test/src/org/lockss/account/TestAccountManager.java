/*
 * $Id: TestAccountManager.java,v 1.5 2009-06-19 08:27:25 tlipkis Exp $
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
import org.lockss.config.*;
import org.lockss.servlet.*;
import org.lockss.test.*;
import static org.lockss.servlet.LockssServlet.ROLE_USER_ADMIN;

/**
 * Test class for org.lockss.account.AccountManager
 */
public class TestAccountManager extends LockssTestCase {

  MyAccountManager acctMgr;

  public void setUp() throws Exception {
    super.setUp();
    Properties p = new Properties();
    p.put(AccountManager.PARAM_ENABLED, "true");
    p.put(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
	  getTempDir("accttest").toString());
    ConfigurationUtil.setCurrentConfigFromProps(p);
    acctMgr = new MyAccountManager();
    getMockLockssDaemon().setAccountManager(acctMgr);
    acctMgr.initService(getMockLockssDaemon());
    acctMgr.startService();
  }
  
  public void tearDown() throws Exception {
    super.tearDown();
  }

  void assertEqualAccts(UserAccount a1, UserAccount a2) {
    assertEquals(a1.getName(), a2.getName());
    assertEquals(a1.getPassword(), a2.getPassword());
    assertEquals(a1.getRoles(), a2.getRoles());
    assertEquals(a1.getRoleSet(), a2.getRoleSet());
    assertEquals(a1.getEmail(), a2.getEmail());
    assertEquals(a1.getHashAlgorithm(), a2.getHashAlgorithm());
    
    assertEquals(a1.getCredentialString(), a2.getCredentialString());
    assertEquals(a1.getLastPasswordChange(), a2.getLastPasswordChange());
    assertEquals(a1.getLastUserPasswordChange(),
		 a2.getLastUserPasswordChange());

    assertEquals(a1.isEnabled(), a2.isEnabled());
  }

  public void testGetUserFactory() {
    assertTrue(acctMgr.getUserFactory("basic")
	       instanceof BasicUserAccount.Factory);
    assertTrue(acctMgr.getUserFactory("Basic")
	       instanceof BasicUserAccount.Factory);
    assertTrue(acctMgr.getUserFactory("Unknown")
	       instanceof BasicUserAccount.Factory);
    assertTrue(acctMgr.getUserFactory("LC")
	       instanceof LCUserAccount.Factory);
    assertTrue(acctMgr.getUserFactory("org.lockss.account.LCUserAccount")
	       instanceof LCUserAccount.Factory);
    assertTrue(acctMgr.getUserFactory("org.lockss.account.StaticUserAccount")
	       instanceof StaticUserAccount.Factory);

    assertTrue(acctMgr.getUserFactory("Unknown")
	       instanceof BasicUserAccount.Factory);
    assertTrue(acctMgr.getUserFactory("")
	       instanceof BasicUserAccount.Factory);
    assertTrue(acctMgr.getUserFactory(null)
	       instanceof BasicUserAccount.Factory);
  }

  public void testCreateUser() {
    UserAccount acct = acctMgr.createUser("fred");
    assertTrue(acct instanceof BasicUserAccount);
    assertEquals("fred", acct.getName());
    ConfigurationUtil.addFromArgs(AccountManager.PARAM_NEW_ACCOUNT_TYPE, "lc");
    acct = acctMgr.createUser("ethel");
    assertTrue(acct.getClass().toString(), acct instanceof LCUserAccount);
    assertEquals("ethel", acct.getName());
  }

  public void testGetUser() throws Exception {
    assertFalse(acctMgr.hasUser("nouser"));
    assertNull(acctMgr.getUserOrNull("nouser"));
    assertSame(AccountManager.NOBODY_ACCOUNT, acctMgr.getUser("nouser"));

    acctMgr.addStaticUser("foo", "SHA-1:0102");
    assertTrue(acctMgr.hasUser("foo"));
    assertEquals("foo", acctMgr.getUser("foo").getName());
  }

  public void testAddUser() throws Exception {
    String user = "fred";
    UserAccount acct1 = acctMgr.createUser(user);
    try {
      acctMgr.addUser(acct1);
      fail("Should be illegal to add user without password");
    } catch (AccountManager.NotAddedException e) {
    }
    acct1.setPassword("password");
    assertNull(acctMgr.getUserOrNull(user));
    acctMgr.addUser(acct1);
    UserAccount acct2 = acctMgr.getUser(user);
    assertSame(acct1, acct2);
    // should be ok to add redundantly
    acctMgr.addUser(acct1);
    UserAccount acct3 = acctMgr.createUser(user);
    acct3.setPassword("fleeble");
    try {
      acctMgr.addUser(acct3);
      fail("Should be illegal to add user with existing name");
    } catch (AccountManager.UserExistsException e) {
    }
  }

  public void testAddStaticUser() throws Exception {
    String user = "ferd";
    String cred = "SHA-1:01020304";
    UserAccount acct1 = acctMgr.addStaticUser(user, cred);
    UserAccount acct2 = acctMgr.getUser(user);
    assertSame(acct1, acct2);
    assertTrue(acct1.isStaticUser());
    File f1 = new File(acctMgr.getAcctDir(), "user");
    assertFalse(f1.exists());
  }

  public void testAddPlatformUser0() throws Exception {
    String user = "ferd";
    String cred = "SHA-1:01020304";

    assertEquals(0, acctMgr.getUsers().size());
    acctMgr.installPlatformUser(null, cred);
    assertEquals(0, acctMgr.getUsers().size());
    acctMgr.installPlatformUser(user, null);
    assertEquals(0, acctMgr.getUsers().size());
    ConfigurationUtil.addFromArgs(AccountManager.PARAM_CONDITIONAL_PLATFORM_USER,
				  "true");
    acctMgr.installPlatformUser(user, cred);
    assertEquals(1, acctMgr.getUsers().size());
    UserAccount acct1 = acctMgr.getUser(user);
    assertEquals(user, acct1.getName());
    assertEquals(ROLE_USER_ADMIN, acct1.getRoles());
    assertTrue(acct1.isStaticUser());
    File f1 = new File(acctMgr.getAcctDir(), "user");
    assertFalse(f1.exists());
  }

  public void testAddPlatformUser1() throws Exception {
    String user = "ferd";
    String cred = "SHA-1:01020304";

    assertEquals(0, acctMgr.getUsers().size());
    UserAccount admin = acctMgr.createUser("foo");
    admin.setPassword("1234Abcd");
    admin.setRoles(ROLE_USER_ADMIN);
    acctMgr.addUser(admin);
    assertEquals(1, acctMgr.getUsers().size());
    ConfigurationUtil.addFromArgs(AccountManager.PARAM_CONDITIONAL_PLATFORM_USER,
				  "true");
    acctMgr.installPlatformUser(user, cred);
    assertEquals(1, acctMgr.getUsers().size());
    assertNull(acctMgr.getUserOrNull(user));
  }

  public void testLoadFromProps() throws Exception {
    String name = "luser";
    assertEquals(0, acctMgr.getUsers().size());
    acctMgr.loadFromProps(PropUtil.fromArgs(name,
					    "SHA-1:0102,fooRole,barRole\n"));
    UserAccount acct = acctMgr.getUser(name);
    assertEquals(SetUtil.set("fooRole", "barRole"),
		 SetUtil.theSet(acct.getRoleSet()));
  }

  public void xtestLoadFromProps() throws Exception {
    String name = "luser";
    String url =
      FileTestUtil.urlOfString(name + ": SHA-1:01020304,fooRole,barRole\n");
    assertEquals(0, acctMgr.getUsers().size());
    acctMgr.loadFromProps(url);
    UserAccount acct = acctMgr.getUser(name);
    assertEquals(SetUtil.set("FooRole", "BarRole"), acct.getRoleSet());
  }

  UserAccount makeUser(String name) {
    UserAccount acct = new BasicUserAccount.Factory().newUser(name, acctMgr);
    return acct;
  }

  public void testSanitizeName() {
    assertEquals("foobar123", acctMgr.sanitizeName("foobar123"));
    assertEquals("foobar", acctMgr.sanitizeName("foo.bar"));
    assertEquals("foobar_", acctMgr.sanitizeName(" +.!|,foo.bar?<>_"));
  }

  public void testGenerateFilename() {
    assertEquals("john_smith",
		 acctMgr.generateFilename(makeUser("John_Smith")));
    assertEquals("foo",
		 acctMgr.generateFilename(makeUser("foo!")));
  }

  static String PWD1 = "123Sb!@#";
  static String PWD2 = "223Sb!@#";

  // This tests both storeUser() and loadUsers()
  public void testStoreUser() throws Exception {
    UserAccount acct1 = makeUser("lu@ser");
    acct1.setPassword(PWD1, true);
    try {
      acctMgr.storeUser(acct1);
      fail("Shouldn't be able to store un-added account");
    } catch (IllegalArgumentException e) {
    }
    acctMgr.addUser(acct1);
    File f1 = new File(acctMgr.getAcctDir(), "luser");
    assertTrue(f1.exists());
    UserAccount acct2 = makeUser("luser!");
    acct2.setPassword(PWD2, true);
    acctMgr.addUser(acct2);
    File f2 = new File(acctMgr.getAcctDir(), "luser_1");
    assertTrue(f2.exists());
    f1.delete();
    assertFalse(f1.exists());
    acctMgr.storeUser(acct1);
    assertFalse(f1.exists());
    acct1.setEmail("her@there");
    acctMgr.storeUser(acct1);
    assertTrue(f1.exists());
    
    assertSame(acct1, acctMgr.getUser(acct1.getName()));
    assertSame(acct2, acctMgr.getUser(acct2.getName()));
    acctMgr.clearAccounts();
    assertNull(acctMgr.getUserOrNull(acct2.getName()));

    acctMgr.loadUsers();
    assertEqualAccts(acct1, acctMgr.getUser(acct1.getName()));
    assertEqualAccts(acct2, acctMgr.getUser(acct2.getName()));
    assertEquals(2, acctMgr.getUsers().size());

    // Now test loadUsers' file filtering

    acctMgr.clearAccounts();
    assertEquals(0, acctMgr.getUsers().size());

    // Rename acct1 file to a name that shouldn't pass the filter
    File illFile = new File(f1.getParent(), "lu.ser");
    assertEquals(f1.getParent(), illFile.getParent());
    f1.renameTo(illFile);

    // Create a subdir that shouldn't pass the filter.  It doesn't hurt
    // anything even if AccountManager tries to process the subdir, and the
    // test won't fail, but the error will appear in the test log.
    File subdir = new File(f1.getParent(), "adir");
    subdir.mkdir();

    acctMgr.loadUsers();
    assertEquals(1, acctMgr.getUsers().size());
    assertNull(acctMgr.getUserOrNull(acct1.getName()));
    assertEqualAccts(acct2, acctMgr.getUser(acct2.getName()));
  }

  public void testStoreWrongUser() throws Exception {
    UserAccount acct1 = makeUser("luser");
    acct1.setPassword(PWD1, true);
    acctMgr.addUser(acct1);

    UserAccount acct2 = makeUser("luser");
    acct2.setPassword(PWD1, true);
    try {
      acctMgr.storeUser(acct2);
      fail("Shouldn't be able to store different instance");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testDeleteUser() throws Exception {
    String name = "lu@ser";
    UserAccount acct1 = makeUser(name);
    acct1.setPassword(PWD1, true);
    acctMgr.addUser(acct1);
    File f1 = new File(acctMgr.getAcctDir(), "luser");
    assertTrue(f1.exists());
    assertSame(acct1, acctMgr.getUser(name));
    assertTrue(acct1.isEnabled());
    assertTrue(acctMgr.deleteUser(name));
    assertFalse(acct1.isEnabled());
    assertFalse(f1.exists());
    assertNull(acctMgr.getUserOrNull(name));

    assertTrue(acctMgr.deleteUser("notthere"));
  }

  static class MyAccountManager extends AccountManager {
    void clearAccounts() {
      accountMap.clear();
    }
  }

}
